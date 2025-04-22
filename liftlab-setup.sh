#!/bin/bash

#use this script to set up on the cloud lab env

filename="/home/chlou/.ssh/id_rsa"

# Create new file or overwrite if it exists
touch "$filename"

# cloudlab-only private key
cat > "$filename" << EOL
-----BEGIN OPENSSH PRIVATE KEY-----
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
-----END OPENSSH PRIVATE KEY-----
EOL

chmod 600 /home/chlou/.ssh/id_rsa
eval `ssh-agent -s`
ssh-add /home/chlou/.ssh/id_rsa

git clone git@github.com:OrderLab/T2C.git
#git clone git@github.com:OrderLab/zookeeper.git
#git clone git@github.com:OrderLab/hadoop.git
#git clone git@github.com:OrderLab/cassandra.git

sudo apt-get update
sudo apt-get install -y git maven ant vim openjdk-8-jdk golang-go gnuplot zsh
sudo update-alternatives --set java $(sudo update-alternatives --list java | grep "java-8")
sh -c "$(wget -O- https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"

cd ./T2C && ./run_engine.sh compile
cd ./T2C && rm conf/workers && cat > "conf/workers" << EOL
lift01
lift02
lift03
lift04
lift05
EOL
