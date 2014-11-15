#!/bin/bash
cd `dirname $0`
if [ -f remote.pid ]; then
    ./stop.sh
fi
jar=`ls target/ | grep ".*-standalone\.jar"`
java -jar target/$jar
pid=$!
echo $pid > remote.pid
echo "Started. PID is" $pid