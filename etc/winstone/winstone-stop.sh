#!/bin/bash

function printProcess() {
  ps -ef | grep "java" | grep "winstone" | grep "cryptotrader"
}

if [ "`printProcess`" == "" ]; then

  echo "No process running."

  exit 0

fi

pushd "`dirname "$0"`" > /dev/null || exit $?

java -cp winstone-*.jar winstone.tools.WinstoneControl shutdown --host=127.0.01 --port=41422

for i in `seq 1 10`
do

  if [ "`printProcess`" == "" ]; then

    echo "Process stopped."

    break

  fi

  echo "Waiting for the process to terminate... ($i)"

  sleep "$i"

done

popd > /dev/null 2>&1
