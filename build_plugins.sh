
mkdir -p build
mkdir -p build/li
mkdir -p build/yue
mkdir -p build/yue-html

echo -e "\033[35;106m build giant-explorer-plugin-core \033[0m"
cd common-ui-list-structure
bash gradlew app:giant-explorer-plugin-core:build

echo
echo -e "\033[35;106m build yue \033[0m"
cd ../giant-explorer/yue
bash gradlew build
lr=$?
if [ $lr -ne 0 ]; then
echo -e "\033[101m build yue failed \033[0m"
exit $lr
fi
cp app/build/outputs/apk/release/*.apk ../../build/yue/


echo
echo -e "\033[35;106m build li \033[0m"
cd ../li
bash gradlew build
lr=$?
if [ $lr -ne 0 ]; then
echo -e "\033[101m build li failed \033[0m"
exit $lr
fi
cp app/build/outputs/apk/release/*.apk ../../build/li/


echo
echo -e "\033[35;106m build yue-html \033[0m"
cd ../yue-html
bash dispatch.sh
lr=$?
if [ $lr -ne 0 ]; then
echo -e "\033[101m build yue-html failed \033[0m"
exit $lr
fi
cp -r build ../../build/yue-html/
echo -e "\033[30;102m build plugin done \033[0m"
