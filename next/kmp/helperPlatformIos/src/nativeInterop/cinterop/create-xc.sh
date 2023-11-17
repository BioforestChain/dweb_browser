xcodebuild -create-xcframework \
    -archive archives/DwebPlatformIosKit-iOS.xcarchive -framework DwebPlatformIosKit.framework \
    -archive archives/DwebPlatformIosKit-iOS_Simulator.xcarchive -framework DwebPlatformIosKit.framework \
    -output xcframeworks/DwebPlatformIosKit.xcframework
# -archive archives/DwebPlatformIosKit-macOS.xcarchive -framework DwebPlatformIosKit.framework
# -archive archives/DwebPlatformIosKit-Mac_Catalyst.xcarchive -framework DwebPlatformIosKit.framework
