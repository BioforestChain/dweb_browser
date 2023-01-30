//
//  BatchRecommendManager.swift
//  BFS
//
//  Created by ui03 on 2022/9/6.
//

import UIKit

class RecommendAppManager: InnerAppManager {

    override var filePath: String { Bundle.main.bundlePath + "/app/recommend-app" }
    
    override var appInstalledPath: String {
        return documentdir + "/recommend-app"
    }
    
    override func iconImagePath(appId: String) -> String {
        return filePath + "/\(appId)/sys/"
    }
    
    
    func fetchRecommandAppLinkConfig(by appId: String) -> [String: Any]?{
        let path = filePath + "/\(appId)/boot/link.json"
        guard let data = FileManager.default.contents(atPath: path) else { return nil}
        guard let content = String(data: data, encoding: .utf8) else { return nil}
        return  ChangeTools.stringValueDic(content)
    }
    
    func fetchAllRecommandAppLinkConfig() -> [[String : Any]]? {
        var configs = [[String : Any]]()
        self.appDirs().forEach { appId in
            if let config = fetchRecommandAppLinkConfig(by: appId){
                configs.append(config)
            }
        }
        return configs
    }
}
