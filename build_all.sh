mkdir -p build
mkdir -p build/li
mkdir -p build/yue
mkdir -p build/yue-html
mkdir -p build/app
mkdir -p build/giant-explorer
mkdir -p build/ping

#compile sml
echo -e "\033[1;101m compile sml \033[0m"
cd sml
bash gradlew publish
echo
echo -e "\033[1;101m build common-ui-list \033[0m"
#compile giant-explorer ping
cd ../common-ui-list-structure
bash gradlew build
cp app/build/outputs/apk/release/*.apk ../build/app/
cp app/ping/build/outputs/apk/release/*.apk ../build/ping/
cp app/giant-explorer/build/outputs/apk/release/*.apk ../build/giant-explorer/
echo
echo -e "\033[1;101m build yue \033[0m"
cd ../giant-explorer/yue
bash gradlew build
cp app/build/outputs/apk/release/*.apk ../../build/yue/
echo
echo -e "\033[1;101m build li \033[0m"
cd ../li
bash gradlew build
cp app/build/outputs/apk/release/*.apk ../../build/li/
echo
echo -e "\033[1;101m build yue-html \033[0m"
cd ../yue-html
bash dispatch.sh
cp -r build ../../build/yue-html/
echo -e "\033[1;102m build done \033[0m"
