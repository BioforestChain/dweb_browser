//
//  DownloadListView.swift
//  DwebWebBrowser
//
//  Created by instinct on 2024/4/10.
//

import Foundation
import Observation
import SwiftUI

struct DownloadListView: View {
    @Bindable var viewModel = DownloadListViewModel()
    var body: some View {
        Group {
            if viewModel.isEmpty {
                emptyView
            } else {
                listView
            }
        }
        .toolbarTitleDisplayMode(.automatic)
        .toolbarBackground(.white)
        .navigationTitle("Download")
        .task {
            viewModel.loadDatas()
        }
        .onDisappear(perform: {
            viewModel.commitEidt()
            viewModel.clear()
        })
    }
    
    var emptyView: some View {
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
    }
    
    var listView: some View {
        List(content: {
            
            let downloadingDatas = viewModel.downloadingDatas
            if downloadingDatas.count > 0 {
                Section(header: Text("下载中")) {
                    ForEach(viewModel.downloadingDatas, id: \.id) { data in
                        if data.isLoaded {
                            NavigationLink {
                                getPreview(data)
                            } label: {
                                DownloadItemView(data: data)
                            }
                        } else {
                            DownloadItemView(data: data)
                        }
                    }
                    .onDelete(perform: { indexSet in
                        viewModel.removeDownloading(indexSet)
                    })
                }
            }
            
            let downloadedDatas = viewModel.downloadedDatas
            if downloadedDatas.count > 0 {
                Section(header: Text("已下载")) {
                    ForEach(viewModel.downloadedDatas, id: \.id) { data in
                        if data.isLoaded {
                            NavigationLink {
                                getPreview(data)
                            } label: {
                                DownloadItemView(data: data)
                            }
                        } else {
                            DownloadItemView(data: data)
                        }
                    }
                    .onDelete(perform: { indexSet in
                        viewModel.removeDownloaded(indexSet)
                    })
                }
            }
        })
        .listStyle(.plain)
        .environment(viewModel)
    }
    
    @ViewBuilder
    func getPreview(_ item: DownloadItem) -> some View {
//        DownloadTextPreviewView(data: item)
        // TODO: 预览
        Text("TODO: Preview")
    }
}


struct DownloadItemView: View {
    @StateObject var data: DownloadItem
    @Environment(DownloadListViewModel.self) var viewModel
    var body: some View {
        HStack {
            let _ = Self._printChanges()
            leftView
                .frame(maxWidth: 40)
                .padding(10)
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
    
    @ViewBuilder
    var leftView: some View {
        if case let .loading(progress) = data.state {
            DwebCircleProgressView(pauseAction: {
                viewModel.pause(item: data)
            }, pause: data.pause, progress: progress)
            .background(content: {
                Image(systemName: data.mime.sfIcon)
                    .font(.system(size: 20))
            })
        } else if case let .pause(progress) = data.state {
            DwebCircleProgressView(pauseAction: {
                viewModel.resume(item: data)
            }, pause: data.pause, progress: progress)
        } else {
            Circle()
                .foregroundStyle(Color.blue.opacity(0.5))
                .background(content: {
                    Image(systemName: data.mime.sfIcon)
                        .font(.system(size: 20))
                })
        }
    }
}


struct Demo: View {
    var data = DownloadItem(id: "", mime: .text("sss"), title: "Fuck", date: "2021-01", dateValue: 0, size: "11M", state: .pause(0.5))
    var body: some View {
        List {
            DownloadItemView(data: data)
        }
        .task {
            Task {
                for i in (0..<9) {
                    data.updateState(DownloadItem.State.loading(Float(i) * 0.1 + 0.1))
                    try? await Task.sleep(nanoseconds: 1_000_000_000)
                }
                
                data.updateState(DownloadItem.State.loaded(""))
            }
        }
    }
}


#Preview(body: {
    Demo()
        .environment(DownloadListViewModel())
})
