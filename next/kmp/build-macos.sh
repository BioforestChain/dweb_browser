./gradlew :desktopApp:createReleaseDistributable
cd app/desktopApp/build/compose/binaries/main-release/
mkdir pkg
productbuild --component app/DwebBrowser.app /Applications --sign "3rd Party Mac Developer Installer: Instinct Blockchain Technology (Malta) Limited (F9M4UWUYYN)" --keychain "~/Library/Keychains/login.keychain-db" --product app/DwebBrowser.app/Contents/Info.plist pkg/DwebBrowser.pkg
