//
//  Schemehandler.swift
//  DWebBrowser
//
//  Created by mac on 2022/5/23.
//

import UIKit
import WebKit
import MobileCoreServices

let schemeString: String = "iosqmkkx"

class Schemehandler: NSObject, WKURLSchemeHandler {

    private var schemeTasksDict: [String: Bool] = [:]
    private var schemeTask: WKURLSchemeTask?
    
    private var fileString: String = ""
    
    init(appId: String) {
        super.init()
        fileString = appId
    }
    
    func webView(_ webView: WKWebView, start urlSchemeTask: WKURLSchemeTask) {
        guard var urlstring = urlSchemeTask.request.url?.absoluteString else { return }
        schemeTasksDict[urlSchemeTask.description] = true
        print(urlstring)
        if needIntercept(urlstring: urlstring) {
            analysisSetUiFunction(urlSchemeTask: urlSchemeTask, urlString: urlstring)
            return
        }
        
        guard let appId = urlSchemeTask.request.url?.path else { return }
        let mainPath = Schemehandler.filePath()
        let htmlPath = mainPath + "/" + fileString//Schemehandler.appId()
        let filepath = htmlPath + appId
        print(filepath)
        let manager = FileManager.default
        if manager.fileExists(atPath: filepath) {
            
            guard let data = manager.contents(atPath: filepath) else { return }
            var type = self.mimeType(pathExtension: filepath)
            if type.count == 0 {
                type = "text/html"
            }
            
            let response = URLResponse(url: urlSchemeTask.request.url!, mimeType: type, expectedContentLength: data.count, textEncodingName: nil)
            print(response)
            urlSchemeTask.didReceive(response)
            urlSchemeTask.didReceive(data)
            urlSchemeTask.didFinish()
            
        } else {
            if urlstring.hasPrefix(schemeString) {
                if urlstring.contains("\(schemeString)://") {
                    if let replaceURL = NetworkMap.shared.replaceDownloadUrlString(urlString: urlstring) {
                        urlstring = replaceURL
                    }
                } else {
                    urlstring = urlstring.replacingOccurrences(of: schemeString, with: "http")
                }
            }
            guard let url = URL(string: urlstring) else { return }
            let request = URLRequest(url: url)
            let config = URLSessionConfiguration.default
            let session = URLSession(configuration: config)
            let task = session.dataTask(with: request) { data, response, error in
                DispatchQueue.main.async {
                    let isScheme = self.schemeTasksDict[urlSchemeTask.description] ?? false
                    guard isScheme else { return }
                    if (response != nil) {
                        urlSchemeTask.didReceive(response!)
                    } else {
                        let response = URLResponse(url: urlSchemeTask.request.url!, mimeType: "空类型", expectedContentLength: data?.count ?? 0, textEncodingName: nil)
                        urlSchemeTask.didReceive(response)
                    }
                    if data != nil {
                        urlSchemeTask.didReceive(data!)
                    }
                    if (error != nil) {
                        urlSchemeTask.didFailWithError(error!)
                    } else {
                        urlSchemeTask.didFinish()
                    }
                }
            }
            task.resume()
        }
    }

    func webView(_ webView: WKWebView, stop urlSchemeTask: WKURLSchemeTask) {
        print("stop")
        schemeTasksDict[urlSchemeTask.description] = false
    }
    
    static func setupHTMLCache(appId: String, fromPath: String) {
        
        clearHTMLCache(appId: appId)
        let manager = FileManager.default
        let markString = appId//Schemehandler.appId()
        let toPath = Schemehandler.filePath() + "/" + markString
        if manager.fileExists(atPath: toPath) {
            
        } else {
            try? manager.copyItem(atPath: fromPath, toPath: toPath)
        }
    }
    
    static func clearHTMLCache(appId: String) {
        let manager = FileManager.default
        let markString = appId//appId()
        let toPath = filePath() + "/" + markString
        if manager.fileExists(atPath: toPath) {
            try? manager.removeItem(atPath: toPath)
        }
    }
    
    static func appId() -> String {
        return "bmr9vohvtvbvwrs3p4bwgzsmolhtphsvvj"
    }

    static func filePath() -> String {
         documentdir
    }
    
    func mimeType(pathExtension: String) -> String {
        
        let defaultMIMEType = "application/octet-stream"
        // 获取⽂件名后缀标记
        guard let tag = pathExtension.components(separatedBy: "/").last?
            .components(separatedBy: ".").last?
            .trimmingCharacters(in: .whitespacesAndNewlines) else { return defaultMIMEType }
        // 异常则返回⼆进制通⽤类型
        guard let uti = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension, tag as CFString, nil)?.takeRetainedValue(),
              let mimeType = UTTypeCopyPreferredTagWithClass(uti, kUTTagClassMIMEType)?.takeRetainedValue()
        else { return defaultMIMEType }
        return mimeType as String
    }
    
    //得到返回的数据
    private func analysisAbsoluteString(urlString: String) -> String {
        
        guard urlString.contains("=") else { return "" }
        let array = urlString.components(separatedBy: "=")
        if let last = array.last {
            let result = last.hexStringToString(symbol: ",")
            return result
        }
        return ""
    }
    //获取函数名和参数
    private func fetchFunctionAndParam(content: String) -> (String?,Any) {
        
        let result = analysisAbsoluteString(urlString: content)
        let dict = ChangeTools.stringValueDic(result)
        let function = dict?["function"] as? String
        let param = dict?["data"] as Any
        return (function,param)
    }
    
    private func analysisSetUiFunction(urlSchemeTask: WKURLSchemeTask, urlString: String) {
        let result = fetchFunctionAndParam(content: urlString)
        let function = result.0 ?? ""
        let param = result.1
        let dict = ["scheme":urlSchemeTask,"function":function,"param":param] as [String : Any]
        NotificationCenter.default.post(name: NSNotification.Name.interceptNotification, object: nil, userInfo: dict)
    }
    
    //判断是否拦截请求
    private func needIntercept(urlstring: String) -> Bool {
        
        if urlstring.contains("setUi?data") {
            return true
        }
        
        if urlstring.contains("poll?data") {
            return true
        }
        return false
    }
}
