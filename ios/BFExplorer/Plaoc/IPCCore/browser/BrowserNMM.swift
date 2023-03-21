//
//  BrowserNMM.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/2.
//

import Foundation
import UIKit

class BrowserNMM: NativeMicroModule {
    
    init() {
        super.init(mmid: "browser.sys.dweb")
    }
    
    override func _bootstrap(bootstrapContext: BootstrapContext) throws {
      
        DispatchQueue.main.async {
            guard let app = UIApplication.shared.delegate as? AppDelegate else { return }
            
            app.window = UIWindow(frame: UIScreen.main.bounds)
            app.window?.makeKeyAndVisible()
            app.window?.rootViewController = UINavigationController(rootViewController: BrowserContainerViewController())
        }
        
    }
    
    override func _shutdown() throws {
        
    }
}
