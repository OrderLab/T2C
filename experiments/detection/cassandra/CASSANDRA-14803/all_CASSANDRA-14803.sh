#! /bin/bash

# ./all_CASSANDRA-14803 <cassandra dir>

"$( dirname -- "$( readlink -f -- "$0"; )"; )/install_CASSANDRA-14803.sh" "$1" || exit
"$( dirname -- "$( readlink -f -- "$0"; )"; )/trigger_CASSANDRA-14803.sh" "$1" || exit
"$( dirname -- "$( readlink -f -- "$0"; )"; )/kill.sh" || exit