#! /bin/bash

# sudo ./zookeeper/ZK-3832/all_ZK-3832.sh <zookeeper absolute path>

"$( dirname -- "$( readlink -f -- "$0"; )"; )/install_ZK-3832.sh" "$1" || exit
"$( dirname -- "$( readlink -f -- "$0"; )"; )/trigger_ZK-3832.sh" "$1" || exit
"$( dirname -- "$( readlink -f -- "$0"; )"; )/kill.sh" "$1" "$2" || exit