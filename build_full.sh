#!/bin/sh
. ./common.sh

# cd builder
# customBuild builder installDist $1
# sh build/install/builder/bin/builder
# checkLastResult agp-version $?
# cd ..

sh build_app.sh $1 $2
checkLastResult app $?

sh build_plugins.sh $1 $2
checkLastResult plugins $?

printEndLabel all
