mkdir -p build
mkdir -p build/app
mkdir -p build/giant-explorer
mkdir -p build/ping

#compile sml
echo -e "\033[35;106m compile sml \033[0m"
cd sml
bash gradlew plugin:publish
lr=$?
if [ $lr -ne 0 ]; then
echo -e "\033[101m build failed \033[0m"
exit $lr
fi


#compile app giant-explorer ping
echo
echo -e "\033[35;106m build common-ui-list \033[0m"
cd ../common-ui-list-structure
bash gradlew build
lr=$?
if [ $lr -ne 0 ]; then
echo -e "\033[101m build failed \033[0m"
exit $lr
fi
cp app/build/outputs/apk/release/*.apk ../build/app/
cp app/ping/build/outputs/apk/release/*.apk ../build/ping/
cp app/giant-explorer/build/outputs/apk/release/*.apk ../build/giant-explorer/

echo -e "\033[30;102m build app done \033[0m"
