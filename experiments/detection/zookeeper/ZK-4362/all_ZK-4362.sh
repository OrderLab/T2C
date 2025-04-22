#! /bin/bash

# sudo ./zookeeper/ZK-4362/all_ZK-4362.sh /home/dparikesit/Projects/garudaAce/zookeeper

"$( dirname -- "$( readlink -f -- "$0"; )"; )/install_ZK-4362.sh" "$1" || exit
"$( dirname -- "$( readlink -f -- "$0"; )"; )/trigger_ZK-4362.sh" "$1" || exit
"$( dirname -- "$( readlink -f -- "$0"; )"; )/kill.sh" "$1" "$2" || exit