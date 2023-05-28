mkdir -p build
zip build/yue-html.zip src/index.html src/imgTouchCanvas.js config
cd dispatcher
./gradlew clean installDist
build/install/dispatcher/bin/dispatcher