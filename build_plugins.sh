#!/bin/sh
. ./common.sh

mkdir -p build
mkdir -p build/li
mkdir -p build/yue
mkdir -p build/yue-html

cd examples/GiantExplorer
customBuild giant-explorer-plugin giant-explorer-plugin-core:build $1
cd ../../

cd giant-explorer

buildApp2 yue app yue $1
buildApp2 li app li $1

cd yue-html
printStartLabel yue-html
sh dispatch.sh $1
checkLastResult yue-html $?
cp -r build ../../build/yue-html/

printEndLabel plugin
