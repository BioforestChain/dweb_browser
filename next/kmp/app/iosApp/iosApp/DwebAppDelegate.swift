//
//  DwebAppDelegate.swift
//  iosApp
//
//  Created by instinct on 2023/11/10.
//  Copyright © 2023 orgName. All rights reserved.
//

import UIKit

class DwebAppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        Log("Launch! home path:\(NSHomeDirectory())")
        KotlinComposeRedrawerFix.fix()
        DwebDeskVCStore.shared.startUpNMMs(application)
        return true
    }

    func application(_ application: UIApplication, configurationForConnecting connectingSceneSession: UISceneSession, options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        let sceneConfig = UISceneConfiguration(name: nil, sessionRole: connectingSceneSession.role)
        sceneConfig.delegateClass = SceneDelegate.self
        return sceneConfig
    }
}

class SceneDelegate: NSObject, UIWindowSceneDelegate {
    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        if let urlContext = connectionOptions.urlContexts.first {
            let url = urlContext.url

            if url.scheme == "dweb" {
                DwebDeepLink.shared.openDeepLink(url: url.absoluteString)
            }
        }
    }

    func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
        guard let url = URLContexts.first?.url else { return }

        if url.scheme == "dweb" {
            DwebDeepLink.shared.openDeepLink(url: url.absoluteString)
        }
    }
}
