#!/bin/bash

pushd "`dirname "$0"`" > /dev/null || exit $?

curl -i -XPOST "http://localhost:41480/cryptotrader/rest/reload"

popd > /dev/null 2>&1
