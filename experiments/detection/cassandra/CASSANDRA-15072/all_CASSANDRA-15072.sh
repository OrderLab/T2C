#! /bin/bash

# ./all_CASSANDRA-15072 <cassandra dir>

"$( dirname -- "$( readlink -f -- "$0"; )"; )/install_CASSANDRA-15072.sh" "$1" || exit
"$( dirname -- "$( readlink -f -- "$0"; )"; )/trigger_CASSANDRA-15072.sh" "$1" || exit
"$( dirname -- "$( readlink -f -- "$0"; )"; )/kill.sh" || exit