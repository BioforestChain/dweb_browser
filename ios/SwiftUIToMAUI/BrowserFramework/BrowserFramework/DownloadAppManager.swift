//
//  DownloadAppManager.swift
//  BrowserFramework
//
//  Created by ui03 on 2023/6/5.
//

import SwiftUI

@objc(DownloadAppManager)
public class DownloadAppManager: NSObject {
    
    @objc public var downloadView: UIView?
    
    public typealias onStartDownload = ([String:String]) -> Void
    
    private var callback: onStartDownload?
    
    @objc public init(data: [String:Any], isLoaded: Bool) {
        super.init()
        let controller = UIHostingController(rootView: DownloadAppView(modelDict: data, isLoaded: isLoaded))
        downloadView = controller.view
        
        _ = downloadPublisher.sink { value in
            self.callback?(value)
        }
    }
    
    @objc public func onListenProgress(progress: Float) {
        progressPublisher.send(progress)
    }
    
    //点击下载按钮
    @objc public func clickDownloadAction(callback: @escaping onStartDownload) {
        self.callback = callback
    }
    //下载完成
    @objc public func downloadComplete() {
        NotificationCenter.default.post(name: NSNotification.Name.downloadComplete, object: nil)
    }
    //下载失败
    @objc public func downloadFail() {
        NotificationCenter.default.post(name: NSNotification.Name.downloadFail, object: nil)
    }
}
