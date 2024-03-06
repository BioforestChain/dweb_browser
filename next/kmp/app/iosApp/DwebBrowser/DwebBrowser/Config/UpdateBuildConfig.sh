#!/bin/bash

#exec &> log.txt

function prepare() {
    rm -rf log.txt
    touch log.txt
    date "+%Y/%m/%d %H:%M:%S" # >> log.txt
}

function build() {
    date "+%Y/%m/%d %H:%M:%S" # >> log.txt
    
    ####
    echo "update info.plist start" # >> log.txt

    update_dweb_order_out_put "$1/DwebBrowser/DwebBrowser/Info.plist" "$1/DwebBrowser/DwebBrowser/Config/LinkSort.order"
    update_dweb_mode "$1/DwebBrowser/DwebBrowser/Info.plist" "DwebOrderDumping"
    update_dweb_git "$1/DwebBrowser/DwebBrowser/Info.plist"
    
    echo "update info.plist end" # >> log.txt


    ####
    echo "update build config start" # >> log.txt

    DwebBrowserFile="$1/DwebBrowser/DwebBrowser.xcodeproj/project.pbxproj"
    DwebWebBrowserFile="$1/DwebWebBrowser/DwebWebBrowser.xcodeproj/project.pbxproj"
    DwebPlatformIosKitFile="$1/DwebPlatformIosKit/DwebPlatformIosKit.xcodeproj/project.pbxproj"
    DwebBrowserCommonFile="$1/DwebBrowserCommon/DwebBrowserCommon.xcodeproj/project.pbxproj"

    update_build_flags $DwebBrowserFile "-fsanitize-coverage=func,trace-pc-guard" "-sanitize-coverage=func -sanitize=undefined"
    update_build_flags $DwebWebBrowserFile "-fsanitize-coverage=func,trace-pc-guard" "-sanitize-coverage=func -sanitize=undefined"
    update_build_flags $DwebPlatformIosKitFile "-fsanitize-coverage=func,trace-pc-guard" "-sanitize-coverage=func -sanitize=undefined"
    update_build_flags $DwebBrowserCommonFile "-fsanitize-coverage=func,trace-pc-guard" "-sanitize-coverage=func -sanitize=undefined"

    echo "update build config end" # >> log.txt
}

function clear() {
###
    echo "do clear" # >> log.txt
    echo "input1: $1" # >> log.txt
    
    recover_build_flags "$1/DwebBrowser/DwebBrowser.xcodeproj/project.pbxproj" "-fsanitize-coverage=func,trace-pc-guard" "-sanitize-coverage=func -sanitize=undefined"
    recover_build_flags "$1/DwebWebBrowser/DwebWebBrowser.xcodeproj/project.pbxproj" "-fsanitize-coverage=func,trace-pc-guard" "-sanitize-coverage=func -sanitize=undefined"
    recover_build_flags "$1/DwebPlatformIosKit/DwebPlatformIosKit.xcodeproj/project.pbxproj" "-fsanitize-coverage=func,trace-pc-guard" "-sanitize-coverage=func -sanitize=undefined"
    recover_build_flags "$1/DwebBrowserCommon/DwebBrowserCommon.xcodeproj/project.pbxproj" "-fsanitize-coverage=func,trace-pc-guard" "-sanitize-coverage=func -sanitize=undefined"


    recover_dweb_order_out_put "$1/DwebBrowser/DwebBrowser/Info.plist"
    recover_dweb_mode "$1/DwebBrowser/DwebBrowser/Info.plist"
    recover_dweb_git "$1/DwebBrowser/DwebBrowser/Info.plist"
    
    echo "clear completed" # >> log.txt
    echo -e "\033[32m clear completed \033[0m"
###
}

function completed() {
    kill -KILL $1
    echo -e "\033[32m **************************************** \033[0m"
    echo -e "\033[32m                COMPLETED                 \033[0m"
    echo -e "\033[32m       请检查和提交LinkSort.order文件        \033[0m"
    echo -e "\033[32m **************************************** \033[0m"
}

function findBootedUUID() {
    local str=`xcrun simctl list | grep "Booted"`
    str=${str#*(}
    str=${str%%)*}
    echo "$str"
}

# mode
# 0: 普通模式，clean -> build -> dump
# 1: 快速模式，--> dump

mode=0
if [ "$1" == "q" ]; then
    mode=1
        echo -e "\033[32m **************************************** \033[0m"
        echo -e "\033[32m *               Quick Dump             * \033[0m"
        echo -e "\033[32m **************************************** \033[0m"
    else
        echo -e "\033[32m **************************************** \033[0m"
        echo -e "\033[32m *               Normal Dump            * \033[0m"
        echo -e "\033[32m **************************************** \033[0m"
fi

root_dict=`pwd`
root_dict="$root_dict/../../.."

echo -e "\033[32m root_dict: $root_dict \033[0m"

source "$root_dict/DwebBrowser/DwebBrowser/Config/OrderBuildConfig.sh"

#prepare
clear $root_dict
build $root_dict
cd $root_dict

if [ "$mode" -eq "0" ]; then
    echo -e "\033[34m Xcode clean... \033[0m"
    xcodebuild -workspace DwebBrowser.xcworkspace -scheme DwebBrowser -quiet clean
    echo -e "\033[32m Xcode clean done \033[0m"

    echo -e "\033[34m Xcode build... \033[0m"
    xcodebuild -workspace DwebBrowser.xcworkspace -scheme DwebBrowser -destination 'platform=iOS Simulator,name=iPhone 15 Pro Max' -quiet build
    if [ $? -ne 0 ]; then
        echo -e "\033[31m Xcode build Fail \033[0m"
        exit 1
    fi
    echo -e "\033[32m Xcode build done \033[0m"
fi

bootedUUID=$(findBootedUUID)
echo -e "\033[32m bootedUUID: $bootedUUID \033[0m"

appPath="${HOME}/Library/Developer/Xcode/DerivedData/Build/Products/Debug-iphonesimulator/DwebBrowser.app"
echo -e "\033[32m appPath: $appPath \033[0m"

bundleId="com.instinct.bfexplorer.debug"
echo -e "\033[32m bundleId: $bundleId \033[0m"

if [ ! -e "$appPath" ];then
    echo -e "\033[31m DwebBrowser.app no exist!\033[0m"
    if [ "$mode" == "1" ]; then
        echo -e "\033[31m Please use normal mode! \033[0m"
    fi
    exit 1
fi

xcrun simctl install $bootedUUID $appPath

pid=`xcrun simctl launch $bootedUUID $bundleId`
pid=${pid#*:}
pid=`echo $pid | tr -d [:blank:]`
echo -e "\033[32m pid: $pid \033[0m"

echo -e "\033[34m Waiting Xcode dump...\033[0m"
sleep 3 && clear $root_dict
sleep 1 && completed $pid


#function action() {
#    echo "action do" # >> log.txt
#    isOrderPhaseBuild "$1/DwebBrowser/DwebBrowser/Info.plist"
#
#    if [ $? -eq 0 ]; then
#        echo "do update config" # >> log.txt
#        build $1 $2
#    else
#        echo "do clear" # >> log.txt
#        (sleep 10 && clear $1) &
#    fi
#    echo "$result" # >> log.txt
#}
