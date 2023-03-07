//
//  BrowserNMM.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/28.
//

import Foundation
import WebKit
import UIKit

class BrowserNMM: NativeMicroModule {
    override init() {
        super.init()
        mmid = "browser.sys.dweb"
    }
    
//    static var activityPo: PromiseOut<>
    
    override func _bootstrap() async throws {
        await MainActor.run {
            guard let app = UIApplication.shared.delegate as? AppDelegate else { return }
            
            app.window = UIWindow(frame: UIScreen.main.bounds)
            app.window?.makeKeyAndVisible()
            app.window?.rootViewController = UINavigationController(rootViewController: BrowserContainerViewController())
        }
    }
}
