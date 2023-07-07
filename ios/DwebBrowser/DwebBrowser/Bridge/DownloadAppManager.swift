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
        let downloadAppObserve = NotificationCenter.default.addObserver(forName: Notification.Name.downloadApp, object: nil, queue: .main) { noti in
            let type = noti.userInfo?["type"] as? String ?? ""
            self.callback?(type)
        }
        let backObserve = NotificationCenter.default.addObserver(forName: Notification.Name.backToLastView, object: nil, queue: .main) { _ in
            self.backCallback?()
        }
        let observes = [downloadAppObserve, backObserve]
        
        let controller = UIHostingController(rootView: DownloadAppView(modelData: data, downloadStatus: DownloadStatus(rawValue: downloadStatus) ?? DownloadStatus.IDLE, Observes: observes))
        self.downloadView = controller.view
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
            NotificationCenter.default.post(name: Notification.Name.downloadComplete, object: nil)
        case .Fail:
            NotificationCenter.default.post(name: Notification.Name.downloadFail, object: nil)
        default:
            break
        }
    }
    
    @objc public func onBackAction(callback: @escaping backCallback) {
        self.backCallback = callback
    }
}
