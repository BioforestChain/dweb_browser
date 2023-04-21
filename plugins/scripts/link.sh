#!/bin/bash

# 快速npm link 方便测试

dirctory=("plugin")

for dir in ${dirctory[@]}
do
    cd ./build/$dir/ && pwd && npm link && cd ../../
done


cd ./demo/ && npm link @bfex/plugin

