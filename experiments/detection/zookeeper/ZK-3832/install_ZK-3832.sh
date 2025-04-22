#! /bin/bash

# Uncomment and change if JAVA_HOME or mvn or java not detected
# PATH=$PATH:/home/dparikesit/.sdkman/candidates/java/current/bin:/home/dparikesit/.sdkman/candidates/maven/current/bin
# JAVA_HOME=/home/dparikesit/.sdkman/candidates/java/current

script_dir=$( dirname -- "$( readlink -f -- "$0"; )"; )

cd "$1" || exit 1

git stash
git checkout tags/release-3.5.8

sudo rm -rf logs/ version-2/ ssl/

mvn install -DskipTests -Dmaven.test.skip=true -Dmaven.site.skip=true -Dmaven.javadoc.skip=true || exit 1
chmod +x -R bin/

cp $script_dir/zoo.cfg ./conf
echo "dataDir=$1" >> ./conf/zoo.cfg
echo "secureClientPort=2183" >> ./conf/zoo.cfg
echo "sslQuorum=true" >> ./conf/zoo.cfg
echo "ssl.trustStore.location=$1/ssl/truststore.jks" >> ./conf/zoo.cfg
echo "ssl.trustStore.password=password" >> ./conf/zoo.cfg
echo "ssl.keyStore.location=$1/ssl/keystore.jks" >> ./conf/zoo.cfg
echo "ssl.keyStore.password=password" >> ./conf/zoo.cfg

mkdir ssl

keytool -genkeypair -keyalg RSA -alias selfsigned2 -keystore $1/ssl/keystore.jks -storepass password -keypass password -validity 360 -keysize 2048 -dname "CN=127.0.0.1, OU=YourOrganizationUnit, O=YourOrganization, L=YourCity, S=YourState, C=YourCountry" -ext "SAN=EMAIL:test@example.com" || exit 1

echo password | keytool -export -alias selfsigned2 -keystore $1/ssl/keystore.jks -rfc -file ./ssl/selfsigned.cer || exit 1

echo yes | keytool -importcert -alias selfsigned2 -file $1/ssl/selfsigned.cer -keystore ./ssl/truststore.jks -storepass password || exit 1

sed -i "1 a JAVA_HOME=$JAVA_HOME" ./bin/zkEnv.sh

SERVER_FLAGS="\"-Xmx\${ZK_SERVER_HEAP}m \$SERVER_JVMFLAGS -Dzookeeper.serverCnxnFactory=org.apache.zookeeper.server.NettyServerCnxnFactory -Dzookeeper.ssl.keyStore.location=$1/ssl/keystore.jks -Dzookeeper.ssl.keyStore.password=password -Dzookeeper.ssl.trustStore.location=$1/ssl/truststore.jks -Dzookeeper.ssl.trustStore.password=password\""

CLIENT_FLAGS="\"-Xmx\${ZK_CLIENT_HEAP}m \$CLIENT_JVMFLAGS -Dzookeeper.clientCnxnSocket=org.apache.zookeeper.ClientCnxnSocketNetty -Dzookeeper.client.secure=true -Dzookeeper.ssl.keyStore.location=$1/ssl/keystore.jks -Dzookeeper.ssl.keyStore.password=password -Dzookeeper.ssl.trustStore.location=$1/ssl/truststore.jks -Dzookeeper.ssl.trustStore.password=password\""

sed -i "/export SERVER_JVMFLAGS=/c\export SERVER_JVMFLAGS=$SERVER_FLAGS" ./bin/zkEnv.sh
sed -i "/export CLIENT_JVMFLAGS=/c\export CLIENT_JVMFLAGS=$CLIENT_FLAGS" ./bin/zkEnv.sh

echo Build successful