//
//  RefreshManager.swift
//  BFS
//
//  Created by ui03 on 2022/8/26.
//

import UIKit
import SwiftyJSON

class RefreshManager: NSObject {

    private var task: URLSessionDataTask?
    
    var lastUpdateTime: Int? {
        return UserDefaults.standard.object(forKey: "updateTime") as? Int
    }
    
    static func fetchLastUpdateTime(appId: String) -> Int? {
        return UserDefaults.standard.object(forKey: appId) as? Int
    }
    
    
    // FIXME: 太多这个记录更新了，这个字段具体做什么用的？
    static func saveLastUpdateTime(appId: String, time: Int) {
        UserDefaults.standard.setValue(time, forKey: appId)
    }
    
    func loadUpdateRequestInfo(appId: String? = nil, urlString: String? = nil, isCompare: Bool = false) {
        
        guard urlString != nil else { return }
        guard let url = URL(string: urlString!) else { return }
        let request = URLRequest(url: url)
        let session = URLSession.shared
        let task = session.dataTask(with: request) { data, response, error in
            //缓存到bfs-app-id/tmp/autoUpdate/文件中 格式为TIME.json  需要校验
            guard data != nil else { return }
            guard let result = String(data: data!, encoding: .utf8) else { return }
            let dict = ChangeTools.stringValueDic(result)
            guard let dataDict = dict?["data"] as? [String:Any] else { return }
            if appId != nil {
//                let versionType = self.analysisVersion(urlString: urlString!)
//                guard let subDict = self.versionTypeJson(type: versionType, dict: dataDict) else {
//                    DispatchQueue.main.async {
//                        if isCompare {
//                            self.openAlertAction()
//                        }
//                    }
//                    return
//                }
                RefreshManager.saveLastUpdateTime(appId: appId!, time: Date().timeStamp)
                InnerAppFileManager.shared.writeUpdateContent(appId: appId!, json: dataDict)
                if isCompare {
                    //TODO 发送比较版本信息
                    operateMonitor.refreshCompleteMonitor.onNext(appId!)
                }
            } else {  //扫码下载
                guard let name = dataDict["name"] as? String else { return }
                operateMonitor.scanMonitor.onNext((name,dataDict))
            }
        }
        
        task.resume()
        
    }
    
    func cancelUpdateRequest() {
        task?.cancel()
    }
    
    func testDict() -> [String:Any] {
        
        var dict: [String:Any] = [:]
        dict["version"] = "4.5"
        dict["releaseNotes"] = "test"
        dict["releaseName"] = "测试"
        dict["releaseDate"] = "2022.08.06"
        dict["files"] = [["url":"http://dldir1.qq.com/qqfile/qq/QQ7.9/16621/QQ7.9.exe",
                         "size": 102984,
                         "sha512": "haha"]]
        return dict
    }
    
    //获取当前下载的版本类型
    private func analysisVersion(urlString: String) -> String {
        //https://shop.plaoc.com/KEJPMHLA/appversion.json?type=Beta
        guard urlString.contains("type=") else { return "Release" }
        if let result = urlString.components(separatedBy: "type=").last {
            return result
        }
        return "Release"
    }
    //获取当前下载版本的内容
    private func versionTypeJson(type: String, dict: [String:Any]) -> [String:Any]? {
        
        guard let files = dict["files"] as? [[String:Any]] else { return nil }
        for file in files {
            if file["type"] as? String == type {
                return file
            }
        }
        return nil
    }
    //查找当前版本失败弹框
    private func openAlertAction() {
        let dict = ["message":"暂无当前可下载版本","confirmText":"取消"]
        let alertModel = AlertConfiguration(dict: JSON(dict))
        let alertView = CustomAlertPopView(frame: CGRect(x: 0, y: 0, width: screen_width, height: screen_height))
        alertView.alertModel = alertModel
        alertView.show()
    }
}
