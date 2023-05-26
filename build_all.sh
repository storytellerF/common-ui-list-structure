mkdir -p build
mkdir -p build/li
mkdir -p build/yue
mkdir -p build/yue-html
mkdir -p build/app
mkdir -p build/giant-explorer
mkdir -p build/ping

bash build_app.sh
if [ $? -ne 0 ]; then
echo -e "\033[31m build failed \033[0m"
exit $?
fi
echo
bash build_plugins.sh
if [ $? -ne 0 ]; then
echo -e "\033[31m build failed \033[0m"
exit $?
fi

echo
echo -e "\033[30;102m build all done \033[0m"
