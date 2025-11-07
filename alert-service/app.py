"""
告警服务：支持邮件、企业微信、短信多渠道告警
"""
from flask import Flask, request, jsonify
import os
import json
import pymysql
import smtplib
import requests
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from datetime import datetime
from dotenv import load_dotenv

load_dotenv()
app = Flask(__name__)

# 数据库配置
DB_CONFIG = {
    "host": os.getenv("MYSQL_HOST", "mysql"),
    "port": int(os.getenv("MYSQL_PORT", 3306)),
    "user": os.getenv("MYSQL_USER", "root"),
    "password": os.getenv("MYSQL_PASSWORD", "root"),
    "database": os.getenv("MYSQL_DATABASE", "ai_inspect"),
    "charset": "utf8mb4"
}

# 告警渠道配置
EMAIL_CONFIG = {
    "smtp_host": os.getenv("SMTP_HOST", "smtp.qq.com"),
    "smtp_port": int(os.getenv("SMTP_PORT", 587)),
    "smtp_user": os.getenv("SMTP_USER", ""),
    "smtp_password": os.getenv("SMTP_PASSWORD", ""),
    "from_email": os.getenv("FROM_EMAIL", "")
}

WECHAT_WEBHOOK = os.getenv("WECHAT_WEBHOOK", "")  # 企业微信机器人Webhook
DINGTALK_WEBHOOK = os.getenv("DINGTALK_WEBHOOK", "")  # 钉钉机器人Webhook

def get_db_connection():
    """获取数据库连接"""
    return pymysql.connect(**DB_CONFIG)

def send_email(to_email: str, subject: str, content: str) -> bool:
    """发送邮件告警"""
    try:
        msg = MIMEMultipart()
        msg['From'] = EMAIL_CONFIG["from_email"]
        msg['To'] = to_email
        msg['Subject'] = subject
        msg.attach(MIMEText(content, 'plain', 'utf-8'))
        
        server = smtplib.SMTP(EMAIL_CONFIG["smtp_host"], EMAIL_CONFIG["smtp_port"])
        server.starttls()
        server.login(EMAIL_CONFIG["smtp_user"], EMAIL_CONFIG["smtp_password"])
        server.send_message(msg)
        server.quit()
        return True
    except Exception as e:
        print(f"邮件发送失败：{e}")
        return False

def send_wechat(message: str) -> bool:
    """发送企业微信告警"""
    if not WECHAT_WEBHOOK:
        return False
    
    try:
        data = {
            "msgtype": "text",
            "text": {
                "content": message
            }
        }
        resp = requests.post(WECHAT_WEBHOOK, json=data, timeout=10)
        return resp.status_code == 200
    except Exception as e:
        print(f"企业微信发送失败：{e}")
        return False

def send_dingtalk(message: str) -> bool:
    """发送钉钉告警"""
    if not DINGTALK_WEBHOOK:
        return False
    
    try:
        data = {
            "msgtype": "text",
            "text": {
                "content": message
            }
        }
        resp = requests.post(DINGTALK_WEBHOOK, json=data, timeout=10)
        return resp.status_code == 200
    except Exception as e:
        print(f"钉钉发送失败：{e}")
        return False

def send_sms(phone: str, message: str) -> bool:
    """发送短信告警（需要接入短信服务商API）"""
    # 这里需要接入阿里云、腾讯云等短信服务
    # 示例：调用阿里云短信API
    print(f"短信发送到 {phone}：{message}")
    return True

@app.post("/api/alert/send")
def send_alert():
    """发送告警"""
    data = request.json
    
    device_id = data.get("device_id")
    alert_config_id = data.get("alert_config_id")
    metric = data.get("metric")
    metric_value = data.get("metric_value")
    message = data.get("message", f"设备 {device_id} 指标 {metric} 异常：{metric_value}")
    
    # 获取告警配置
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT channels, name FROM alert_config WHERE id = %s", (alert_config_id,))
    config = cursor.fetchone()
    
    if not config:
        conn.close()
        return jsonify({"code": 404, "msg": "告警配置不存在"})
    
    channels = json.loads(config[0])
    alert_name = config[1]
    
    # 获取接收人（管理员和运维人员）
    cursor.execute("SELECT email, phone FROM user WHERE role IN ('admin', 'operator')")
    users = cursor.fetchall()
    conn.close()
    
    # 发送告警
    send_results = {}
    
    for channel in channels:
        if channel == "email":
            for email, _ in users:
                if email:
                    send_results[f"email_{email}"] = send_email(
                        email,
                        f"【网络巡检告警】{alert_name}",
                        message
                    )
        
        elif channel == "wechat":
            send_results["wechat"] = send_wechat(message)
        
        elif channel == "dingtalk":
            send_results["dingtalk"] = send_dingtalk(message)
        
        elif channel == "sms":
            for _, phone in users:
                if phone:
                    send_results[f"sms_{phone}"] = send_sms(phone, message)
    
    # 保存告警记录
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("""
        INSERT INTO alert_record (alert_config_id, device_id, metric_value, alert_level, message, channels, send_status)
        VALUES (%s, %s, %s, %s, %s, %s, %s)
    """, (
        alert_config_id,
        device_id,
        metric_value,
        "critical" if "critical" in message.lower() else "warning",
        message,
        json.dumps(channels),
        "success" if any(send_results.values()) else "failed"
    ))
    conn.commit()
    conn.close()
    
    return jsonify({"code": 200, "data": send_results})

@app.post("/api/alert/config/add")
def add_alert_config():
    """添加告警配置"""
    data = request.json
    
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("""
        INSERT INTO alert_config (name, device_id, metric, threshold, comparison, channels, enabled)
        VALUES (%s, %s, %s, %s, %s, %s, %s)
    """, (
        data.get("name"),
        data.get("device_id"),
        data.get("metric"),
        data.get("threshold"),
        data.get("comparison", ">"),
        json.dumps(data.get("channels", [])),
        data.get("enabled", True)
    ))
    
    config_id = cursor.lastrowid
    conn.commit()
    conn.close()
    
    return jsonify({"code": 200, "data": {"id": config_id}})

@app.get("/api/alert/config/list")
def list_alert_configs():
    """获取告警配置列表"""
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT id, name, device_id, metric, threshold, enabled FROM alert_config")
    
    configs = []
    for row in cursor.fetchall():
        configs.append({
            "id": row[0],
            "name": row[1],
            "device_id": row[2],
            "metric": row[3],
            "threshold": float(row[4]),
            "enabled": bool(row[5])
        })
    
    conn.close()
    return jsonify({"code": 200, "data": configs})

@app.get("/api/alert/record/list")
def list_alert_records():
    """获取告警记录列表"""
    device_id = request.args.get("device_id")
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    if device_id:
        cursor.execute("""
            SELECT id, device_id, metric_value, alert_level, message, send_status, create_time
            FROM alert_record WHERE device_id = %s ORDER BY create_time DESC LIMIT 50
        """, (device_id,))
    else:
        cursor.execute("""
            SELECT id, device_id, metric_value, alert_level, message, send_status, create_time
            FROM alert_record ORDER BY create_time DESC LIMIT 50
        """)
    
    records = []
    for row in cursor.fetchall():
        records.append({
            "id": row[0],
            "device_id": row[1],
            "metric_value": float(row[2]),
            "alert_level": row[3],
            "message": row[4],
            "send_status": row[5],
            "create_time": row[6].strftime("%Y-%m-%d %H:%M:%S")
        })
    
    conn.close()
    return jsonify({"code": 200, "data": records})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8004)


