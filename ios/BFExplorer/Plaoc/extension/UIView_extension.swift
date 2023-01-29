//
//  UIView_extension.swift
//  DWebBrowser
//
//  Created by mac on 2022/4/21.
//

import UIKit
import AVFoundation

extension UIView {
    
    //获取某个view所在的控制器
    public func currentViewController() -> UIViewController {
        var viewController: UIViewController? = nil
        var next = self.next
        while (next != nil) {
            if next!.isKind(of: UIViewController.self) {
                viewController = (next as! UIViewController)
                break
            }
            next = next!.next
        }
        return viewController!
    }
    
    //打开关闭手电筒
    public func openTorch() {
        guard let device = AVCaptureDevice.default(for: AVMediaType.video) else { return }
        if device.hasTorch && device.isTorchAvailable {
            try? device.lockForConfiguration()
            device.torchMode = device.torchMode == .off ? .on : .off
            device.unlockForConfiguration()
        }
    }
}
