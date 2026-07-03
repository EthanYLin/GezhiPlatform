# 学生档案协同管理平台

## Overview / 项目简介

用于管理学生基础信息和学生档案，支持学校内不同角色按权限查询、维护和导出档案数据。系统面向实际校园管理场景，关注多角色协同、学生隐私数据访问控制和复杂档案表单维护。

项目采用前后端分离架构：前端基于 Next.js、React 和 TypeScript 构建，后端基于 Spring Boot、JPA、Sa-Token 和 MySQL 构建，并提供 Docker Compose 部署配置。

## Features / 功能

- **元数据驱动的动态档案表单**：后端提供学生档案字段元数据与 JSON Schema，前端根据元数据动态渲染表单、校验字段并维护复杂档案结构，减少前后端对固定字段的硬编码依赖。
- **基于角色的访问控制（RBAC）**：支持为同一用户配置多个角色，例如校级领导、年级组长、班主任、协作用户、家长用户、学生用户等。系统会根据用户角色判断其可访问的学生范围。
- **字段级读写权限控制**：在学生范围权限之外，系统进一步通过 JSON Path 配置字段级可读 / 可写权限。用户访问某个学生档案时，系统会计算当前用户对该学生生效的角色，并自动合并这些角色对应的字段权限，用于控制档案查询、编辑和导出。
- **学生档案导出与审计**：支持将权限过滤后的学生档案导出为 Excel 文件，并记录档案查询、导出等敏感操作，方便管理员追踪访问行为。
- **容器化部署**：提供前端、后端和 MySQL 的 Docker Compose 配置，支持本地构建部署和基于镜像的部署方式。

## Getting Started / 快速开始

### Requirements / 环境要求

- Java 23
- Maven 3.9+
- Node.js 20+
- npm
- MySQL 8
- Docker 与 Docker Compose（可选，用于容器化部署）

### Installation / 安装

安装前端依赖：

```bash
cd frontend
npm install
```

后端使用 Maven 构建，仓库中已包含 Maven Wrapper：

```bash
cd backend
./mvnw clean package
```

### Configuration / 配置

后端默认启用 `dev` profile，并读取本地 MySQL：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/gezhi?useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=
```

生产环境可通过环境变量配置数据库连接：

```env
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/db
SPRING_DATASOURCE_USERNAME=your-database-username
SPRING_DATASOURCE_PASSWORD=your-database-password
```

前端默认通过 `/api` 访问后端。Next.js rewrites 会将 `/api/*` 转发到 `BACKEND_URL`，未配置时默认使用 `http://localhost:8080`：

```env
BACKEND_URL=http://localhost:8080
```

### Run / 启动

本地启动后端：

```bash
cd backend
./mvnw spring-boot:run
```

本地启动前端：

```bash
cd frontend
npm run dev
```

前端默认访问 [http://localhost:3000](http://localhost:3000)，后端默认监听 `8080` 端口。

也可以使用 Docker Compose 启动完整服务。离线构建部署：

```bash
DATABASE_USERNAME=root DATABASE_PASSWORD=your-password docker compose -f docker-compose.prod-offline.yml up -d --build
```

使用已发布镜像部署：

```bash
DATABASE_USERNAME=root DATABASE_PASSWORD=your-password docker compose -f docker-compose.prod-online.yml up -d
```

如需生成测试数据，可以运行 `seed` profile：

```bash
docker compose -f docker-compose.prod-offline.yml --profile seed run --rm seed -- --option=111
```

`--option` 三位数字依次表示是否生成学生档案、用户、权限组，例如 `111` 表示全部生成。

## Usage / 使用方式

1. 管理员创建或导入学生基础信息。
2. 管理员创建用户，并为用户配置一个或多个角色。
3. 管理员配置权限组，定义不同角色可读取、可编辑的档案 JSON Path。
4. 用户登录后进入工作台，系统根据其角色展示可访问的学生范围。
5. 用户查询学生档案时，系统只返回其有权查看的字段。
6. 用户编辑学生档案时，系统只接受其有权写入的字段。
7. 用户导出学生档案时，系统只导出其有权查看的内容，并记录审计日志。

## Project Structure / 项目结构

```text
backend/                         Spring Boot 后端服务
backend/src/main/java/           控制器、实体、服务、权限与审计逻辑
backend/src/main/resources/      应用配置与档案导出模板
frontend/                        Next.js 前端应用
frontend/app/                    页面路由和业务页面
frontend/components/             通用组件和导航组件
frontend/contexts/               用户上下文
frontend/lib/                    API 客户端、认证和工具函数
docker-compose.prod-offline.yml  本地构建部署配置
docker-compose.prod-online.yml   镜像部署配置
```


## License / 开源协议

本项目使用 [Apache License 2.0](./LICENSE)。
