//
//  FirstViewController.swift
//  Plaoc-iOS
//
//  Created by ui03 on 2022/10/13.
//

import UIKit
import RxSwift
import SDWebImage
import Combine
import Alamofire
import Vapor

class FirstViewController: UIViewController {

    private var appNames: [String] = []
    private var buttons: [UIButton] = []
    private var labels: [UILabel] = []
    private let disposeBag = DisposeBag()
    private var jsCore: JSCoreManager!
    
    private var writeDataScope = DispatchQueue.init(label: "write")
    private var readDataScope = DispatchQueue.init(label: "read")
    
    let dataSizeChangeChannel = PassthroughSubject<Int, Never>()
    let passThroughSubject = PassthroughSubject<String, Error>()
    var subscription: AnyCancellable?
    var app: Application!
    
    override func viewDidLoad() {
        super.viewDidLoad()

        self.view.backgroundColor = .white
        
        appNames =  Array( sharedAppInfoMgr.appIdList )
        
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
                let type = sharedAppInfoMgr.currentAppType(appId: name)
                if type == .user {
                    let urlString = sharedAppInfoMgr.scanImageURL(appId: name)
                    button.sd_setImage(with: URL(string: urlString), for: .normal)
                } else {
                    button.setImage(sharedAppInfoMgr.currentAppImage(appId: name), for: .normal)
                }
                button.tag = i
                button.layer.cornerRadius = 10
                button.layer.masksToBounds = true
                self.view.addSubview(button)
                buttons.append(button)
                
                let label = UILabel(frame: CGRect(x: button.frame.minX, y: 280, width: 60, height: 20))
                label.textAlignment = .center
                label.textColor = .black
                label.text = sharedAppInfoMgr.currentAppName(appId: name)
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
        
        
        
        DispatchQueue.global().async {
//            self.test()
        }
       
    }
    
    func test() {
        
        
        var env = try! Environment.detect()

        app = Application(.development)
        defer { app.shutdown() }

        try! configure(app)

        try! app.run()
    }
    
    public func configure(_ app: Application) throws {
        app.logger.logLevel = .debug
        
        app.http.server.configuration.hostname = "127.0.0.1"
        app.http.server.configuration.port = 8080
        
        // routes
//        app.on(.GET, "ping") { req in
//                return "123456"
//        }
        
        app.routes.get("hello", ":a") { req in
            return req.parameters.get("a") ?? ""
        }
    }
    
    @objc func update(noti: Notification) {
        guard let infoDict = noti.userInfo else { return }
        guard let type = infoDict["progress"] as? String else { return }
        let appId = infoDict["appId"] as? String
        DispatchQueue.main.async {
            if type == "complete" {
                if appId != nil {
                    sharedAppInfoMgr.updateFileType(appId: appId!)
                    if let index = self.appNames.firstIndex(of: appId!) {
                        let button = self.buttons[index]
                        button.setImage(sharedAppInfoMgr.currentAppImage(appId: appId!), for: .normal)
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
    @objc func tap(sender: UIButton) throws {
        
//        dataSizeChangeChannel.send(2)
        
        
        
//        let semaphore = DispatchSemaphore(value: 0)
        
//        print("1")
//        test1(semaphore: semaphore)
//        test2(sem: semaphore)
//        semaphore.wait()
////        print("2")
//
//        semaphore.wait()
        
        return
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
        let type = sharedAppInfoMgr.currentAppType(appId: name)
        if type == .system {
            let second = WebViewViewController()
            second.appId = name
            second.urlString = sharedAppInfoMgr.systemWebAPPURLString(appId: name)! //"iosqmkkx:/index.html"
            let type = sharedAppInfoMgr.systemAPPType(appId: name)
            let url = sharedAppInfoMgr.systemWebAPPURLString(appId: name) ?? ""
            if type == "web" {
                second.urlString = url
            } else {
                second.urlString = "iosqmkkx:/index.html"
            }
            self.navigationController?.pushViewController(second, animated: true)
        } else if type == .recommend {
        //    sharedAppInfoMgr.clickRecommendAppAction(appId: name)
        } else if type == .user {
        //    sharedAppInfoMgr.clickRecommendAppAction(appId: name)
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
        let scanURLString = sharedAppInfoMgr.scanDownloadURLString(appId: name)
        guard scanURLString.count > 0 else { return }
        sharedNetworkMgr.downloadApp(appId: name, urlString: scanURLString)
        let button = self.view.viewWithTag(3) as? UIButton
        button!.setupForAppleReveal()
    }
    
    func test1(semaphore: DispatchSemaphore) {
        
        
        writeDataScope.async {
//            semaphore.wait()//当信号量为0时，阻塞在此
            Thread.sleep(forTimeInterval: 2)
            print("test1")
            semaphore.signal()//信号量加1
        }
        

        
        
    }
    
    func test2(sem: DispatchSemaphore) {
        
        
        readDataScope.async {
//            sem.wait()//当信号量为0时，阻塞在此
            Thread.sleep(forTimeInterval: 1)
            print("test2")
            sem.signal()//信号量加1
        }
    }
    
    func test() async {
        
        let add = { (min: Int,max: Int) -> Int in
            var sum = 0
            for i in min..<max {
                sum += i
            }
            return sum
        }
        
        let seg = 10
        
        let n = Int(arc4random_uniform(10))
        
        let result = await withTaskGroup(of: Int.self, body: { group -> Int in
            
            for i in 1...(n / seg) {
                group.addTask {
                    add(seg * (i - 1), seg * i)
                }
            }
            
            if n % seg > 0 {
                group.addTask {
                    add(n - n % seg, n + 1)
                }
            }
            
            var totalSum = 0
            for await result in group {
                totalSum += result
            }
            return totalSum
        })
        print(result)
    }
    

}


