//
//  AppDelegate.swift
//  Browser
//
//         
//

import UIKit
import Network
import Vapor
import SwiftUI
import Moya
import AsyncHTTPClient
import PromiseKit
import SwiftyJSON
import HandyJSON
import Alamofire

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?
    
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        window = UIWindow(frame: UIScreen.main.bounds)
        window?.makeKeyAndVisible()

        sharedCachesMgr.cacheNews()
        appVersionMgr.startCheck()
        window?.rootViewController = UINavigationController(rootViewController: FirstViewController())
        
//        startDwebBrowser()
        
        return true
    }
    
    func sendCodeContinuation() async -> Data {
        
        await withCheckedContinuation({ continuation in
            let request = URLRequest(url: URL(string: "https://www.jianshu.com/p/69f06a0d757b")!)
            let tash = URLSession.shared.dataTask(with: request) { data, response, error in
                continuation.resume(returning: data!)
            }
            tash.resume()
        })
        
    }
    
    
//    func testAction(comment: IPC_MESSAGE_TYPE2) {
//        var type: IPC_MESSAGE_TYPE2 = .REQUEST1
//        
//        if type == .REQUEST1 {
//            
//        }
//    }

}

extension UIApplication {
var statusBarUIView: UIView? {

    if #available(iOS 13.0, *) {
        let tag = 3848245

        let keyWindow = UIApplication.shared.connectedScenes
            .map({$0 as? UIWindowScene})
            .compactMap({$0})
            .first?.windows.first

        if let statusBar = keyWindow?.viewWithTag(tag) {
            return statusBar
        } else {
            let height = keyWindow?.windowScene?.statusBarManager?.statusBarFrame ?? .zero
            let statusBarView = UIView(frame: height)
            statusBarView.tag = tag
            statusBarView.layer.zPosition = 999999
            statusBarView.backgroundColor = .red
            keyWindow?.addSubview(statusBarView)
            return statusBarView
        }

    } else {

        if responds(to: Selector(("statusBar"))) {
            return value(forKey: "statusBar") as? UIView
        }
    }
    return nil
  }
}


