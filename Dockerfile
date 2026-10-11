# ============================================================
# TerranAngel Demo Web —— 多阶段构建
#
# 阶段一 build：用带 Maven 的 JDK 25 镜像编译打包
# 阶段二       ：只保留 JRE + 可执行 jar，产出精简运行镜像
#
# 构建：docker build -t terranangel-demo-web .
# 运行：docker run -p 8080:8080 -e TERRANANGEL_DATASOURCE_POSTGRES_ENABLED=false \
#                    -e TERRANANGEL_DATASOURCE_MYSQL_ENABLED=false terranangel-demo-web
# ============================================================

# ---------- 阶段一：构建 ----------
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /build

# 复制全部源码（无关内容已由 .dockerignore 排除）
COPY . .

# 只构建 demo-web 及其依赖的模块
#   -pl 指定模块
#   -am 同时构建它依赖的模块（terranangel-common / exception / context / demo-api）
#   跳过测试编译与执行，加快构建
RUN mvn -B -pl terranangel-demo-web -am clean package -Dmaven.test.skip=true

# ---------- 阶段二：运行 ----------
FROM eclipse-temurin:25-jre

WORKDIR /app

# 只把构建产物里的可执行 jar 拿过来（*.jar.original 不会被匹配）
COPY --from=build /build/terranangel-demo-web/target/*.jar terranangel-web.jar

# 时区，保证日志时间正确
ENV TZ=Asia/Shanghai

# 服务端口不在这里写死！
# Spring Boot 的配置优先级是「环境变量 > application.yml」，
# 一旦这里设了 ENV SERVER_PORT，就会覆盖掉 yml 里的 server.port=${PORT:8080}。
# 端口交给 application.yml 决定：有 PORT 用 PORT，没有用 8080。
EXPOSE 8080

# 注意路径：COPY 的目标是相对 WORKDIR(/app) 的，
# 所以 jar 实际位于 /app/terranangel-web.jar，这里必须写全（不能只写 /terranangel-web.jar）
ENTRYPOINT ["java", "-jar", "/app/terranangel-web.jar"]
