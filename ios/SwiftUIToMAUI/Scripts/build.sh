#!/bin/zsh
timestamp() {
  date +"%T" # current time
}

print_green() {
  printf "\e[32m$1\e[m"
}

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

print_help(){
  echo "Run ./build without parameters to build ALL PARTS (Swift framework, Binding library, MAUI library). Default behavior."
  echo "\nParameters:"
  echo "-all or --all\t\t - builds all parts (like without parameters)"
  echo "-ios or --ios\t\t - builds Swift framework"
  echo "-b or --bindings\t - builds Bindings library"
  echo "-m or --maui\t\t - builds MAUI library"
  echo "-r or --release\t\t - builds dotnet parts in release"
  echo "-h or --help\t\t - shows help"  
  echo "\nExample:"
  echo "./build.sh -ios -b\ -- builds Swift framework and Bindings library"
  echo "\nSource on GitHub:"
  echo "https://github.com/BioforestChain/dweb_browser"
}

print_green "SCRIPT STARTED at $(timestamp)"

#SETUP BUILD CONFIGURATION

#parameters list
all=true
ios=false
bindings=false
maui=false
isRelease=false

#read parameters one by one
while [[ "$#" -gt 0 ]]
do
case $1 in
    -all|--all)
      all=true;;
    -ios|--ios)
      all=false
      ios=true;;
    -b|--bindings)
      all=false
      bindings=true;;
    -m|--maui)
      all=false
      maui=true;;
    -h|--help)
      print_help
      all=false;;
    -r|--release)
      isRelease=true;;      
    *)
      echo "Unknown parameter passed: $1"
      print_help
      exit 1;;
esac
shift
done

if [[ "$all" = true ]]; then
  ios=true
  bindings=true
  maui=true
fi

if [[ "$ios" = false ]] && [[ "$bindings" = false ]] && [[ "$maui" = false ]]; then
  exit 0
fi

print_yellow "\nBUILD TARGETS:\n"
print_yellow "BrowserFramework = $ios\n"
print_yellow "SwiftUIBindingMAUI = $bindings\n"
print_yellow "SwiftUIMAUILibrary = $maui\n"

#GO TO ROOT DIRECTORY

cd ..

#CLEAN

print_yellow "\n[Clean directories]\n"

if [[ "$ios" = true ]]; then
  printf "Clean XCFrameworks...\n"
  rm XCFrameworks/ApiDefinitions.cs
  rm -r XCFrameworks/*.xcarchive
  rm -r XCFrameworks/BrowserFramework.xcframework

  printf "Clean ApiDefinitions.cs file...\n"
  rm SwiftUIBindingMAUI/SwiftUIBindingMAUI/ApiDefinitions.cs
fi

if [[ "$bindings" = true ]]; then
  printf "Clean SwiftUIBindingMAUI...\n"
  rm -r SwiftUIBindingMAUI/SwiftUIBindingMAUI/bin
  rm -r SwiftUIBindingMAUI/SwiftUIBindingMAUI/obj
fi

if [[ "$maui" = true ]]; then
  printf "Clean SwiftUIMAUILibrary...\n"
  rm -r SwiftUIMAUILibrary/SwiftUIMAUILibrary/bin
  rm -r SwiftUIMAUILibrary/SwiftUIMAUILibrary/obj
fi

#CREATE XCFramework

if [[ "$ios" = true ]]
then
  cd BrowserFramework

  print_yellow "\n[Archive build]\n"

  xcodebuild archive \
   -scheme BrowserFramework \
   -archivePath ../XCFrameworks/BrowserFramework-ios.xcarchive \
   -sdk iphoneos \
   SKIP_INSTALL=NO

  xcodebuild archive \
   -scheme BrowserFramework \
   -archivePath ../XCFrameworks/BrowserFramework-sim.xcarchive \
   -sdk iphonesimulator \
   SKIP_INSTALL=NO

  print_yellow "\n[Create xcframework]\n"

  xcodebuild -create-xcframework \
   -framework ../XCFrameworks/BrowserFramework-sim.xcarchive/Products/Library/Frameworks/BrowserFramework.framework \
   -framework ../XCFrameworks/BrowserFramework-ios.xcarchive/Products/Library/Frameworks/BrowserFramework.framework \
   -output ../XCFrameworks/BrowserFramework.xcframework  
  
  cd ..

  #GENERATE ApiDefinitions  

  print_yellow "\n[Generate ApiDefinitions.cs file]\n"

  sharpie bind \
   -sdk iphoneos16.4 \
   -output XCFrameworks/ \
   -namespace BrowserFramework \
   -framework XCFrameworks/BrowserFramework.xcframework/ios-arm64/BrowserFramework.framework

  print_yellow "\n[Fix protocol/intarface names in ApiDefinitions.cs file]\n"

  python3 Scripts/protocolsToInterfaces.py

  cp XCFrameworks/ApiDefinitions.cs SwiftUIBindingMAUI/SwiftUIBindingMAUI/
else
  print_yellow "\nBuild Swift framework skipped...\n"
fi

#BUILD Bindings library

if [[ "$bindings" = true ]]
then
  cd SwiftUIBindingMAUI

  print_yellow "\n[Build bindings]\n"

  build $isRelease

  cd ..
else
  print_yellow "\nBuild bindings skipped...\n"
fi

#BUILD MAUI library

if [[ "$maui" = true ]]
then
  cd SwiftUIMAUILibrary

  print_yellow "\n[Build MAUI library]\n"

  build $isRelease

  cd ..
else
  print_yellow "\n[Build MAUI library skipped...]\n"
fi

print_green "\nSCRIPT FINISHED at $(timestamp)!\n"