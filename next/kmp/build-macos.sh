./gradlew :desktopApp:createReleaseDistributable
#./gradlew :desktopApp:notarizeReleasePkg --info
cd app/desktopApp/build/compose/binaries/main-release/

# mkdir pkg
# productbuild --component app/DwebBrowser.app /Applications --sign "Developer ID Application: Instinct Blockchain Technology (Malta) Limited (F9M4UWUYYN)" --keychain "/Users/kzf/Library/Keychains/login.keychain-db" --product app/DwebBrowser.app/Contents/Info.plist pkg/DwebBrowser.pkg
# pkgutil --check-signature pkg/DwebBrowser.pkg
# productbuild --component app/DwebBrowser-x86.app /Applications --sign "Developer ID Application: Instinct Blockchain Technology (Malta) Limited (F9M4UWUYYN)" --keychain "~/Library/Keychains/login.keychain-db" --product app/DwebBrowser-x86.app/Contents/Info.plist pkg/DwebBrowser-x86.pkg

# ## 重签名
# codesign -vvv --deep --strict app/DwebBrowser.app 
# security find-identity -v -p codesigning
# codesign --deep --force --verify --verbose --sign "Apple Development: kezhaofeng@bnqkl.cn (C8QLFRQQG7)" --entitlements ../../../../default-entitlements.plist app/DwebBrowser.app
# spctl -vvv --assess --raw --type exec app/DwebBrowser.app/Contents/MacOS/DwebBrowser

# ## 公证
# /usr/bin/ditto -c -k app/DwebBrowser.app app/DwebBrowser.zip
# /usr/bin/xcrun notarytool submit --wait --apple-id kezhaofeng@bnqkl.cn --team-id "F9M4UWUYYN" app/DwebBrowser.zip
# # xcrun notarytool log 951c0427-4b8a-4372-8f29-7ba8f8ec9664 --apple-id kezhaofeng@bnqkl.cn --team-id "F9M4UWUYYN" developer_log.json
# # /usr/bin/xcrun stapler staple app/DwebBrowser.app

# create-dmg \
#   --volname "Dweb Browser Installer" \
#   --volicon "app/desktopApp/src/desktopMain/res/icons/mac/icon.icns" \
#   --window-pos 200 120 \
#   --window-size 800 400 \
#   --icon-size 100 \
#   --icon "DwebBrowser.app" 200 190 \
#   --hide-extension "DwebBrowser.app" \
#   --app-drop-link 600 185 \
#   "Dweb-Browser-Installer.dmg" \
#   "app/desktopApp/build/compose/binaries/main-release/app"