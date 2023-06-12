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
    
    public typealias onStartDownload = (String) -> Void
    
    public typealias backCallback = () -> Void
    
    private var callback: onStartDownload?
    private var backCallback: backCallback?
    
    @objc public init(data: Data, downloadStatus: Int) {
        super.init()
        let controller = UIHostingController(rootView: DownloadAppView(modelData: data, downloadStatus: DownloadStatus(rawValue: downloadStatus) ?? DownloadStatus.IDLE))
        self.downloadView = controller.view
        
        NotificationCenter.default.addObserver(forName: NSNotification.Name.downloadApp, object: nil, queue: .main) { noti in
            self.callback?(noti.userInfo?["type"] as? String ?? "")
        }
        
        NotificationCenter.default.addObserver(forName: NSNotification.Name.backToLastView, object: nil, queue: .main) { _ in
            self.backCallback?()
        }
    }
    
    @objc public func onListenProgress(progress: Float) {
        DispatchQueue.main.async {
            progressPublisher.send(progress)
        }
    }
    
    // 点击下载按钮
    @objc public func clickDownloadAction(callback: @escaping onStartDownload) {
        self.callback = callback
    }
    
    // 下载状态变更
    @objc public func onDownloadChange(downloadStatus: Int) {
        switch DownloadStatus(rawValue: downloadStatus) {
        case .Installed:
            NotificationCenter.default.post(name: NSNotification.Name.downloadComplete, object: nil)
        case .Fail:
            NotificationCenter.default.post(name: NSNotification.Name.downloadFail, object: nil)
        default:
            break
        }
    }
    
    @objc public func onBackAction(callback: @escaping backCallback) {
        self.backCallback = callback
    }
}
