

import UIKit
import Alamofire
import SSZipArchive

class BFSNetworkManager: NSObject {
    
    static let shared = BFSNetworkManager()
    
    private var requestDict: [String: DownloadRequest] = [:]
    
    func downloadApp(appId: String? = nil, urlString: String) {
        guard let appId = obtainAppName(from: urlString) else{ return }
        let testUrl = "http://dldir1.qq.com/qqfile/qq/QQ7.9/16621/QQ7.9.exe"
        
        let request = AF.download(urlString).downloadProgress { progress in
            print(progress.fractionCompleted)  //进度值
            NotificationCenter.default.post(name: NSNotification.Name.progressNotification, object: nil, userInfo: ["progress": "\(progress.fractionCompleted)", "appId": appId])
        }.responseURL { response in
            print(response)
            switch response.result {
            case .success:
                //下载后的文件路径
                if response.fileURL != nil {
                    DispatchQueue.global().async {

                        // FIXME: 需要根据下载的app类型来判断是属于哪种
                        var desPath = documentdir + "/system-app/"
                        DispatchQueue.main.async {
                            NVHTarGzip.sharedInstance().unTarGzipFile(atPath: response.fileURL!.path, toPath: desPath) { error in
                                if error == nil {
                                    var schemePath = desPath + "\(appId)/sys"
                                    if self.shouldUpdate(name: appId) {
                                        Schemehandler.setupHTMLCache(appId: appId, fromPath: schemePath)
                                        sharedInnerAppFileMgr.updateRedHot(appId: appId, statue: true)
                                    }
                                    RefreshManager.saveLastUpdateTime(appId: appId, time: Date().timeStamp)
                                }
                                let msg = (error == nil) ? "complete" : "fail"
                                NotificationCenter.default.post(name: NSNotification.Name.progressNotification, object: nil, userInfo: ["progress": msg, "appId": appId])
                            }
                             
                        }
                    }
                }
            case .failure:
                NotificationCenter.default.post(name: NSNotification.Name.progressNotification, object: nil, userInfo: ["progress": "fail", "appId": appId])
            }
        }
        
        self.requestDict[appId] = request
        
    }
    
    func cancelNetworkRequest(urlString: String?) {
        guard urlString != nil else { return }
        AF.session.getTasksWithCompletionHandler { dataTask, uploadTask, downloadTask in
            
            dataTask.forEach { task in
                if task.originalRequest?.url?.absoluteString == urlString {
                    //到时候需要添加取消的id
                    NotificationCenter.default.post(name: NSNotification.Name.progressNotification, object: nil, userInfo: ["progress": "cancel"])
                    task.cancel()
                }
            }
            uploadTask.forEach { task in
                if task.originalRequest?.url?.absoluteString == urlString {
                    NotificationCenter.default.post(name: NSNotification.Name.progressNotification, object: nil, userInfo: ["progress": "cancel"])
                    task.cancel()
                }
            }
            downloadTask.forEach { task in
                if task.originalRequest?.url?.absoluteString == urlString {
                    NotificationCenter.default.post(name: NSNotification.Name.progressNotification, object: nil, userInfo: ["progress": "cancel"])
                    task.cancel()
                }
            }
        }
    }
    
    func obtainAppName(from url: String) -> String?{
        guard let appFullName = url.split(separator: "/").last,
              let appShortName = appFullName.split(separator: ".").first else{ return nil}
        print(appShortName)
        return String(appShortName)
    }
    
    func suspendRequest(appId: String) {
        if let request = requestDict[appId] {
            request.suspend()
        }
        
    }
    
    func resumeRequest(appId: String) {
        if let request = requestDict[appId] {
            request.resume()
        }
    }
    
    //读取文件夹下面的第一级文件夹
    private func subFilePathNames(atPath path: String) -> [String] {
        var fileList: [String] = []
        guard let filePaths = FileManager.default.subpaths(atPath: path) else { return fileList }
        for appId in filePaths {
            var isDir: ObjCBool = true
            let fullPath = "\(path)/\(appId)"
            if FileManager.default.fileExists(atPath: fullPath, isDirectory: &isDir) {
                if !isDir.boolValue {
                    
                } else {
                    //后续是不需要判断.的，因为这是临时添加的，后续从网络获取
                    if !appId.contains("/"), !appId.contains(".") {
                        fileList.append(appId)
                    }
                }
            }
        }
        return fileList
    }
    
    private func shouldUpdate(name: String) -> Bool {
        
//        guard let filePath = documentdir else { return false }
        let desPath = documentdir + "/system-app/\(name)"
        if FileManager.default.fileExists(atPath: desPath) {
            let tmpManager = BatchTempManager()
            let tmpVersion = tmpManager.tempAppVersion(name: name)
            let oldVersion = sharedInnerAppFileMgr.systemAPPVersion(appId: name)
            let result = tmpVersion.versionCompare(oldVersion: oldVersion)
            if result == .orderedAscending {
                return true
            }
            return false
        } else {
            return true
        }
    }
    
    private func copyItemToSystem(name: String) {
//        guard let filePath = documentdir else { return }
        let desPath = documentdir + "/system-app/\(name)"
        let tmpPath = NSTemporaryDirectory() + name
        do {
            if !FileManager.default.fileExists(atPath: desPath) {
                try FileSystemManager.mkdir(at: URL(fileURLWithPath: documentdir + "/system-app/"), recursive: true)
            }
            try FileManager.default.copyItem(atPath: tmpPath, toPath: desPath)
        } catch {
            print(error.localizedDescription)
        }
    }
}
