# 基础镜像
FROM openjdk:21
# 设定时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
# 拷贝jar包
COPY hm-service.jar /app.jar
# 入口
ENTRYPOINT ["java", "-jar", "/app.jar"]