//
//  DownloadListView.swift
//  DwebWebBrowser
//
//  Created by instinct on 2024/4/10.
//

import Foundation
import SwiftUI
import Observation

struct DownloadListView: View {
    let viewModel = DownloadListViewModel()
    var body: some View {
        Group {
            if viewModel.isEmpty {
                VStack(alignment: .center) {
                    Image(systemName: "arrow.down.circle")
                        .font(.system(size: 40))
                        .foregroundColor(.gray)
                        .padding(10)
                    Text("No Downloaded Files")
                        .font(.system(size: 24, weight: .semibold))
                        .padding(5)
                    Text("Files you download will appear here")
                        .font(.system(size: 16))
                        .foregroundColor(.gray)
                }
            } else {
                List(content: {
                    ForEach(viewModel.datas, id: \.id) { data in
                            DownloadItemView(data: data)
                        }
                        .onDelete(perform: { indexSet in
                            viewModel.remove(indexSet)
                        })
                })
                .listStyle(.plain)
            }
        }
        .toolbarTitleDisplayMode(.automatic)
        .toolbarBackground(.white)
        .navigationTitle("Download Manager")
        .task {
            viewModel.loadDatas()
        }
        .onDisappear(perform: {
            viewModel.commitEidt()
        })
    }
}

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
    private lazy var dateStringCreator: (UInt64) -> String = {
        return { [weak self] date in
            let date = Date(timeIntervalSince1970: TimeInterval(date/1000))
            return self?.dateFormater.string(from: date) ?? ""
        }
    }()
    
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
    
    func loadDatas() {
        datas = browserViewDataSource.loadAllDownloadDatas()?.map {
            return DownloadItem(id: $0.id,
                                mime: $0.mime.toDownloadDataType(),
                                title: $0.name,
                                date: dateStringCreator($0.date),
                                size: sizeStringCreator($0.size))
        } ?? []
    }
    
    func remove(_ indexSet: IndexSet) {
        let deletes: Array<String> = indexSet.map { index in
            datas[index].id
        }
        toDeletes.formUnion(deletes)
        datas.remove(atOffsets: indexSet)
    }
    
    func commitEidt() {
        guard !toDeletes.isEmpty else { return }
        browserViewDataSource.removeDownload(ids: Array(toDeletes))
    }
}

enum DownloadDataMIME {
    case text(String)
    case image(String)
    case audio(String)
    case video(String)
    case application(String) //appl
    case other(String)
    
    var sfIcon: String {
        switch self {
            case .text:
                "doc.plaintext"
            case .image:
                "photo"
            case .audio:
                "speaker.wave.3"
            case .video:
                "video"
            case let .application(subType):
                switch subType {
                    case "json": "doc.plaintext"
                    case "xml": "tablecells"
                    case "pdf": "doc.plaintext"
                    case "zip": "doc.zipper"
                    default:
                        "doc"
                }
            default:
                "doc"
        }
    }
}

extension String {
    func toDownloadDataType() -> DownloadDataMIME {
        
        let compents = components(separatedBy: "/")
        
        guard compents.count == 2 else {
            return DownloadDataMIME.other(self)
        }
        //
        return switch compents.first! {
            case "text": .text(compents.last!)
            case "image": .image(compents.last!)
            case "audio": .audio(compents.last!)
            case "video": .video(compents.last!)
            case "application": .application(compents.last!)
            default :
                    .other(self)
        }
    }
}

struct DownloadItem: Identifiable {
    let id: String
    let mime: DownloadDataMIME
    let title: String
    let date: String
    let size: String
}

struct DownloadItemView: View {
    let data: DownloadItem
    var body: some View {
        HStack {
            Circle()
                .foregroundColor(.blue.opacity(0.3))
                .frame(maxWidth: 40)
                .overlay {
                    Image(systemName: data.mime.sfIcon)
                }
                .padding()
            VStack(alignment: .leading) {
                Text(data.title)
                    .font(.system(size: 18, weight: .semibold))
                    .padding(.bottom, 5)
                HStack {
                    Text(data.date)
                        .foregroundStyle(.gray)
                    Text(data.size)
                        .foregroundStyle(.gray)
                }
            }
        }
    }
}


#Preview(body: {
    DownloadItemView(data: DownloadItem(id: "",
                                        mime: .text("html"),
                                        title: "Hello.txt",
                                        date: "100000000",
                                        size: "100000"))
})
