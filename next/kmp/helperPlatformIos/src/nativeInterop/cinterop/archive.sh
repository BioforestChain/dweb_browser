xcodebuild archive \
    -project DwebPlatformIosKit.xcodeproj \
    -scheme DwebPlatformIosKit \
    -destination "generic/platform=iOS" \
    -archivePath "archives/DwebPlatformIosKit-iOS" \
    SKIP_INSTALL=NO \
    BUILD_LIBRARY_FOR_DISTRIBUTION=YES

xcodebuild archive \
    -project DwebPlatformIosKit.xcodeproj \
    -scheme DwebPlatformIosKit \
    -destination "generic/platform=iOS Simulator" \
    -archivePath "archives/DwebPlatformIosKit-iOS_Simulator" \
    SKIP_INSTALL=NO \
    BUILD_LIBRARY_FOR_DISTRIBUTION=YES
