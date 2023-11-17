//
//  DwebAppDelegate.swift
//  iosApp
//
//  Created by instinct on 2023/11/10.
//  Copyright © 2023 orgName. All rights reserved.
//

import UIKit

class DwebAppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        Log("Launch! home path:\(NSHomeDirectory())")
        KmpBridgeManager.shared.registerIMPs()
        DwebDeskVCStroe.shared.startUpNMMs(application)
        return true
    }
}
