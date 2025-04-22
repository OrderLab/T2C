#! /bin/bash

cd "$1" || exit

git stash
git checkout tags/cassandra-3.11.3
# ant realclean
ant
chmod +x -R build/

echo Build successful