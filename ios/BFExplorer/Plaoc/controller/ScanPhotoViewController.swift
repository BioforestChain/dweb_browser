//
//  ScanPhotoViewController.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/7/22.
//

import UIKit
import AVFoundation
import RxSwift
import Photos

class ScanPhotoViewController: UIViewController {

    private var scanRectView: UIView?
    private var device: AVCaptureDevice?
    private var input: AVCaptureDeviceInput?
    private var output: AVCaptureMetadataOutput?
    private var session: AVCaptureSession?
    private var preview: AVCaptureVideoPreviewLayer?
    var callback: ThirdCallback?
    private let disposeBag = DisposeBag()
    
    var animationIV = ScanNetAnimationImageView.instance()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.view.backgroundColor = .black
        self.view.addSubview(naviView)
        
        operateMonitor.scanMonitor.subscribe(onNext: { [weak self] (appId,dict) in
            guard let strongSelf = self else { return }
            DispatchQueue.main.async {
                strongSelf.alertUpdateViewController(appId: appId, dataDict: dict)
            }
        }).disposed(by: disposeBag)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        startScan()
        
        let image = UIImage(named: "qrcode_scan_part_net")
        animationIV.startAnimatingWithRect(animationRect: self.scanRectView!.frame, parentView: self.view, image: image)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        self.session?.stopRunning()
        
    }
    
    lazy private var naviView: NaviView = {
        let naviView = NaviView(frame: CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: UIDevice.current.statusBarHeight() + 44))
        return naviView
    }()
    
    //开始扫描
    func startScan() {
        
        do {
            self.device = AVCaptureDevice.default(for: .video)
            if self.device != nil {
                self.input = try? AVCaptureDeviceInput(device: self.device!)
            }
            
            self.output = AVCaptureMetadataOutput()
            self.output?.setMetadataObjectsDelegate(self, queue: .main)
            
            
            self.session = AVCaptureSession()
            guard self.input != nil, self.session!.canAddInput(self.input!) else { return }
            guard self.output != nil, self.session!.canAddOutput(output!) else { return }
            
            if UIScreen.main.bounds.height < 500 {
                self.session?.sessionPreset = AVCaptureSession.Preset.vga640x480
            } else {
                self.session?.sessionPreset = AVCaptureSession.Preset.high
            }
            
            session?.addInput(input!)
            session?.addOutput(output!)
            // 注意点: 设置数据类型一定要在输出对象添加到会话之后才能设置
            self.output?.metadataObjectTypes = [.qr, .ean13, .ean8,.code93, .code128, .code39, .code93, .code39Mod43]
            
            let scanSize = CGSize(width: UIScreen.main.bounds.size.width * 0.75, height: UIScreen.main.bounds.size.width * 0.75)
            var scanRect = CGRect(x: (UIScreen.main.bounds.size.width - scanSize
                .width) * 0.5, y: (UIScreen.main.bounds.size.height - scanSize
                    .width) * 0.5, width: scanSize.width, height: scanSize.height)
            
            //计算rectOfInterest 注意x,y交换位置
            scanRect = CGRect(x:scanRect.origin.y / UIScreen.main.bounds.size.height,
                              y:scanRect.origin.x / UIScreen.main.bounds.size.width,
                              width:scanRect.size.height / UIScreen.main.bounds.size.height,
                              height:scanRect.size.width / UIScreen.main.bounds.size.width)
            self.output?.rectOfInterest = scanRect
            
            self.preview = AVCaptureVideoPreviewLayer(session: self.session!)
            self.preview?.videoGravity = AVLayerVideoGravity.resizeAspectFill
            self.preview?.frame = UIScreen.main.bounds
            self.view.layer.insertSublayer(self.preview!, at: 0)
            
            self.scanRectView = UIView(frame: CGRect(x: 0, y: 0, width: scanSize.width, height: scanSize.height))
            self.scanRectView?.center = CGPoint(x: UIScreen.main.bounds.midX, y: UIScreen.main.bounds.midY)
//            self.scanRectView?.layer.borderColor = UIColor.green.cgColor
//            self.scanRectView?.layer.borderWidth = 1
            self.view.addSubview(self.scanRectView!)
            DispatchQueue.global().async {
                self.session?.startRunning()

            }
            
            //通过代码拉近镜头焦距，放大内容区域让机器更好的识别
            do {
                try self.device?.lockForConfiguration()
            } catch _ {
                print("error: lockForConfiguration")
            }
            self.device?.videoZoomFactor = 1.5
            self.device?.unlockForConfiguration()
        } catch _ {
            //打印错误消息
            let alertController = UIAlertController(title: "温馨提醒",
                                                    message: "请在iPhone的\"设置-隐私-相机\"选项中,允许本程序访问您的相机",
                                                    preferredStyle: .alert)
            let cancelAction = UIAlertAction(title: "确定", style: .cancel, handler: nil)
            alertController.addAction(cancelAction)
            self.present(alertController, animated: true, completion: nil)
        }
    }
    
    private func alertUpdateViewController(appId: String, dataDict: [String:Any]) {
        let alertVC = UIAlertController(title: "确认下载更新吗？", message: nil, preferredStyle: .alert)
        let sureAction = UIAlertAction(title: "确认", style: .default) { action in
     
            sharedInnerAppFileMgr.scanToDownloadApp(appId: appId, dict: dataDict)
            
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                self.navigationController?.popViewController(animated: true)
                self.callback?(appId)
            }
        }
        let cancelAction = UIAlertAction(title: "取消", style: .cancel) { action in
            self.navigationController?.popViewController(animated: true)
        }
        alertVC.addAction(sureAction)
        alertVC.addAction(cancelAction)
        self.present(alertVC, animated: true)
    }
    //停止扫描
    private func stopScan() {
        self.session?.stopRunning()
        self.animationIV.stopStepAnimating()
    }
    //暂停扫描
    private func pauseScanning() {
        print("pause scanning")
    }
    //恢复扫描
    private func resumeScanning() {
        print("resume scanning")
    }
    //检查是否有摄像头权限，如果没有或者被拒绝，那么会强制请求打开权限（设置）
    private func checkCameraPermission() -> Bool {
        let authStatus = PHPhotoLibrary.authorizationStatus()
        switch authStatus {
        case .authorized:
            return true
        default:
            return false
        }
    }
    //跳转到设置界面
    private func openAppSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else {
            return
        }
        if UIApplication.shared.canOpenURL(url) {
            UIApplication.shared.open(url, options: [:], completionHandler: nil)
        }
    }
    //打开关闭手电筒
    private func toggleTorch() {
        guard let device = AVCaptureDevice.default(for: AVMediaType.video) else { return }
        if device.hasTorch && device.isTorchAvailable {
            try? device.lockForConfiguration()
            device.torchMode = device.torchMode == .off ? .on : .off
            device.unlockForConfiguration()
        }
    }
    //手电筒状态
    private func getTorchState() -> Bool {
        guard let device = AVCaptureDevice.default(for: AVMediaType.video) else { return false }
        return device.torchMode == .off ? false : true
    }
    //隐藏webview背景
    private func hideBackground() {
        print("hide background")
    }
    //显示webview背景
    private func showBackground() {
        print("show background")
    }
    
}

extension ScanPhotoViewController: AVCaptureMetadataOutputObjectsDelegate {
    
    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        if let metadataObj = metadataObjects.first as? AVMetadataMachineReadableCodeObject {
            let result = metadataObj.stringValue  //扫描结果
            if result != nil {
//                let refreshManager = RefreshManager()
//                refreshManager.loadUpdateRequestInfo(appId: nil, urlString: result, isCompare: false)
            }
        }
        self.stopScan()
    }
}
