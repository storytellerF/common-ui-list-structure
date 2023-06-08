source ../../common.sh
mkdir -p build
zip build/yue-html.zip src/index.html src/imgTouchCanvas.js config
cd dispatcher
invokeClean dispatcher "gradlew clean" $1
sh gradlew installDist
#invoke command
build/install/dispatcher/bin/dispatcher