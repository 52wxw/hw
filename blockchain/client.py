import requests
import json
import hashlib
from web3 import Web3
from dotenv import load_dotenv
import os

load_dotenv()

class BlockchainClient:
    def __init__(self, chain_url):
        self.web3 = Web3(Web3.HTTPProvider(chain_url))
        # 修复：从环境变量读取合约地址
        self.contract_address = os.getenv("BLOCKCHAIN_CONTRACT_ADDRESS")
        # 修复：从环境变量读取私钥，导入账户
        self.account = self.web3.eth.account.from_key(os.getenv("BLOCKCHAIN_PRIVATE_KEY"))
        self.contract_abi = json.load(open("contract/LogStore.abi"))
        self.contract = self.web3.eth.contract(address=self.contract_address, abi=self.contract_abi)

    def _calc_hash(self, content):
        """计算内容哈希"""
        return hashlib.sha256(json.dumps(content, sort_keys=True).encode()).hexdigest()

    def upload_log(self, log_data, operator="system"):
        """上传日志哈希到区块链"""
        if not self.web3.is_connected():
            raise Exception("区块链节点连接失败")
        
        log_hash = self._calc_hash(log_data)
        # 构建交易
        tx = self.contract.functions.uploadLog(
            deviceId=str(log_data["device_id"]),
            opType=log_data["type"],
            logHash=log_hash,
            operator=operator
        ).build_transaction({
            "from": self.account.address,
            "nonce": self.web3.eth.get_transaction_count(self.account.address)
        })
        # 签名并发送交易
        signed_tx = self.account.sign_transaction(tx)
        tx_hash = self.web3.eth.send_raw_transaction(signed_tx.rawTransaction)
        # 等待确认
        self.web3.eth.wait_for_transaction_receipt(tx_hash)
        return log_hash
