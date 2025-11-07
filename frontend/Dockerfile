FROM node:lts-alpine AS build
WORKDIR /app

# 安装依赖并构建生产静态资源
COPY package*.json ./
RUN npm install --silent
COPY . .
RUN npm run build

# 使用 Nginx 作为静态资源服务器
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

