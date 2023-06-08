//
//  DownloadAppViewModel.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/26.
//

import SwiftUI
import Combine

let downloadPublisher = PassthroughSubject<Float, Never>()
let sharedNetworkMgr = DownloadAppViewModel()

class DownloadAppViewModel: NSObject {
    
    private var progress: Float = 0
    private var appId: String = ""
    private var session: URLSession?
    
    func downloadApp(appId: String? = nil, urlString: String) {
        
        guard let appId = obtainAppName(from: urlString) else { return }
        guard let url = URL(string: urlString) else { return }
        self.appId = appId
        session = URLSession(configuration: .default, delegate: self, delegateQueue: .main)
        let task = session?.downloadTask(with: URLRequest(url: url))
        task?.resume()
    }
    
    private func obtainAppName(from url: String) -> String?{
        guard let appFullName = url.split(separator: "/").last,
              let appShortName = appFullName.split(separator: ".").first else{ return nil}
        print(appShortName)
        return String(appShortName)
    }
    
    func cancelNetworkRequest(urlString: String?) {
        guard urlString != nil else { return }
        session?.getTasksWithCompletionHandler { dataTask, uploadTask, downloadTask in
            
            dataTask.forEach { task in
                if task.originalRequest?.url?.absoluteString == urlString {
                    //到时候需要添加取消的id
//                    NotificationCenter.default.post(name: NSNotification.Name.progressNotification, object: nil, userInfo: ["progress": "cancel"])
                    task.cancel()
                }
            }
            downloadTask.forEach { task in
                if task.originalRequest?.url?.absoluteString == urlString {
//                    NotificationCenter.default.post(name: NSNotification.Name.progressNotification, object: nil, userInfo: ["progress": "cancel"])
                    task.cancel()
                }
            }
        }
    }
}


extension DownloadAppViewModel: URLSessionDownloadDelegate {
    
    //下载完成后,通知c#交互
    func urlSession(_ session: URLSession, downloadTask: URLSessionDownloadTask, didFinishDownloadingTo location: URL) {
        
        let appConfig = ["appId": appId, "tempZipPath": location.path]
//        NotificationCenter.default.post(name: NSNotification.Name.DownloadAppFinishedNotification, object: nil,userInfo: appConfig)
    }
    
    func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        if error != nil {
            print("download fail: \(error!.localizedDescription)")
            NotificationCenter.default.post(name: NSNotification.Name.downloadFail, object: nil)
        } else {
            NotificationCenter.default.post(name: NSNotification.Name.downloadComplete, object: nil)
        }
    }
    
    func urlSession(_ session: URLSession, downloadTask: URLSessionDownloadTask, didWriteData bytesWritten: Int64, totalBytesWritten: Int64, totalBytesExpectedToWrite: Int64) {
        progress = Float(totalBytesWritten) / Float(totalBytesExpectedToWrite)
        downloadPublisher.send(progress)
    }
}
