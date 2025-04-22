#! /bin/bash

# Uncomment and change if JAVA_HOME or mvn or java not detected
# PATH=$PATH:/home/dparikesit/.sdkman/candidates/java/current/bin:/home/dparikesit/.sdkman/candidates/maven/current/bin
# JAVA_HOME=/home/dparikesit/.sdkman/candidates/java/current

script_dir=$( dirname -- "$( readlink -f -- "$0"; )"; )

cd "$1" || exit 1

#git stash
git checkout tags/release-3.6.3

sudo rm -rf logs/ version-2/

mvn install -DskipTests -Dmaven.test.skip=true -Dmaven.site.skip=true -Dmaven.javadoc.skip=true || exit 1
chmod +x -R bin/

cp $script_dir/zoo.cfg ./conf
echo dataDir=$1 >> ./conf/zoo.cfg

echo Build successful