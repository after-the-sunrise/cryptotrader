#!/bin/bash

pushd "`dirname "$0"`" > /dev/null || exit $?

if [ ! -d "logs" ]; then
  mkdir "logs" || exit $?
fi

nohup \
  java \
  -server \
  -Xms512m \
  -Xmx512m \
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
  -jar \
  "`find . -name "winstone-*.jar"`" \
  --warfile="`find . -name "cryptotrader-*.war"`" \
  --prefix="cryptotrader" \
  --httpListenAddress=127.0.0.1 \
  --httpPort=41480 \
  --httpsListenAddress=127.0.0.1 \
  --httpsPort=40443 \
  --controlPort=40422 \
 > logs/winstone-console.log 2>&1 & > logs/winstone-nohup.log

popd > /dev/null 2>&1

#  -Dcom.sun.management.jmxremote \
#  -Dcom.sun.management.jmxremote.port=50414 \
#  -Dcom.sun.management.jmxremote.authenticate=true \
#  -Dcom.sun.management.jmxremote.password.file=~/.jmxremote
#  -Dcom.sun.management.jmxremote.ssl=true \
#  -Dcom.sun.management.jmxremote.ssl.need.client.auth=true \
