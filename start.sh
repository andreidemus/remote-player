#!/bin/bash
if [ -f remote.pid ]; then
    echo "Already running. PID is" $(<remote.pid)
else
    jar=`ls target/ | grep ".*-standalone\.jar"`
    java -jar target/$jar $1 $2 &
        pid=$!
        echo $pid > remote.pid
        echo "Started. PID is" $pid
fi
