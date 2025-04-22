#! /bin/bash

# sudo ./zookeeper/ZK-3832/all_ZK-4362.sh <zookeeper absolute path>

"$( dirname -- "$( readlink -f -- "$0"; )"; )/install_ZK-4325.sh" "$1" || exit
"$( dirname -- "$( readlink -f -- "$0"; )"; )/trigger_ZK-4325.sh" "$1" || exit
"$( dirname -- "$( readlink -f -- "$0"; )"; )/kill.sh" "$1" "$2" || exit