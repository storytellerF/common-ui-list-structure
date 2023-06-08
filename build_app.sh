#!/bin/sh
. ./common.sh
    
mkdir -p build
mkdir -p build/common-ui-list
mkdir -p build/giant-explorer
mkdir -p build/ping

#compile sml
#todo 统一和gradle 使用同一个变量

if [ "$2" = "sml" ]; then
    cd SML
    customBuild sml ":plugin:publish" $1
    cd ..
fi


#compile common-ui-list
buildApp common-ui-list app common-ui-list $1

cd examples
#compile giant-explorer ping
buildApp2 GiantExplorer giant-explorer giant-explorer $1
buildApp2 Ping ping ping $1


printEndLabel app
