//
//  DownloadListViewModel.swift
//  DwebWebBrowser
//
//  Created by instinct on 2024/4/12.
//

import Foundation
import Observation

@Observable class DownloadListViewModel {
    var datas: [DownloadItem] = []
    
    @ObservationIgnored
    private var toDeletes = Set<String>()
    
    var isEmpty: Bool {
        return datas.isEmpty
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
    private var sizeStringCreator: (UInt32) -> String = { size in
        switch size {
            case 0..<1024: "\(size)Byte"
            case 1024..<1024*1024: "\(size/1024)KB"
            case 1024*1024..<1024*1024*1024: "\(size/1024/1024)MB"
            default:
                "\(size/1024/1024/1024)GB"
        }
    }
    
    @ObservationIgnored
    private var stateCreator: (WebBrowserViewDownloadData) -> DownloadItem.State = { data in
        return data.toDownloadState()
    }
    
    func loadDatas() {
        
        let downloadDatas = browserViewDataSource.loadAllDownloadDatas()
                
        datas = downloadDatas?.map {
            DownloadItem(id: $0.id,
                         mime: $0.mime.toDownloadDataType(),
                         title: $0.name,
                         date: dateStringCreator($0.date),
                         size: sizeStringCreator($0.size),
                         state: $0.toDownloadState())
        } ?? []
        
        addDownloadObserverIfNeed(downloadDatas)

    }
        
    func remove(_ indexSet: IndexSet) {
        let deletes: [String] = indexSet.map { index in
            datas[index].id
        }
        toDeletes.formUnion(deletes)
        datas.remove(atOffsets: indexSet)
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
    
    func doShareAction() {
        
    }
    
    func addDownloadObserverIfNeed(_ datas: [WebBrowserViewDownloadData]?) {
        guard let datas = datas, datas.count > 0 else { return }
        datas.forEach { [weak self] in
            guard let self = self, $0.isNeedObserved else { return }
            browserViewDataSource.addDownloadObserver(id: $0.id, didChanged: self.downloadDataChanged)
        }
    }
    
    func downloadDataChanged(data: WebBrowserViewDownloadData) {
        datas.first { $0.id == data.id }?.updateState(data.toDownloadState())
        Log("\(data.name) \(data.progress)")
    }
    
    func removeAllDownloadObservers() {
        browserViewDataSource.removeAllDownloadObservers()
    }
}
