//
//  DwebAppDelegate.swift
//  DwebBrowser
//
//  Created by instinct on 2023/11/10.
//  Copyright © 2023 orgName. All rights reserved.
//

import UIKit
import DwebShared

class DwebBrowserAppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        Log("Launch! home path:\(NSHomeDirectory())")
        KotlinComposeRedrawerFix.fix()
        let _ = DwebLifeStatusCenter.shared
        DwebDeskVCStore.startUpNMMs(application)
        DwebOrderFunDump.dumpOrderFileIfNeed()
        return true
    }

    func application(_ application: UIApplication, configurationForConnecting connectingSceneSession: UISceneSession, options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        let sceneConfig = UISceneConfiguration(name: nil, sessionRole: connectingSceneSession.role)
        sceneConfig.delegateClass = SceneDelegate.self
        return sceneConfig
    }
}

class SceneDelegate: NSObject, UIWindowSceneDelegate {
    
    private var toHandShortcut: UIApplicationShortcutItem? = nil

    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        handDeepLink(connectionOptions.urlContexts.first?.url)
        toHandShortcut = connectionOptions.shortcutItem
    }
    
    func sceneDidBecomeActive(_ scene: UIScene) {
        let _ = handShortcut(toHandShortcut)
        toHandShortcut = nil
    }
    
    func windowScene(_ windowScene: UIWindowScene, performActionFor shortcutItem: UIApplicationShortcutItem, completionHandler: @escaping (Bool) -> Void) {
        toHandShortcut = shortcutItem
    }

    func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
        handDeepLink(URLContexts.first?.url)
    }
    
    private func handShortcut(_ item: UIApplicationShortcutItem?) -> Bool {
        guard let item = item else { return false }
        return ShortcutTools.hand(item)
    }
    
    private func handDeepLink(_ url: URL?) {
        guard let url = url, url.scheme == "dweb" else { return }
        DwebDeepLink.shared.openDeepLink(url: url.absoluteString)
    }
}
