source ../../common.sh
mkdir -p build
#!/bin/bash

command_name="zip"
command_path=$(command -v $command_name)

if [ -x "$command_path" ]; then
  zip build/yue-html.zip src/index.html src/imgTouchCanvas.js config
else
    #windows 没有可靠的压缩指令，暂时仅打包
  tar -cf build/yue-html.zip src/index.html src/imgTouchCanvas.js config
fi


cd dispatcher
invokeClean dispatcher "gradlew clean" $1
sh gradlew installDist
#invoke command
build/install/dispatcher/bin/dispatcher