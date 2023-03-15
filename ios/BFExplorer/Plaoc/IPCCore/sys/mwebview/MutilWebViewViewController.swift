//
//  MutilWebViewViewController.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/8.
//

import UIKit

class MutilWebViewViewController: UIViewController {

    private var webViewList: [ViewItem] = []
    private var remoteMmid: String = ""
    private var webviewId_acc = 1
    
    override func viewDidLoad() {
        super.viewDidLoad()

        
    }
    
    func openWebView(module: MicroModule, urlString: String) -> ViewItem {
        
        let tmp = webviewId_acc
        webviewId_acc += 1
        let webviewId = "#w\(tmp)"
        
        let dWebView = DWebView(frame: self.view.bounds, mm: module, options: Options(urlString: urlString))
        //TODO
        
        let item = ViewItem(webviewId: webviewId, dWebView: dWebView)
        webViewList.append(item)
        
        return item
    }
   
    //关闭WebView
    func closeWebView(webviewId: String) -> Bool {
        
        for item in webViewList {
            if item.webviewId == webviewId {
                item.dWebView.removeFromSuperview()
                return true
            }
        }
        return false
    }
    
    //将指定WebView移动到顶部显示
    func moveToTopWebView(webviewId: String) -> Bool {
        
        guard let index = webViewList.firstIndex(where: { $0.webviewId == webviewId }) else { return false }
        let item = webViewList.remove(at: index)
        webViewList.append(item)
        return true
    }

}
