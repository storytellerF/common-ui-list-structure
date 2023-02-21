zip build/yue-html.zip index.html imgTouchCanvas.js
cd dispatcher
./gradlew installDist
build/install/dispatcher/bin/dispatcher .