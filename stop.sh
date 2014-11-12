#!/bin/bash
if [ -f remote.pid ]; then
    pid=$(<remote.pid)
    rm remote.pid
    kill -9 $pid
    if [ $? -ne 0 ]; then
        echo "Failed to stop service."
        exit 1
    fi
    echo "Service stopped."

else
    echo "Service is not running."
fi