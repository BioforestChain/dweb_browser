//
//  FirstViewController.swift
//  Plaoc-iOS
//
//  Created by ui03 on 2022/10/13.
//

import UIKit
import RxSwift
import SDWebImage

class FirstViewController: UIViewController {

    private var appNames: [String] = []
    private var buttons: [UIButton] = []
    private var labels: [UILabel] = []
    private let disposeBag = DisposeBag()
    private var jsCore: JSCoreManager!
    
    override func viewDidLoad() {
        super.viewDidLoad()

        self.view.backgroundColor = .white
        
        appNames =  Array( sharedInnerAppFileMgr.appIdList )
        
        for i in stride(from: 0, to: appNames.count + 1, by: 1) {
            if i == appNames.count {
                let button = UIButton(frame: CGRect(x: 30 + i * 90, y: 200, width: 60, height: 60))
                button.addTarget(self, action: #selector(tap(sender:)), for: .touchUpInside)
                button.setTitle("测试", for: .normal)
                button.setTitleColor(.black, for: .normal)
                button.tag = i
                self.view.addSubview(button)
            } else {
                let name = appNames[i]
                let button = UIButton(frame: CGRect(x: 30 + i * 90, y: 200, width: 60, height: 60))
                button.addTarget(self, action: #selector(tap(sender:)), for: .touchUpInside)
                let type = sharedInnerAppFileMgr.currentAppType(appId: name)
                if type == .user {
                    let urlString = sharedInnerAppFileMgr.scanImageURL(appId: name)
                    button.sd_setImage(with: URL(string: urlString), for: .normal)
                } else {
                    button.setImage(sharedInnerAppFileMgr.currentAppImage(appId: name), for: .normal)
                }
                button.tag = i
                button.layer.cornerRadius = 10
                button.layer.masksToBounds = true
                self.view.addSubview(button)
                buttons.append(button)
                
                let label = UILabel(frame: CGRect(x: button.frame.minX, y: 280, width: 60, height: 20))
                label.textAlignment = .center
                label.textColor = .black
                label.text = sharedInnerAppFileMgr.currentAppName(appId: name)
                self.view.addSubview(label)
                labels.append(label)
            }
        }
        
        NotificationCenter.default.addObserver(self, selector: #selector(update(noti:)), name: NSNotification.Name.progressNotification, object: nil)
        
        operateMonitor.startAnimationMonitor.subscribe(onNext: { [weak self] appId in
            guard let strongSelf = self else { return }
            if let index = strongSelf.appNames.firstIndex(of: appId) {
                let button = strongSelf.buttons[index]
                DispatchQueue.main.async {
                    button.setupForAppleReveal()
                }
            }
        }).disposed(by: disposeBag)
    }
    
    @objc func update(noti: Notification) {
        guard let infoDict = noti.userInfo else { return }
        guard let type = infoDict["progress"] as? String else { return }
        let appId = infoDict["appId"] as? String
        DispatchQueue.main.async {
            if type == "complete" {
                if appId != nil {
                    sharedInnerAppFileMgr.updateFileType(appId: appId!)
                    if let index = self.appNames.firstIndex(of: appId!) {
                        let button = self.buttons[index]
                        button.setImage(sharedInnerAppFileMgr.currentAppImage(appId: appId!), for: .normal)
                        button.startExpandAnimation()
                    }
                }
            }
        }
        
        if Double(type) != nil {
            
            var count = Double(type)!
            if count >= 0.98 {
                count = 0.98
            }
            if let index = self.appNames.firstIndex(of: appId!) {
                let button = self.buttons[index]
                button.startProgressAnimation(progress: 1.0 - count)
            }
        }
        
    }
    var manager: BrowserManager!
    @objc func tap(sender: UIButton) {
      
        
        if sender.tag == 2 {
            var config = SplashScreenConfig()
            config.backgroundColor = .red
            config.spinnerStyle = .large
            config.spinnerColor = .blue
            
            let setting = SplashScreenSettings()
            let splashScreen = SplashScreenManager(parentView: self.view, config: config)
            splashScreen.show(settings: setting) {
                
            }
//            let second = WebViewViewController()
//            second.appId = "wallet"
//            second.urlString = "https://objectjson.waterbang.top"  //"https://wallet.plaoc.com/"
//            self.navigationController?.pushViewController(second, animated: true)
            return
        }
        let name = appNames[sender.tag]
        let type = sharedInnerAppFileMgr.currentAppType(appId: name)
        if type == .system {
            let second = WebViewViewController()
            second.appId = name
            second.urlString = sharedInnerAppFileMgr.systemWebAPPURLString(appId: name)! //"iosqmkkx:/index.html"
            let type = sharedInnerAppFileMgr.systemAPPType(appId: name)
            let url = sharedInnerAppFileMgr.systemWebAPPURLString(appId: name) ?? ""
            if type == "web" {
                second.urlString = url
            } else {
                second.urlString = "iosqmkkx:/index.html"
            }
            self.navigationController?.pushViewController(second, animated: true)
        } else if type == .recommend {
            sharedInnerAppFileMgr.clickRecommendAppAction(appId: name)
        } else if type == .user {
            sharedInnerAppFileMgr.clickRecommendAppAction(appId: name)
        }
    }
    
    func addScanAppUI(name: String) {
        let button = UIButton(frame: CGRect(x: 30 + 3 * 90, y: 200, width: 60, height: 60))
//                button.addTarget(self, action: #selector(tap(sender:)), for: .touchUpInside)
        button.setTitle(name, for: .normal)
        button.setTitleColor(.black, for: .normal)
        button.tag = 3
        button.layer.cornerRadius = 10
        button.layer.masksToBounds = true
        view.addSubview(button)
        buttons.append(button)
    }
    
    func addScanAppAction(name: String) {
        let scanURLString = sharedInnerAppFileMgr.scanDownloadURLString(appId: name)
        guard scanURLString.count > 0 else { return }
        BFSNetworkManager.shared.downloadApp(appId: name, urlString: scanURLString)
        let button = self.view.viewWithTag(3) as? UIButton
        button!.setupForAppleReveal()
    }

}


