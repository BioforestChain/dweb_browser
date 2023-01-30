//
//  AppVersionManager.swift
//  BFExplorer
//
//  Created by ui06 on 1/17/23.
//

import Foundation
import SwiftyTimer
import Alamofire

let minRegularInterval: TimeInterval = 5

//暂时只管理recommend app 版本
let appVersionMgr = AppVersionManager()

class AppVersionManager: NSObject {
    
    
    override init() {
        super.init()
        //只需要读取一次，跟着版本走的
        readAllupdateIntervalMap()
        
    }
    
    var updateIntervalMap = [String : Int]()
    var versionMap = [String : String]()
    
    var repeatTimes = 0
    
    func startCheck(){
        checkAllAppUpdateInfo(needMatch: false)
        Timer.every(minRegularInterval.seconds) {
            self.repeatTimes += 1
            self.checkAllAppUpdateInfo(needMatch: true)
        }
    }
    
    private func checkUpdateInfo(appIds: [String]){
        print("-----------------------------")
        appIds.forEach {
            print(Date().dateToString() + "check \($0) update Infos ")
            // TODO: it's time to request version Info
            requestAppVersion(by: $0)
        }
        
    }
    
    //needMatch 检查是否需要检查到了达轮询时间
    func checkAllAppUpdateInfo(needMatch: Bool = true){
        if !needMatch{
            updateIntervalMap.forEach { (appId: String, value: Int) in
                checkUpdateInfo(appIds: [appId])
            }
        }else{
            let updates = updateIntervalMap.filter ({ intervalDic in
                0 == repeatTimes % (intervalDic.value)
            })
            let appIds = updates.map { $0.key }
            checkUpdateInfo(appIds: appIds)
        }
    }
    
    
    private func readAllupdateIntervalMap(){
        let configs = sharedRecommendAppManager.fetchAllRecommandAppLinkConfig()
        sharedRecommendAppManager.appDirs().forEach { appId in
            configs?.forEach({ dict in
                guard let updateDic = dict["autoUpdate"] as? [String: Any]  else { return }
                guard let versionUrl = updateDic["url"] as? String else { return }
                guard let updateInterval = updateDic["maxAge"] as? Int else { return }
                
                updateIntervalMap[appId] = updateInterval / Int(minRegularInterval)  // 60的多少倍，意味着多少分钟轮训一次
                versionMap[appId] = versionUrl
            })
        }
        
        updateIntervalMap["AAAAA"] = Int(5 / minRegularInterval)  //1
        updateIntervalMap["BBBBB"] = Int(10 / minRegularInterval) //2
        updateIntervalMap["CCCCC"] = Int(10 / minRegularInterval) //2
        updateIntervalMap["DDDDD"] = Int(20 / minRegularInterval) //4
    }
    
    func requestAppVersion(by appId: String){
        guard let url = versionMap[appId] as String? else { return }
        
        AF.request(url).responseData { response in
            guard let result = String(data: response.data!, encoding: .utf8) else { return }
            let dict = ChangeTools.stringValueDic(result)
            guard let config = dict?["data"] as? [String:Any] else { return }
            self.cacheAppVersionToLocalFile(appId: appId, config: config)
        }
    }
    
    
    //每次写入最新版本信息，最多保留5项，其余删掉，实际上只有最新的那一个用的上
    func cacheAppVersionToLocalFile(appId: String, config: [String:Any]){
        guard let serverVersion = config["version"] as? String else { return }
        
        if !self.shouldCacheVersionInfo(appId: appId, to: "1.0.2"){
            return
        }
        
        let versionRecordsDir = self.appVersionConfigDir(for: appId)
        if !FileManager.default.fileExists(atPath: versionRecordsDir) {
            try? FileManager.default.createDirectory(atPath: versionRecordsDir, withIntermediateDirectories: true)
        }
        var currentTime = Date().addingTimeInterval(8*3600).dateToString(identifier: "UTC")
        currentTime = currentTime.replacingOccurrences(of: ":", with: "").replacingOccurrences(of: " ", with: "")
        let versionFilePath = versionRecordsDir + "/\(currentTime).json"
        let jsonString = ChangeTools.dicValueString(config) ?? ""
        guard jsonString.count > 0 else { return }
        if !FileManager.default.fileExists(atPath: versionFilePath) {
            FileManager.default.createFile(atPath: versionFilePath, contents: nil)
        }
        
        do {
            try jsonString.write(toFile: versionFilePath, atomically: true, encoding: .utf8)
        } catch {
            print(error.localizedDescription)
        }
        
        keep5ConfigsOnly(configDir: versionRecordsDir)
    }
        
    //只保留5条记录
    func keep5ConfigsOnly(configDir: String){
        let manager = FileManager.default
        let sortedDirs = try? manager.contentsOfDirectory(atPath: configDir).sorted { $0 > $1 }
        if let dirs = sortedDirs, dirs.count > 5 {
            for i in 5...dirs.count-1{
                try? manager.removeItem(atPath: configDir + "/" + dirs[i])
            }
        }
    }

    
    func shouldCacheVersionInfo(appId: String, to serverVersion: String) -> Bool{
        guard let localVersionConfig = self.localRecordedAppConfig(appId: appId) else { return true }
        guard let localVersion = localVersionConfig["version"] as? String else { return true}
        return isIncreasing(nowVersion: localVersion, newVersion: serverVersion)
    }
    
    private func appVersionConfigDir(for appId: String) -> String {
        let type = sharedInnerAppFileMgr.currentAppType(appId: appId)
        var appDir = ""
        if type == .recommend{
            appDir = documentdir + "/recommend-app"
        }else if type == .system{
                appDir = documentdir + "/system-app"
        }else if type == .user{
            appDir = documentdir + "/user-app"
        }
        return appDir + "/\(appId)/tmp/autoUpdate"
    }
    
    //取本地最新的记录
    func localRecordedAppConfig(appId: String) -> [String:Any]? {
        let configDir = self.appVersionConfigDir(for: appId)
        
        let manager = FileManager.default
        let configs = try? manager.contentsOfDirectory(atPath: configDir).sorted { $0 > $1 }
        guard let first = configs?.first else { return nil }
        let path = configDir + "/" + first
        guard let data = manager.contents(atPath: path) else { return nil }
        guard let cacheString = String(data: data, encoding: .utf8) else { return nil }
        return ChangeTools.stringValueDic(cacheString)
    }
    
    func isIncreasing(nowVersion:String, newVersion:String) -> Bool {
        let nowArray = nowVersion.split(separator: ".")
        let newArray = newVersion.split(separator: ".")
        let count = nowArray.count > newArray.count ? newArray.count : nowArray.count
        for index in 0...count - 1 {
            if Int(nowArray[index])! < Int(newArray[index])!  {
                return true
            }
        }
        return false
    }
}
