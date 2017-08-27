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

popd > /dev/null 2>&1

for i in `seq 1 60`
do

  if [ "`printProcess`" == "" ]; then

    echo "Process terminated."

    exit 0

  fi

  echo "Waiting for the process to terminate... ($i)"

  sleep 5

done

echo "Could not confirm process termination."

exit 1
