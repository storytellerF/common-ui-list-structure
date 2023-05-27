mkdir -p build
mkdir -p build/li
mkdir -p build/yue
mkdir -p build/yue-html
mkdir -p build/app
mkdir -p build/giant-explorer
mkdir -p build/ping

bash build_app.sh
lr=$?
if [ $lr -ne 0 ]; then
echo -e "\033[101m build app failed \033[0m"
exit $lr
fi
echo
bash build_plugins.sh
lr=$?
if [ $lr -ne 0 ]; then
echo -e "\033[101m build plugins failed \033[0m"
exit $lr
fi

echo
echo -e "\033[30;102m build all done \033[0m"
