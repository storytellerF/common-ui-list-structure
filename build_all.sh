mkdir -p build
mkdir -p build/li
mkdir -p build/yue
mkdir -p build/yue-html
mkdir -p build/app
mkdir -p build/giant-explorer
mkdir -p build/ping

bash build_app.sh
bash build_plugins.sh

echo -e "\033[1;102m build all done \033[0m"
