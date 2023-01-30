//
//  InnerAppFileManager.swift
//  BFS
//
//  Created by ui03 on 2022/9/6.
//

import UIKit
import RxSwift

enum InnerAppType: Int {
    case recommend //推荐安装，还没装
    case system //推荐安装，已经安装
    case user   //第三方已安装的
}

let sharedRecommendAppManager = RecommendAppManager()
let sharedInnerAppFileMgr = InnerAppFileManager()

class InnerAppFileManager: NSObject {

    private let disposeBag = DisposeBag()
    private let sysManager = SystemAppManager()
    private let scanManager = UserAppManager()
    private var appNames: [String:String] = [:]
    private var appImages: [String:UIImage?] = [:]
    private var fileLinkDict: [String:String] = [:]
    private var redDict: [String: Bool] = [:]
    
//    private var appNamesContainer = [[String]]()

    private var appType: [String : InnerAppType] = [:]

    
    private(set) var appIdList = Set<String>()
    
    let redSoptCachePath = documentdir + "/redHot"

//    static let shared = InnerAppFileManager()
    
    override init() {
        super.init()
        initBatchFile()
    }
    
    private func initBatchFile() {
        
        totalRedHotContent()
        

        
        loadAppType()
        fetchAppNames()
        fetchAppIcons()
        
        GlobalTimer.shared.StartTimer()
        
        operateMonitor.refreshCompleteMonitor.subscribe(onNext: { [weak self] appId in
            guard let strongSelf = self else { return }
            strongSelf.downloadNewFile(appId: appId)
        }).disposed(by: disposeBag)
    }
    
    //点击recommend文件
    func clickRecommendAppAction(appId: String) {
        //1、从bfs-app-id/tmp/autoUpdate/缓存中读取当下的新json数据,并请求更新
        guard appId.count > 0 else { return }
        let currentURLString = readCachedAppConfig(appId: appId)
        alertUpdateViewController(appId: appId, urlstring: currentURLString)
        fileLinkDict[appId] = currentURLString
    }
    //写入轮询更新数据
    func writeUpdateContent(appId: String, json: [String:Any]?) {
        guard json != nil else { return }
        let type = currentAppType(appId: appId)
        if type == .system {
            sysManager.writeUpdateInfoToTmpFile(appId: appId, json: json!)
        } else if type == .recommend {
            sharedRecommendAppManager.writeUpdateInfoToTmpFile(appId: appId, json: json!)
        } else if type == .user {
            scanManager.writeUpdateInfoToTmpFile(appId: appId, json: json!)
        }
    }
    
    //更新文件状态为已下载
    func updateFileType(appId: String) {
        updateLocalSystemAPPData(appId: appId)
    }
    //更新文件状态为扫码
    func updateUserType(appId: String) {
        appType[appId] = .user
        appNames[appId] = scanManager.appName(appId: appId)
    }
    //获取扫码的图片地址
    func scanImageURL(appId: String) -> String {
        return scanManager.appIconUrlString(appId: appId) ?? ""
    }
    //获取扫码后app的下载地址
    func scanDownloadURLString(appId: String) -> String {
        return readCachedAppConfig(appId: appId) ?? ""
    }
    //扫码下载app
    func scanToDownloadApp(appId: String, dict: [String:Any]) {
        self.addAPPFromScan(appId: appId, dict: dict)
        self.updateUserType(appId: appId)
        RefreshManager.saveLastUpdateTime(appId: appId, time: Date().timeStamp)
        self.writeUpdateContent(appId: appId, json: dict)
    }

    //定时刷新
    func fetchRegularUpdateTime() {
        
        guard appIdList.count > 0 else { return }
        let updateArray = appIdList//.filter{ currentAppType(appId: $0) == .recommend }
//        guard updateArray.count > 0 else { return }
        let refreshManager = RefreshManager()
        for appId in updateArray {
            if isNeedUpdate(appId: appId) {
                let updateString = autoUpdateURLString(appId: appId)
                refreshManager.loadUpdateRequestInfo(appId: appId, urlString: updateString, isCompare: false)
            }
        }
    }
    //判断是否过了缓存时间
    func isNeedUpdate(appId: String) -> Bool {
        
        guard let lastTime = RefreshManager.fetchLastUpdateTime(appId: appId) else { return false }
        guard let maxAge = autoUpdateMaxAge(appId: appId) else { return false }
        let currentDate = Date().timeStamp
        if currentDate - lastTime > maxAge {
            return true
        }
        return false
    }
    
    //获取system-app最新的版本信息
    func fetchSystemAppNewVersion(appId: String, urlString: String) {
        
        //system-app升级操作
        //1、点击升级，退回到桌面界面
        //2、开始动画，下载文件
        operateMonitor.startAnimationMonitor.onNext(appId)
        BFSNetworkManager.shared.downloadApp(appId: appId, urlString: urlString)
        
    }
    
    //system-app升级后，更新本地文件，提供给app使用
    func updateSystemAppFile(appId: String, path: String) {
        let desPath = documentdir + "/system-app"
        
        NVHTarGzip.sharedInstance().unTarGzipFile(atPath: path, toPath: desPath) { error in
            if error == nil {
                let schemePath = desPath + "/\(appId)/sys"   //后面看返回数据修改
                Schemehandler.setupHTMLCache(appId: appId, fromPath: schemePath)
                RefreshManager.saveLastUpdateTime(appId: appId, time: Date().timeStamp)
            }
        }
    }
    
    //扫码添加安装的app数据
    func addAPPFromScan(appId: String, dict: [String:Any]) {
        scanManager.writeLinkJson(appId: appId, dict: dict)
    }
    
    //获取system-app的entryPath
    func systemAPPEntryPath(appId: String) -> String? {
        return sysManager.fetchEntryPath(appId: appId)
    }
    
    //获取system-app的appType
    func systemAPPType(appId: String) -> String? {
        return sysManager.readAppType(appId: appId)
    }
    
    //获取system-app的web地址
    func systemWebAPPURLString(appId: String) -> String? {
        return sysManager.readWebAppURLString(appId: appId)
    }
    //获取system-app的版本号
    func systemAPPVersion(appId: String) -> String {
        return sysManager.readMetadataVersion(appId: appId) ?? ""
    }
    
    //更新信息下载完后，重新下载项目文件,  可能不需要判断system-app 看最后system-app升级时的需求
    private func downloadNewFile(appId: String) {
        
        let type = currentAppType(appId: appId)
        if type == .recommend {
            downloadRecommendFile(appId: appId)
        } else if type == .system {
            //暂时注释掉
           // downloadSystemFile(appId: appId)
        }
    }
    //更新信息下载完后，重新下载Recommend项目文件
    private func downloadRecommendFile(appId: String) {
        //3、如果有最新信息，停止缓存中的更新
        guard hasNewUpdateInfo(appId: appId) else { return }
        //4再从从bfs-app-id/tmp/autoUpdate/缓存中读取最新的json数据
        guard let newURLString = readCachedAppConfig(appId: appId) else { return }
        let currentURLString = fileLinkDict[appId]

        // FIXME: 为什么要重新下载
        if currentURLString == nil {
            operateMonitor.startAnimationMonitor.onNext(appId)
            BFSNetworkManager.shared.downloadApp(appId: appId, urlString: newURLString)
        } else {
            //5、重新下载
            reloadUpdateFile(appId: appId, cancelUrlString: currentURLString, urlString: newURLString)
        }
    }
    //更新信息下载完后，重新下载System项目文件
    private func downloadSystemFile(appId: String) {
        //3、如果有最新信息，弹框
        guard isSystemUpdate(appId: appId) else { return }
        //4再从从bfs-app-id/tmp/autoUpdate/缓存中读取最新的json数据
        guard let newURLString = readCachedAppConfig(appId: appId) else { return }
        alertUpdateViewController(appId: appId, urlstring: newURLString)
        
    }
    
    //从bfs-app-id/tmp/autoUpdate/缓存中读取url数据
    private func readCachedAppConfig(appId: String) -> String? {
        let cacheInfo = readCacheUpdateInfo(appId: appId)
        let caches = cacheInfo?["files"] as? [[String:Any]]
        let fileInfo = caches?.first
        guard let urlstring = fileInfo?["url"] as? String else { return nil }
        return urlstring
    }
    
    //读取缓存更新的信息
    private func readCacheUpdateInfo(appId: String) -> [String:Any]? {
        
        let type = currentAppType(appId: appId)
        if type == .system {
            return sysManager.readCacheUpdateInfo(appId: appId)
        } else if type == .recommend {
            return sharedRecommendAppManager.readCacheUpdateInfo(appId: appId)
        } else if type == .user {
            return scanManager.readCacheUpdateInfo(appId: appId)
        }
        return nil
    }
    
    //从link.json的autoUpdate读取更新信息
    private func refreshNewAutoUpdateInfo(appId: String, isCompare: Bool) {
        guard let updateString = autoUpdateURLString(appId: appId) else { return }
        let refreshManager = RefreshManager()
        refreshManager.loadUpdateRequestInfo(appId: appId, urlString: updateString, isCompare: isCompare)
    }
    
    //取消缓存的下载信息，重新下载最新信息
    private func reloadUpdateFile(appId: String, cancelUrlString: String?, urlString: String?) {
        BFSNetworkManager.shared.cancelNetworkRequest(urlString: cancelUrlString)
        if urlString != nil {
            operateMonitor.startAnimationMonitor.onNext(appId)
            BFSNetworkManager.shared.downloadApp(appId: appId, urlString: urlString!)
        }
    }
    
    //获取自动更新的url
    private func autoUpdateURLString(appId: String) -> String? {
        let type = currentAppType(appId: appId)
        if type == .system {
            return sysManager.readAutoUpdateURLInfo(appId: appId)
        } else if type == .recommend {
            return sharedRecommendAppManager.readAutoUpdateURLInfo(appId: appId)
        } else if type == .user {
            return scanManager.readAutoUpdateURLInfo(appId: appId)
        }
        return nil
    }
    
    //获取自动更新的时间间隔
    private func autoUpdateMaxAge(appId: String) -> Int? {
        let type = currentAppType(appId: appId)
        if type == .system {
            return sysManager.readAutoUpdateMaxAge(appId: appId)
        } else if type == .recommend {
            return sharedRecommendAppManager.readAutoUpdateMaxAge(appId: appId)
        } else if type == .user {
            return scanManager.readAutoUpdateMaxAge(appId: appId)
        }
        return nil
    }
    
    //判断是否有新的更新消息
    private func hasNewUpdateInfo(appId: String) -> Bool {
        let type = currentAppType(appId: appId)
        if type == .system {
            return sysManager.isNewUpdateInfo(appId: appId)
        } else if type == .recommend {
            return sharedRecommendAppManager.isNewUpdateInfo(appId: appId)
        } else if type == .user {
            return scanManager.isNewUpdateInfo(appId: appId)
        }
        return false
    }
    
    //判断system-app是否有新的更新消息
    private func isSystemUpdate(appId: String) -> Bool {
        guard let currentVersion = sysManager.readMetadataVersion(appId: appId) else { return false }
        let cacheDict = sysManager.readCacheUpdateInfo(appId: appId)
        guard let version = cacheDict?["version"] as? String  else { return false }
        let result = version.versionCompare(oldVersion: currentVersion)
        if result == .orderedAscending {
            return true
        }
        return false
    }
    
    func recommandAppList(){
        
    }
    
    //获取文件夹的类型
    private func loadAppType() {
        let recommendAppList = sharedRecommendAppManager.appDirs()
        let sysAppList = sysManager.appDirs()
        let userAppList = scanManager.appDirs()
        
        appIdList = Set(Array( recommendAppList + sysAppList + userAppList))
        guard appIdList.count > 0 else { return }
        
        for appId in appIdList {
            if sysAppList.contains(appId) {
                appType[appId] = .system
            } else if userAppList.contains(appId) {
                appType[appId] = .user
            } else if recommendAppList.contains(appId) {
                appType[appId] = .recommend
            }
        }
    }
    
    //下载弹框
    private func alertUpdateViewController(appId: String, urlstring: String?) {
        let alertVC = UIAlertController(title: "确认下载更新吗？", message: nil, preferredStyle: .alert)
        let sureAction = UIAlertAction(title: "确认", style: .default) { action in
            if urlstring != nil {
                operateMonitor.startAnimationMonitor.onNext(appId)
                BFSNetworkManager.shared.downloadApp(appId: appId, urlString: urlstring!)
            }
            let type = self.currentAppType(appId: appId)
//            if appType != nil {
//                type = appType!
//            }
            if type == .system {
                operateMonitor.backMonitor.onNext(appId)
            } else if type == .recommend {
                self.refreshNewAutoUpdateInfo(appId: appId, isCompare: true)
            } else if type == .user {
                self.refreshNewAutoUpdateInfo(appId: appId, isCompare: true)
            }
        }
        let cancelAction = UIAlertAction(title: "取消", style: .cancel) { action in
            let type = self.currentAppType(appId: appId)
            if type == .recommend {
                self.refreshNewAutoUpdateInfo(appId: appId, isCompare: false)
            }
        }
        alertVC.addAction(sureAction)
        alertVC.addAction(cancelAction)
        
        let appDelegate = UIApplication.shared.delegate as! AppDelegate
        let controller = appDelegate.window?.rootViewController
        controller?.present(alertVC, animated: true)
    }
}

//MARK: 获取app信息
extension InnerAppFileManager {
    
    //根据文件名获取app名称
    func currentAppName(appId: String) -> String {
        return appNames[appId] ?? ""
    }
    //根据文件名获取app图片
    func currentAppImage(appId: String) -> UIImage? {
        return appImages[appId] ?? nil
    }
    //根据文件名获取app类型
    func currentAppType(appId: String) -> InnerAppType? {
        return appType[appId] 
    }

    
    //获取所有的icon名称
    private func fetchAppNames() {
        for (appId, type) in appType {
            if type == .system {
                appNames[appId] = sysManager.appName(appId: appId)
            } else if type == .recommend {
                appNames[appId] = sharedRecommendAppManager.appName(appId: appId)
            } else if type == .user {
                appNames[appId] = scanManager.appName(appId: appId)
            }
        }
    }
    //获取所有的icon图片
    private func fetchAppIcons() {
        for (key,type) in appType {
            if type == .system {
                appImages[key] = sysManager.appIcon(appId: key)
            } else if type == .recommend {
                appImages[key] = sharedRecommendAppManager.appIcon(appId: key)
            }
        }
    }
}

//MARK: 更新app下载后的信息
extension InnerAppFileManager {
    //获取app红点信息
    private func totalRedHotContent() {
        let path = redSoptCachePath
        guard let content = try? FileSystemManager.readFile(at: URL(fileURLWithPath: path), with: true) else { return }
        redDict = ChangeTools.stringValueDic(content) as? [String:Bool] ?? [:]
    }
    
    //更新新版本红点
    func updateRedHot(appId: String, statue: Bool) {
        redDict[appId] = statue
        
        guard let redString = ChangeTools.dicValueString(redDict) else { return }
        try? FileSystemManager.writeFile(at: URL(fileURLWithPath: redSoptCachePath), with: redString, recursive: true, encoding: true)
    }
    //得到红点状态
    func redHot(appId: String) -> Bool {
        return redDict[appId] ?? false
    }
    
    //更新本地文件数据
    func updateLocalSystemAPPData(appId: String) {
        
        if !appIdList.contains(appId) {
            appIdList.insert(appId)
            
        }
        appType[appId] = .system
        appNames[appId] = sysManager.appName(appId: appId)
        appImages[appId] = sysManager.appIcon(appId: appId)
    }

}
