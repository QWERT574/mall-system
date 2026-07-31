#!/bin/sh
# 启动 RocketMQ Broker — Docker Desktop 上绕过 docker-compose 的 entrypoint/command 253 bug
docker rm -f minimall-rmq-broker 2>/dev/null
docker run -d --name minimall-rmq-broker \
  --network minimall-infra-net \
  -p 10911:10911 -p 10909:10909 \
  -v minimall-rmq-broker-logs:/home/rocketmq/logs \
  -v minimall-rmq-broker-store:/home/rocketmq/store \
  -e JAVA_OPT_EXT="-server -Xms256m -Xmx256m -Xmn128m" \
  docker.m.daocloud.io/apache/rocketmq:4.9.7 \
  sh -c "cd /home/rocketmq/rocketmq-4.9.7/bin && exec sh mqbroker -n minimall-rmq-namesrv:9876" 2>&1
echo "Broker started, check with: docker logs minimall-rmq-broker"
