mkdir -p build
mkdir -p build/li
mkdir -p build/yue
mkdir -p build/yue-html
mkdir -p build/app
mkdir -p build/giant-explorer
mkdir -p build/ping

#compile sml
cd sml
bash gradlew publish
#compile giant-explorer ping
cd ../common-ui-list-structure
bash gradlew build
cp app/build/outputs/apk/release/*.apk ../build/app/
cp app/ping/build/outputs/apk/release/*.apk ../build/ping/
cp app/giant-explorer/build/outputs/apk/release/*.apk ../build/giant-explorer/

cd ../giant-explorer/yue
bash gradlew build
cp app/build/outputs/apk/release/*.apk ../../build/yue/
cd ../li
bash gradlew build
cp app/build/outputs/apk/release/*.apk ../../build/li/
cd ../yue-html
bash dispatch.sh
cp -r build ../../build/yue-html/

