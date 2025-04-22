#! /bin/bash

cd ~/jvmtop-0.8.0
pid=2211204
out_file=zookeeper_raw.log

while sleep 1; do
        ./jvmtop.sh $pid -n1 | grep --line-buffered CPU: >> $out_file
done