#!/bin/sh

#  RecoverBuildConfig.sh
#  DwebBrowser
#
#  Created by instinct on 2024/3/5.
#  Copyright © 2024 orgName. All rights reserved.

function update_build_flags() {
    local file_name="$1"
    local flag1="$2"
    local flag2="$3"

    if ! grep -q -- "$flag1" "$file_name"; then
        sed -i '' "s/OTHER_CFLAGS = \"\([^\"]*\)\"/OTHER_CFLAGS = \"\1 $flag1\"/g" "$file_name"
        if [ $? -eq 0 ]; then
            echo "$file_name set: OTHER_CFLAGS" # >> log.txt
        else
            echo "Failed to set OTHER_CFLAGS for $file_name" # >> log.txt
        fi
    fi

    if ! grep -q -- "$flag2" "$file_name"; then
        sed -i '' "s/OTHER_SWIFT_FLAGS = \"\([^\"]*\)\"/OTHER_SWIFT_FLAGS = \"\1 $flag2\"/g" "$file_name"
        if [ $? -eq 0 ]; then
            echo "$file_name set: OTHER_SWIFT_FLAGS" # >> log.txt
        else
            echo "Failed to set OTHER_SWIFT_FLAGS for $file_name" # >> log.txt
        fi
    fi
}

function recover_build_flags() {
    echo "recover_build_flags do:" # >> log.txt
    echo "input1: $1" # >> log.txt
    echo "input2: $2" # >> log.txt
    local file_name="$1"
    local flag1=" $2" # 插入的时候多了一个空格，这边得还回去
    local flag2=" $3" # 插入的时候多了一个空格，这边得还回去
    
    sed -i '' "s/$flag1//g" "$file_name"
    sed -i '' "s/$flag2//g" "$file_name"
}

#OrderPhaseToBuild
#OrderPhaseToClear
function isOrderPhaseBuild() {
    echo "isOrderPhaseBuild do:" # >> log.txt
    echo "input1: $1" # >> log.txt
    echo "input2: $2" # >> log.txt
    
    local filename="$1"
    
    if grep -q "OrderPhaseToBuild" "$filename"; then
        sed -i '' 's/OrderPhaseToBuild/OrderPhaseToClear/g' "$filename"
        return 0
    elif grep -q "OrderPhaseToClear" "$filename"; then
        sed -i '' 's/OrderPhaseToClear/OrderPhaseToBuild/g' "$filename"
        return 1
    else
        return 1
    fi
}

function update_dweb_order_out_put() {
    echo "update_dweb_order_out_put do:" # >> log.txt
    echo "input1: $1" # >> log.txt
    echo "input2: $2" # >> log.txt
    
    local filename="$1"
    local replace_str="DwebOrder:$2"
    sed -i '' "s#<string>DwebOrder<\/string>#<string>$replace_str<\/string>#g" "$filename"
}

function recover_dweb_order_out_put() {
    echo "recover_dweb_order_out_put do:" # >> log.txt
    echo "input1: $1" # >> log.txt
    local filename="$1"
    sed -i '' 's#<string>DwebOrder[^<]*</string>#<string>DwebOrder</string>#g' "$filename"
}

function update_dweb_mode() {
    echo "update_dweb_mode do:" # >> log.txt
    echo "input1: $1" # >> log.txt
    echo "input2: $2" # >> log.txt
    local filename="$1"
    local replace_str="DwebMode:$2"
    sed -i '' "s#<string>DwebMode<\/string>#<string>$replace_str<\/string>#g" "$filename"
}

function recover_dweb_mode() {
    echo "recover_dweb_mode do:" # >> log.txt
    echo "input1: $1" # >> log.txt
    
    local filename="$1"
    sed -i '' 's#<string>DwebMode[^<]*</string>#<string>DwebMode</string>#g' "$filename"
}

function update_dweb_git() {
    echo "update_dweb_git do:" # >> log.txt
    echo "input1: $1" # >> log.txt
    
    local filename="$1"
    local commit_info=$(git log -1 --pretty=format:"%H %ci" HEAD)
    commit_info=$(echo $commit_info | awk '{print $1, $2, $3}')
    
    echo "当前最新的 commit ID 是：$commit_info"
    local replace_str="DwebGit:$commit_info"
    sed -i '' "s#<string>DwebGit<\/string>#<string>$replace_str<\/string>#g" "$filename"
}

function recover_dweb_git() {
    echo "recover_dweb_git do:" # >> log.txt
    echo "input1: $1" # >> log.txt
    
    local filename="$1"
    sed -i '' 's#<string>DwebGit[^<]*</string>#<string>DwebGit</string>#g' "$filename"
}

function watch() {
    file_to_watch=$1

    # 使用 kqueue 监听文件修改事件
    echo "Monitoring file $file_to_watch for changes..." # >> log.txt
    while true; do
        if kqueue_file=$(kqueue -F "$file_to_watch" -e write); then
            echo "File $file_to_watch has been modified" # >> log.txt
        fi
    done
}
