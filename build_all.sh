#!/bin/sh
. ./common.sh

sh build_app.sh $1 $2
checkLastResult app $?

sh build_plugins.sh $1 $2
checkLastResult plugins $?

printEndLabel all
