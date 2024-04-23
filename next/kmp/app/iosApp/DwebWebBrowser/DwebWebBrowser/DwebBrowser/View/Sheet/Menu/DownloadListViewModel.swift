//
//  DownloadListViewModel.swift
//  DwebWebBrowser
//
//  Created by instinct on 2024/4/12.
//

import Foundation
import Observation

@Observable class DownloadListViewModel {
    
    var downloadedDatas: [DownloadItem] = []
    var downloadingDatas: [DownloadItem] = []
    
    @ObservationIgnored
    private var toDeletes = Set<String>()
    
    var isEmpty: Bool {
        return downloadingDatas.count + downloadedDatas.count == 0
    }
        
    @ObservationIgnored
    private lazy var dateFormater: DateFormatter = {
        let formater = DateFormatter()
        formater.dateFormat = "yyyy-MM-dd"
        return formater
    }()
    
    @ObservationIgnored
    private lazy var dateStringCreator: (UInt64) -> String = { [weak self] date in
        let date = Date(timeIntervalSince1970: TimeInterval(date/1000))
        return self?.dateFormater.string(from: date) ?? ""
    }
    
    @ObservationIgnored
    private var sizeStringCreator: (UInt32) -> String = { oriSize in
        let size = Float(oriSize)
        return switch size {
            case 0..<1024: String(format: "%.1fByte", size)
            case 1024..<1024*1024: String(format: "%.1fKB", size/1024)
            case 1024*1024..<1024*1024*1024: String(format: "%.1fMB", size/1024/1024)
            default:
                String(format: "%.1fGB", size/1024/1024/1024)
        }
    }
    
    @ObservationIgnored
    private var stateCreator: (WebBrowserViewDownloadData) -> DownloadItem.State = { data in
        return data.toDownloadState()
    }
    
    func loadDatas() {
        
        let downloadDatas = browserViewDataSource.loadAllDownloadDatas()
                
        let allDatas = downloadDatas?.map {
            DownloadItem(id: $0.id,
                         mime: $0.mime.toDownloadDataType(),
                         title: $0.name,
                         date: dateStringCreator($0.date),
                         dateValue: $0.date,
                         size: sizeStringCreator($0.size),
                         state: $0.toDownloadState(),
                         localPath: $0.localPath
            )
        } ?? []
        
        downloadingDatas = allDatas.filter({ !$0.isLoaded })
        downloadedDatas = allDatas.filter({ $0.isLoaded })

        addDownloadObserverIfNeed(downloadDatas)

    }
        
    func removeDownloading(_ indexSet: IndexSet) {
        let deletes: [String] = indexSet.map { index in
            downloadingDatas[index].id
        }
        toDeletes.formUnion(deletes)
        downloadingDatas.remove(atOffsets: indexSet)
    }
    
    func removeDownloaded(_ indexSet: IndexSet) {
        let deletes: [String] = indexSet.map { index in
            downloadedDatas[index].id
        }
        toDeletes.formUnion(deletes)
        downloadedDatas.remove(atOffsets: indexSet)
    }
    
    func commitEidt() {
        guard !toDeletes.isEmpty else { return }
        browserViewDataSource.removeDownload(ids: Array(toDeletes))
    }
    
    func pause(item: DownloadItem) {
        guard case let .loading(p) = item.state else { return }
        item.updateState(.pause(p))
        browserViewDataSource.pauseDownload(id: item.id)
    }
    
    func resume(item: DownloadItem) {
        guard case let .pause(p) = item.state else { return }
        item.updateState(.loading(p))
        browserViewDataSource.resumeDownload(id: item.id)
    }
    
    private func addDownloadObserverIfNeed(_ datas: [WebBrowserViewDownloadData]?) {
        guard let datas = datas, datas.count > 0 else { return }
        datas.forEach { [weak self] in
            guard let self = self, $0.isNeedObserved else { return }
            browserViewDataSource.addDownloadObserver(id: $0.id, didChanged: self.downloadingDataChanged)
        }
    }
        
    private func downloadingDataChanged(data: WebBrowserViewDownloadData) {
        DispatchQueue.main.async {
            if let item = self.downloadingDatas.first(where: { $0.id == data.id }) {
                item.updateState(data.toDownloadState())
                self.downloadingToDownloadedHandleIfNeed(item)
            }
            Log("\(data.name) \(data.progress) \(Thread.current)")
        }
    }
    
    private func downloadingToDownloadedHandleIfNeed(_ item: DownloadItem) {
        guard item.isLoaded else { return }
        downloadingDatas.removeAll { $0.id == item.id }
        downloadedDatas.append(item)
        downloadedDatas.sort { $0.dateValue > $1.dateValue }
    }
    
    private func removeAllDownloadObservers() {
        browserViewDataSource.removeAllDownloadObservers()
    }
    
    func clear() {
        removeAllDownloadObservers()
    }
}
