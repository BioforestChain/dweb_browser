
# cat ./info.log

node -e "console.log(process.env)"
echo "Define parameters"
# xcodebuild -showsdks
IOS_SDK_VERSION="16.4"
SWIFT_PROJECT_NAME="BrowserFramework"

echo "Build iOS framework for device"

rm -Rf "../XCFrameworks/$SWIFT_PROJECT_NAME.xcframework"

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

cp -Rf "../XCFrameworks/ApiDefinitions.cs" "../SwiftUIBindingMAUI/SwiftUIBindingMAUI/"
echo "Done!"