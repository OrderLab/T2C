#! /bin/bash

cd "$1" || exit

git checkout tags/cassandra-3.0.15
# ant realclean
ant
chmod +x -R build/

echo Build successful