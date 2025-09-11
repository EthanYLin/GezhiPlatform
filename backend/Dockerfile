# ------------------------------------
# 构建阶段 BUILD
# ------------------------------------
FROM maven:3.9.9-eclipse-temurin-23 AS build
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
RUN mvn -B -q dependency:go-offline

# 复制源代码
COPY src ./src

# 构建应用 Boot 3.x 会生成分层 JAR
RUN mvn -B -q clean package -DskipTests

# 抽取分层（dependencies/、spring-boot-loader/、application/ ...）
RUN set -eux; \
    ls -lah target; \
    cp -v "$(ls -1 target/*.jar | grep -v '\-original\.jar$' | head -n1)" /app/app.jar; \
    test -f /app/app.jar

# ------------------------------------
# 运行阶段 RUN
# ------------------------------------
FROM eclipse-temurin:23-jre-alpine
WORKDIR /app
RUN apk add --no-cache ca-certificates tzdata

COPY --from=build /app/app.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]