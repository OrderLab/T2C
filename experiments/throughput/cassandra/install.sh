cd ~
git clone git@github.com:apache/cassandra.git && cd cassandra && git checkout tags/cassandra-3.11.5
cd ~
git clone https://github.com/OrderLab/YCSB.git && cd YCSB && git checkout -b 0.12.0-dh origin/0.12.0-dh
cd ~/cassandra
ant