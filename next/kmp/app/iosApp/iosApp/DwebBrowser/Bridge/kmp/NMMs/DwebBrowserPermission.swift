//
//  DwebBrowserPermission.swift
//  iosApp
//
//  Created by ios on 2024/1/8.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import DwebShared

final class BrowserSystemPermission {
    
    func requestSystemPermission(permissionName: SysSystemPermissionName, callback: @escaping ((Bool,String)) -> ()) {
        
        browserService.requestSystemPermission(permissionName: permissionName) { result, error in
            callback((result?.boolValue ?? true, error?.localizedDescription ?? ""))
        }
    }
}



