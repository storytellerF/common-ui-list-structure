mkdir -p build
mkdir -p build/app
mkdir -p build/giant-explorer
mkdir -p build/ping

#compile sml
echo -e "\033[1;101m compile sml \033[0m"
cd sml
bash gradlew publish


#compile app giant-explorer ping
echo
echo -e "\033[1;101m build common-ui-list \033[0m"
cd ../common-ui-list-structure
bash gradlew build
cp app/build/outputs/apk/release/*.apk ../build/app/
cp app/ping/build/outputs/apk/release/*.apk ../build/ping/
cp app/giant-explorer/build/outputs/apk/release/*.apk ../build/giant-explorer/

echo -e "\033[1;102m build app done \033[0m"
