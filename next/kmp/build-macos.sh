./gradlew :desktopApp:createReleaseDistributable
#./gradlew :desktopApp:notarizeReleasePkg --info
cd app/desktopApp/build/compose/binaries/main-release/
mkdir pkg
productbuild --component app/DwebBrowser.app /Applications --sign "3rd Party Mac Developer Installer: Instinct Blockchain Technology (Malta) Limited (F9M4UWUYYN)" --keychain "/Users/kzf/Library/Keychains/login.keychain-db" --product app/DwebBrowser.app/Contents/Info.plist pkg/DwebBrowser.pkg
# productbuild --component app/DwebBrowser-x86.app /Applications --sign "3rd Party Mac Developer Installer: Instinct Blockchain Technology (Malta) Limited (F9M4UWUYYN)" --keychain "~/Library/Keychains/login.keychain-db" --product app/DwebBrowser-x86.app/Contents/Info.plist pkg/DwebBrowser-x86.pkg

# /usr/bin/xcrun notarytool submit --wait --apple-id kezhaofeng@bnqkl.cn --team-id "F9M4UWUYYN" /Users/kzf/Development/GitHub/dweb_browser/next/kmp/app/desktopApp/build/compose/binaries/main-release/dmg/DwebBrowser-3.6.0601.dmg
# xcrun notarytool log 9bbd3047-052a-4ba3-8703-f77c3a10b64b --apple-id kezhaofeng@bnqkl.cn --team-id "F9M4UWUYYN"   
# /usr/bin/xcrun stapler staple /Users/kzf/Development/GitHub/dweb_browser/next/kmp/app/desktopApp/build/compose/binaries/main-release/dmg/DwebBrowser-3.6.0601.dmg
