//
//  ScanQRCodeVC.swift
//  BFExplorer
//
//  Created by ui06 on 1/16/23.
//

import UIKit
class ScanQRCodeVC: UIViewController {
    var scanResultHandler: ThirdCallback?
    
    
    
    func setScanParam(){
        //设置扫码区域参数
//        var style = LBXScanViewStyle()
//
//        style.centerUpOffset = 60
//        style.xScanRetangleOffset = 30
//
//        if UIScreen.main.bounds.size.height <= 480 {
//            //3.5inch 显示的扫码缩小
//            style.centerUpOffset = 40
//            style.xScanRetangleOffset = 20
//        }
//
//        style.color_NotRecoginitonArea = UIColor(red: 0.4, green: 0.4, blue: 0.4, alpha: 0.4)
//
//        style.photoframeAngleStyle = LBXScanViewPhotoframeAngleStyle.Inner
//        style.photoframeLineW = 2.0
//        style.photoframeAngleW = 16
//        style.photoframeAngleH = 16
//
//        style.isNeedShowRetangle = false
//
//        style.anmiationStyle = LBXScanViewAnimationStyle.NetGrid
//        style.animationImage = UIImage(named: "qrcode_scan_full_net")
//
//        let vc = LBXScanViewController()
//
//        vc.scanStyle = style
//        vc.isSupportContinuous = true;
//        
//        vc.scanResultDelegate = self
//
//        self.delegate!.present(vc, animated: true, completion: nil)
    }
}
