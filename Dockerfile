# ------------------------------------
# 构建阶段：使用 Maven + JDK 23
# ------------------------------------
FROM maven:3.9.9-amazoncorretto-23 AS build

# 设置工作目录
WORKDIR /app

# 使用阿里云 Maven 源替换默认 central
COPY pom.xml ./
RUN mkdir -p /root/.m2
RUN echo '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" \
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" \
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 \
  https://maven.apache.org/xsd/settings-1.0.0.xsd"> \
  <mirrors> \
    <mirror> \
      <id>aliyunmaven</id> \
      <mirrorOf>*</mirrorOf> \
      <name>Aliyun Maven</name> \
      <url>https://maven.aliyun.com/repository/public</url> \
    </mirror> \
  </mirrors> \
</settings>' > /root/.m2/settings.xml

# 预下载依赖，形成缓存层（只要 pom.xml 不变，这层不变）
RUN mvn dependency:go-offline

# 复制项目其他文件
COPY . .

# 构建应用
RUN mvn clean package -DskipTests

# ------------------------------------
# 运行阶段：精简运行镜像（OpenJDK 23）
# ------------------------------------
FROM openjdk:23-jdk

# 设置工作目录
WORKDIR /app

# 拷贝 jar 包到运行镜像中
COPY --from=build /app/target/*.jar app.jar

# 显示容器内部端口（Spring Boot 默认是 8080）
EXPOSE 8080

# 启动 Spring Boot 应用
ENTRYPOINT ["java", "-jar", "app.jar"]
