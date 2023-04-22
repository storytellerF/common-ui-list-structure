mkdir -p build
mkdir -p build/li
mkdir -p build/yue
mkdir -p build/yue-html

echo -e "\033[1;101m build giant-explorer-plugin-core \033[0m"
cd common-ui-list-structure
bash gradlew app:giant-explorer-plugin-core:build

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
echo -e "\033[1;102m build plugin done \033[0m"
