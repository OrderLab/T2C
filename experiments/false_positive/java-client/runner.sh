#! /bin/bash

repo="/u/vqx2dc/lein_repo"
version="1.0.31"

if [[ $# -eq 1 ]] ; then
    if [[ $1 == "package" ]] ; then
        mvn package -DskipTests
    elif [[ $1 == "deploy" ]] ; then
        mkdir -p $repo && mvn deploy:deploy-file -Dfile=target/java-client-$version.jar -DartifactId=java-client -Dversion=$version -DgroupId=edu.uva.liftlab -Dpackaging=jar -Durl=file:$repo
    fi
else
    echo "Invalid arguments"
fi