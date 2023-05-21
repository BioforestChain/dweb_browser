//
//  TabsCollectionView.swift
//  DwebBrowser
//
//  Created by ui06 on 4/25/23.
//

import SwiftUI
import Kingfisher
import FaviconFinder

struct CellFrameInfo: Equatable{
    var index: Int
    var frame: CGRect
}

struct CellFramePreferenceKey: PreferenceKey {
    static var defaultValue: [CellFrameInfo] = []
    
    static func reduce(value: inout [CellFrameInfo], nextValue: () -> [CellFrameInfo]) {
        value += nextValue()
    }
}

struct WebPreViewGrid: View {
    @EnvironmentObject var brower: BrowerVM
    
    @State var frames: [CellFrameInfo] = []
    
    @Binding var cellFrames: [CGRect]
    
    var webStores: [WebViewStore] {
        brower.pages.map { $0.webStore}
    }
    
    var body: some View {
        GeometryReader { geo in
            ScrollView{
                LazyVGrid(columns: [
                    GridItem(.adaptive(minimum: (screen_width/3.0 + 1),maximum: screen_width/2.0),spacing: 15)
                ],spacing: 20,content: {
                    ForEach(webStores, id: \.self) {webstore in
                        GridCell(webstore: webstore)
                            .background(GeometryReader { geometry in
                                Color.clear
                                    .preference(key: CellFramePreferenceKey.self, value: [ CellFrameInfo( index:webStores.firstIndex(of: webstore) ?? 0, frame: geometry.frame(in: .global))])
                            })
                    }
                })
                .padding(15)
                .onPreferenceChange(CellFramePreferenceKey.self) { newFrames in
                    if brower.showingOptions{
                        self.frames = newFrames
                        cellFrames = newFrames.map{ $0.frame }
                    }
                }
            }
        }
    }
    
    func cellFrame(at index: Int) -> CGRect {
        guard index >= 0 && index < frames.count else {
            return .zero
        }
        if let frame = frames.first(where: { $0.index == index })?.frame {
            return frame
        }
        return .zero
    }
}

struct GridCell: View {
    @EnvironmentObject var browser: BrowerVM
    @ObservedObject var webstore: WebViewStore
    @State var runCount = 0
    
    @State private var iconUrl = URL.defaultWebIconURL
    //    @State private var snapshotUrl = URL.defaultSnapshotURL
    
    var body: some View {
        ZStack(alignment: .topTrailing){
            VStack(spacing: 0) {
                
                Image(uiImage: .snapshotImage(from: webstore.webCache.snapshotUrl))
                    .resizable()
                    .frame(alignment: .top)
                    .cornerRadius(gridcellCornerR)
                    .clipped()
                    .onTapGesture {
                        browser.currentSnapshotImage = UIImage.snapshotImage(from: webstore.webCache.snapshotUrl)
                        
                        if let clickIndex = browser.pages.map({ $0.webStore }).firstIndex(of: webstore){
                            browser.selectedTabIndex = clickIndex
                        }
                        browser.showingOptions = false
                        
                    }
                    .onAppear{
                        print("comes to Image onAppear")
                    }
                HStack{
                    //                    if iconUrl != nil{
                    KFImage.url(iconUrl)
                        .fade(duration: 0.1)
                        .onProgress { receivedSize, totalSize in print("\(receivedSize / totalSize) %") }
                        .onSuccess { result in print(result) }
                        .onFailure { error in print(error) }
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 22)
                    //                    }
                    Text(webstore.title ?? "")
                        .fontWeight(.semibold)
                        .lineLimit(1)
                    
                }.frame(height: gridcellBottomH)
                
            }
            .aspectRatio(2.0/3.2, contentMode: .fit)
            
            Button {
                print("delete this tab, remove data from cache")
                if let deleteIndex = browser.pages.map({ $0.webStore }).firstIndex(of: webstore){
                    browser.removePage(at: deleteIndex)
                }
                
            } label: {
                Image(systemName: "xmark.circle.fill")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 26)
            }
            .padding(.top, 8)
            .padding(.trailing, 8)
            .buttonStyle(CloseTabStyle())
            .alignmentGuide(.top) { d in
                d[.top]
            }
            .alignmentGuide(.trailing) { d in
                d[.trailing]
            }
        }
        .onAppear{
            if webstore.webCache.webIconUrl.scheme == "file"{
                URL.downloadWebsiteIcon(iconUrl: webstore.webCache.lastVisitedUrl) { url in
                    print("URL of Favicon: \(url)")
                    webstore.webCache.webIconUrl = url
                    iconUrl = url
                }
            }
        }
        .onChange(of: webstore.webCache.snapshotUrl) { newUrl in
            print("new snapshot is \(newUrl)")
        }
    }
}

struct TabsCollectionView_Previews: PreviewProvider {
    static var previews: some View {
        WebPreViewGrid(cellFrames: .constant([.zero]))
            .frame(height: 754)
    }
}
