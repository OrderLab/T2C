#! /bin/bash

# ./all_CASSANDRA-14873 <cassandra dir>

"$( dirname -- "$( readlink -f -- "$0"; )"; )/install_CASSANDRA-14873.sh" "$1" || exit
"$( dirname -- "$( readlink -f -- "$0"; )"; )/trigger_CASSANDRA-14873.sh" "$1" || exit
"$( dirname -- "$( readlink -f -- "$0"; )"; )/kill.sh" || exit