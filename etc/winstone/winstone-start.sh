#!/bin/bash

pushd "`dirname "$0"`" > /dev/null || exit $?

if [ ! -d "logs" ]; then
  mkdir "logs" || exit $?
fi

nohup \
  java \
  -server \
  -Xms256m \
  -Xmx256m \
  -XX:+UseParallelGC \
  -XX:+UseParallelOldGC \
  -Xloggc:logs/winstone-gc.log \
  -XX:+PrintGCDetails \
  -XX:+PrintGCDateStamps \
  -XX:+UseGCLogFileRotation \
  -XX:NumberOfGCLogFiles=9 \
  -XX:GCLogFileSize=10M \
  -XX:HeapDumpPath=logs/winstone-heap_`date +%Y%m%d_%H%M%S`.log \
  -XX:+HeapDumpOnOutOfMemoryError \
  -jar winstone-*.jar \
  --warfile="`find . -name "cryptotrader-*.war"`" \
  --prefix="cryptotrader" \
  --httpListenAddress=127.0.0.1 \
  --httpPort=41480 \
  --httpsListenAddress=127.0.0.1 \
  --httpsPort=41443 \
  --controlPort=41422 \
 > logs/winstone-console.log 2>&1 & > logs/winstone-nohup.log 2>&1

sleep 1

ps -ef | grep "java" | grep "winstone" | grep "cryptotrader"

popd > /dev/null 2>&1

#  -Dcom.sun.management.jmxremote \
#  -Dcom.sun.management.jmxremote.port=41414 \
#  -Dcom.sun.management.jmxremote.authenticate=true \
#  -Dcom.sun.management.jmxremote.password.file=~/.jmxremote
#  -Dcom.sun.management.jmxremote.ssl=true \
#  -Dcom.sun.management.jmxremote.ssl.need.client.auth=true \
