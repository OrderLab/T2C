#run this scripts on all nodes
#this script is hardcoded, need to be paramerized
if [ "$(hostname)" = "razor14" ]; then
  #important, don't start right away, wait for msg arrive
  sleep 5
  touch fault.ZK-2355
  echo "sleep waiting for session expire"
  #right after it expires
  sleep 35
  rm fault.ZK-2355
  bin/zkCli.sh <<EOF2
  ls /
EOF2
  echo "if you see /q1 exists, the reproducing succeed"
fi

if [ "$(hostname)" = "razor19" ]; then
  bin/zkCli.sh <<EOF
  create -e /q1 11
EOF
fi