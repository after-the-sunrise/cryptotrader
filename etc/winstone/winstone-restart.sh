#!/bin/bash

pushd "`dirname "$0"`" > /dev/null || exit $?

bash winstone-stop.sh && bash winstone-start.sh

popd > /dev/null 2>&1
