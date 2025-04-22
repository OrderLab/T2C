#!/bin/bash

if [ $# -ne 1 ]; then
  dest=protoc     # default creates a protoc directory
else
  dest=$1
fi
version=2.5.0
mkdir -p $dest
dest=$(cd $dest && pwd)
PCD=$dest/$version
if [ -d $PCD ]; then
  echo "Protobuf $version already exist. Skip"
  exit 0
fi
WD=protobuf-$version
DIST=v$version.tar.gz
cd /tmp
wget https://github.com/google/protobuf/archive/$DIST
tar -xvf $DIST -C $dest > /dev/null
mv $dest/$WD $PCD
if [ $? -ne 0 ]; then
  echo "Fail to move source directory"
  exit 1
fi
cd $PCD
sed -i -e "s/curl http:\\/\\/googletest.googlecode.com\\/files\\/gtest-1.5.0.tar.bz2 | tar jx/curl -L https:\\/\\/github.com\\/google\\/googletest\\/archive\\/release-1.5.0.tar.gz | tar zx/" -e "s/mv gtest-1.5.0 gtest/mv googletest-release-1.5.0 gtest/" autogen.sh
./autogen.sh
if [ $? -ne 0 ]; then
  echo "Fail to bootstrap"
  exit 1
fi
mkdir dist
./configure --prefix=$PCD/dist
make -j8
make install
cd /tmp
rm -f $DIST