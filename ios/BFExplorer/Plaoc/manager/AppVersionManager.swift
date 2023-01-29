//
//  AppVersionManager.swift
//  BFExplorer
//
//  Created by ui06 on 1/17/23.
//

import Foundation
import SwiftyTimer

let minRegularInterval: TimeInterval = 60

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
        appIds.forEach {
            print(Date().dateToString() + "check \($0) update Infos ")
            //it's time to request version Info
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
                
                updateIntervalMap[appId] = updateInterval / Int(minRegularInterval)  //1
                versionMap[appId] = versionUrl
            })
        }

        updateIntervalMap["AAAAA"] = Int(60 / minRegularInterval)  //1
        updateIntervalMap["BBBBB"] = Int(120 / minRegularInterval) //2
        updateIntervalMap["CCCCC"] = Int(120 / minRegularInterval) //2
        updateIntervalMap["DDDDD"] = Int(240 / minRegularInterval) //4
    }
    
}
