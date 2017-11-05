#!/bin/bash

function printProcess() {
  ps -ef | grep "java" | grep "winstone" | grep "cryptotrader" | awk '{print $2}'
}

PROCESS_ID="`printProcess`"

if [ "$PROCESS_ID" == "" ]; then

  echo "No process running."

  exit 0

fi

pushd "`dirname "$0"`" > /dev/null || exit $?

java -cp winstone-*.jar winstone.tools.WinstoneControl shutdown --host=127.0.01 --port=41422

popd > /dev/null 2>&1

LIMIT=60

for i in `seq 1 $LIMIT`
do

  echo "Waiting for the process to terminate ($i / $LIMIT) : $PROCESS_ID"

  sleep 5

  PROCESS_ID="`printProcess`"

  if [ "$PROCESS_ID" == "" ]; then

    echo "Process terminated : $PROCESS_ID"

    exit 0

  fi

done

echo "Force terminating : $PROCESS_ID"

kill "$PROCESS_ID"

exit $?
