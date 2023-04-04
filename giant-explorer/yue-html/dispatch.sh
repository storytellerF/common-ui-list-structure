mkdir -p build
zip build/yue-html.zip index.html imgTouchCanvas.js config
cd dispatcher
./gradlew installDist
build/install/dispatcher/bin/dispatcher