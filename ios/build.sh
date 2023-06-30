#!/bin/zsh

IOS_SDK_VERSION="16.4"
SWIFT_PROJECT_NAME="DwebBrowserFramework"
isRelease=false


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

rm -Rf "DwebBrowserFramework/DwebBrowserFramework/"


cp -Rf "DwebBrowser/DwebBrowser/" "DwebBrowserFramework/DwebBrowserFramework/"

echo "Create xcframework"

rm -Rf "XCFrameworks/$SWIFT_PROJECT_NAME.xcframework"

cd $SWIFT_PROJECT_NAME

xcodebuild archive \
 -scheme $SWIFT_PROJECT_NAME \
 -archivePath ../XCFrameworks/$SWIFT_PROJECT_NAME-ios.xcarchive \
 -sdk iphoneos \
 SKIP_INSTALL=NO

xcodebuild archive \
 -scheme $SWIFT_PROJECT_NAME \
 -archivePath ../XCFrameworks/$SWIFT_PROJECT_NAME-sim.xcarchive \
 -sdk iphonesimulator \
 SKIP_INSTALL=NO

xcodebuild -create-xcframework \
 -framework ../XCFrameworks/$SWIFT_PROJECT_NAME-sim.xcarchive/Products/Library/Frameworks/$SWIFT_PROJECT_NAME.framework \
 -framework ../XCFrameworks/$SWIFT_PROJECT_NAME-ios.xcarchive/Products/Library/Frameworks/$SWIFT_PROJECT_NAME.framework \
 -output ../XCFrameworks/$SWIFT_PROJECT_NAME.xcframework

sharpie bind \
 -sdk iphoneos$IOS_SDK_VERSION \
 -output ../XCFrameworks/ \
 -namespace $SWIFT_PROJECT_NAME \
 -framework ../XCFrameworks/$SWIFT_PROJECT_NAME.xcframework/ios-arm64/$SWIFT_PROJECT_NAME.framework

# 设置要操作的文件路径
file_path="../XCFrameworks/ApiDefinitions.cs"
# 临时文件用于保存处理后的内容
tmp_file="../XCFrameworks/ApiDefinitions_tmp.cs"
# 循环删除文件中的指定内容，直到删除完毕
while grep -q '\[Category\]' "$file_path"; do
    awk '/\[Category\]/ { p = 1; next } p && /^[[:space:]]*}$/ { p = 0; next } !p' "$file_path" > "$tmp_file"
    mv "$tmp_file" "$file_path"
done

 echo "ApiDefinitions.cs replace finish"

 cp -Rf "../XCFrameworks/ApiDefinitions.cs" "../SwiftUIBindingMAUI/SwiftUIBindingMAUI/"

 echo "xcframework and cs file finish"

 cd ..
 
 cd SwiftUIBindingMAUI

 build $isRelease

echo "Done!"