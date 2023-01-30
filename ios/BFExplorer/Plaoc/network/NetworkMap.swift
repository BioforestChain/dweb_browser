//
//  NetworkMap.swift
//  Plaoc-iOS
//
//  Created by ui03 on 2022/12/13.
//

import Foundation

class NetworkMap {
    
    static let shared = NetworkMap()
    private var dWebView_host: String = ""
    
    private var whiteString = ""
    private var importmap: [String:String] = [:]
    func metaData(metadata:String, appId: String) {
        guard let dict = ChangeTools.stringValueDic(metadata) else { return }
        dWebView_host = appId
        // 设置白名单
        if let list = dict["whitelist"] as? [String] {
            whiteString = list.joined()
        }
        // 设置映射列表
        let dweb = dict["dwebview"] as? [String:Any]
        if let mapDict = dweb?["importmap"] as? [[String : String]] {
            for subDict in mapDict {
                let value = subDict["response"]
                if let key = subDict["url"] {
                    importmap[resolveUrl(path: key)] = value
                }
            }
        }
        
        // 给入口文件也添加加载路径
        let manifest = dict["manifest"] as? [String:Any]
        guard let entryList = manifest?["enters"] as? [String] else { return }
        for entry in entryList {
            importmap[entry] = resolveUrl(path: entry)
        }
    }
    // 返回应用虚拟路径
    private func resolveUrl(path:String) -> String {
        return "dweb://\(dWebView_host.uppercased()).channel.dweb\(shakeUrl(path: path))"
    }
    /** 适配路径没有 / 的尴尬情况，没有的话会帮你加上*/
    private func shakeUrl(path: String) -> String {
        if (path.hasPrefix("/")) {
            return path
        }
        return "/\(path)"
    }
    
    //判断是否需要重新下载
    func replaceDownloadUrlString(urlString: String) -> String? {
        return importmap[urlString]
    }
    
}


