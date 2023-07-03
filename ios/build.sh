#!/bin/zsh

IOS_SDK_VERSION="16.4"
SWIFT_FRAMEWORk_NAME="DwebBrowserFramework"
SWIFT_PROJECT_NAME="DwebBrowser"
isRelease=false


print_yellow() {
  printf "\e[33m$1\e[m"
}

build() {
  if [[ "$1" = false ]]; then
    echo "Build in Debug"
    dotnet build --no-incremental
  else
    echo "Build in Release"
    dotnet build --no-incremental -c:Release
  fi
}
echo "Build iOS framework for device"

# rm -Rf "DwebBrowserFramework/DwebBrowserFramework/"


# cp -Rf "DwebBrowser/DwebBrowser/" "DwebBrowserFramework/DwebBrowserFramework/"

echo "Create xcframework"

rm -Rf "XCFrameworks/$SWIFT_FRAMEWORk_NAME.xcframework"

cd $SWIFT_PROJECT_NAME

xcodebuild archive \
 -scheme $SWIFT_FRAMEWORk_NAME \
 -archivePath ../XCFrameworks/$SWIFT_FRAMEWORk_NAME-ios.xcarchive \
 -sdk iphoneos \
 SKIP_INSTALL=NO

 print_yellow "\n编译真机:\n"

xcodebuild archive \
 -scheme $SWIFT_FRAMEWORk_NAME \
 -archivePath ../XCFrameworks/$SWIFT_FRAMEWORk_NAME-sim.xcarchive \
 -sdk iphonesimulator \
 SKIP_INSTALL=NO

print_yellow "\n合并framework\n"

xcodebuild -create-xcframework \
 -framework ../XCFrameworks/$SWIFT_FRAMEWORk_NAME-sim.xcarchive/Products/Library/Frameworks/$SWIFT_FRAMEWORk_NAME.framework \
 -framework ../XCFrameworks/$SWIFT_FRAMEWORk_NAME-ios.xcarchive/Products/Library/Frameworks/$SWIFT_FRAMEWORk_NAME.framework \
 -output ../XCFrameworks/$SWIFT_FRAMEWORk_NAME.xcframework

print_yellow "\n生成绑定库\n"

sharpie bind \
 -sdk iphoneos$IOS_SDK_VERSION \
 -output ../XCFrameworks/ \
 -namespace $SWIFT_FRAMEWORk_NAME \
 -framework ../XCFrameworks/$SWIFT_FRAMEWORk_NAME.xcframework/ios-arm64/$SWIFT_FRAMEWORk_NAME.framework

# 设置要操作的文件路径
file_path="../XCFrameworks/ApiDefinitions.cs"
# 临时文件用于保存处理后的内容
tmp_file="../XCFrameworks/ApiDefinitions_tmp.cs"
# 循环删除文件中的指定内容，直到删除完毕

print_yellow "\ndelete static\n"

sed 's/^[[:space:]]\[Static\]/  [Category]/g' "$file_path" > "$tmp_file"

# 将处理后的内容覆盖原始文件
mv "$tmp_file" "$file_path"

print_yellow "\Category static\n"

while grep -q '\[Category\]' "$file_path"; do
    awk '/\[Category\]/ { p = 1; next } p && /^[[:space:]]*}$/ { p = 0; next } !p' "$file_path" > "$tmp_file"
    mv "$tmp_file" "$file_path"
done



 print_yellow "\nApiDefinitions.cs replace finish\n"

 cp -Rf "../XCFrameworks/ApiDefinitions.cs" "../SwiftUIBindingMAUI/SwiftUIBindingMAUI/"

 print_yellow "\nxcframework and cs file finish\n"

 cd ..
 
 cd SwiftUIBindingMAUI

 build $isRelease

echo "Done!"