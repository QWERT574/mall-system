@echo off
set JAVA_HOME=D:\soft\java\jdk1.8.0_181
set DB_HOST=localhost
set DB_PORT=3306
set DB_USERNAME=root
set DB_PASSWORD=123456
set DB_NAME=minimall
set REDIS_HOST=localhost
set REDIS_PORT=6379
set REDIS_PASSWORD=redis123
cd /d "E:\迅雷下载\mall_system_extended\backend"
D:\workspace\apache-maven-3.8.4\apache-maven-3.8.4\bin\mvn.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=8088
