//
//  ShareManager.swift
//  BFExplorer
//
//  Created by ui03 on 2022/12/26.
//

import UIKit

class ShareManager: NSObject {

    static func loadSystemShare(title: String? = nil, text: String? = nil, url: String? = nil, image: UIImage? = nil, files: [String]? = nil) {
        
        var urlPath: URL?
        if url != nil {
            urlPath = URL(string: url!)
        }
        var items: [Any] = []
        if title != nil {
            items.append(title!)
        }
        if text != nil {
            items.append(text!)
        }
        if urlPath != nil {
            items.append(urlPath!)
        }
        if image != nil {
            items.append(image!)
        }
        if files != nil {
            items.append(files!)
        }
        guard items.count > 0 else { return }
        guard let app = UIApplication.shared.delegate as? AppDelegate else { return }
        let activityVC = UIActivityViewController(activityItems: items, applicationActivities: nil)
        activityVC.excludedActivityTypes = []
        app.window?.rootViewController?.present(activityVC, animated: true)
        activityVC.completionWithItemsHandler = { (type, completed, items, error) in
            if completed {
                print("分享完成")
            } else {
                print("分享失败")
            }
        }
        
    }
}
