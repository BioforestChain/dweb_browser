./gradlew :desktopApp:createReleaseDistributable -PreleaseBuild=true
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
# /usr/bin/ditto -c -k app-arm64/DwebBrowser.app zip/DwebBrowser-arm64.zip
# /usr/bin/xcrun notarytool submit --wait --apple-id kezhaofeng@bnqkl.cn --team-id "F9M4UWUYYN" zip/DwebBrowser-arm64.zip
# # xcrun notarytool log 59e68828-dd39-4a62-853d-63eca031ea10 --apple-id kezhaofeng@bnqkl.cn --team-id "F9M4UWUYYN" developer_log.json
# # /usr/bin/xcrun stapler staple app-arm64/DwebBrowser.app

# ## 公证
# /usr/bin/ditto -c -k app-x86_64/DwebBrowser.app zip/DwebBrowser-x86_64.zip
# /usr/bin/xcrun notarytool submit --wait --apple-id kezhaofeng@bnqkl.cn --team-id "F9M4UWUYYN" zip/DwebBrowser-x86_64.zip
# # xcrun notarytool log c60c0970-15cb-41d5-bca7-72564f8b1596 --apple-id kezhaofeng@bnqkl.cn --team-id "F9M4UWUYYN" developer_log.json
# # /usr/bin/xcrun stapler staple app-x86_64/DwebBrowser.app

create-dmg \
  --volname "Dweb Browser Installer" \
  --volicon "../../../../src/desktopMain/res/icons/mac/icon.icns" \
  --window-pos 200 120 \
  --window-size 800 400 \
  --icon-size 100 \
  --icon "DwebBrowser.app" 200 190 \
  --hide-extension "DwebBrowser.app" \
  --app-drop-link 600 185 \
  "DwebBrowser-x86_64.dmg" \
  "app-x86_64"