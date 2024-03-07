#!/bin/sh

#  UTBuildConfig.sh
#  DwebBrowser
#
#  Created by instinct on 2024/3/7.
#  Copyright © 2024 orgName. All rights reserved.

echo "UTBuildConfig.sh"

function testingBuild() {
    # mode:
    # 0: Unit Testing
    # 1: UI Testing
    mode="DwebTesting"
    if [ "$1" == "ui" ]; then
        mode="DwebUITesting"
            echo " UI Testing "
        else
            echo "Unit Testing"
    fi


    root_dict=`pwd`
    root_dict="$root_dict/../../.."

    echo "root_dict: $root_dict"
    echo "SRCROOT: $SRCROOT"

    source "$root_dict/DwebBrowser/DwebBrowser/Config/OrderBuildConfig.sh"

    echo "update info.plist start"

    update_dweb_mode "$root_dict/DwebBrowser/DwebBrowser/Info.plist" $mode
        
    echo "update info.plist end"
}

function testingRecover() {
    root_dict=`pwd`
    root_dict="$root_dict/../../.."

    echo "root_dict: $root_dict"
    echo "SRCROOT: $SRCROOT"

    source "$root_dict/DwebBrowser/DwebBrowser/Config/OrderBuildConfig.sh"
    
    echo "recover info.plist start"

    recover_dweb_mode "$root_dict/DwebBrowser/DwebBrowser/Info.plist"

    echo "recover info.plist end"
}

