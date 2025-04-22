#! /bin/bash

echo "Cleaning up"
user=$(whoami)
pgrep -u "$user" -f cassandra | xargs kill -9