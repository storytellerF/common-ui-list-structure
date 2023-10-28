#!/bin/sh
. ./common.sh
    
mkdir -p build
# mkdir -p build/common-ui-list
mkdir -p build/giant-explorer
mkdir -p build/ping

#compile sml
#todo 统一和gradle 使用同一个变量

if [ "$1" != "cache" ]; then

    # cleanCache common-ui-list
    cd examples
    cleanCache GiantExplorer
    cleanCache Ping
    cd ..

fi

#if [ "$2" = "sml" ]; then
#    cd SML
#    customBuild sml ":plugin:publish" $1
#    cd ..
#fi


#compile common-ui-list
# buildApp common-ui-list app common-ui-list cache

cd examples
#compile giant-explorer ping
buildApp2 GiantExplorer giant-explorer giant-explorer cache
buildApp2 Ping ping ping cache


printEndLabel app
