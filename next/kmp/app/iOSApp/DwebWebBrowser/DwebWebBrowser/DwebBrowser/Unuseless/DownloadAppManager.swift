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
    
    private var callback: onStringCallBack?
    
    @objc public init(data: Data) {
        super.init()
        
        let controller = UIHostingController(rootView: DownloadAppView(modelData: data) { value in
            self.callback?(value)
        })
        self.downloadView = controller.view
    }
    
    @objc public func onListenProgress(progress: Double) {
        DispatchQueue.main.async {
            progressPublisher.send(progress)
        }
    }
    
    // 点击按钮
    @objc public func clickButtonAction(callback: @escaping onStringCallBack) {
        self.callback = callback
    }
    
    // 下载状态变更
    @objc public func onDownloadChange(downloadStatus: Int) {
        DispatchQueue.main.async {
            downloadPublisher.send(downloadStatus)
        }
    }
}
