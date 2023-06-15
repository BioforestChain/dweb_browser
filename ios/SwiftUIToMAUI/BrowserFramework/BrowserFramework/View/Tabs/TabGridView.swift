//
//  TabsCollectionView.swift
//  DwebBrowser
//
//  Created by ui06 on 4/25/23.
//

import SwiftUI
import Kingfisher
import FaviconFinder
import Combine

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

struct TabGridView: View {
    @EnvironmentObject var browser: BrowerVM
    @EnvironmentObject var addrBarOffset: AddrBarOffset
    @EnvironmentObject var tabState: TabState
    
    @ObservedObject var cacheStore = WebCacheMgr.shared

    @State var frames: [CellFrameInfo] = []
    
    @Binding var cellFrames: [CGRect]
  
    var body: some View {
        
        GeometryReader { geo in
            ScrollViewReader{ scrollproxy in
                ScrollView{
                    LazyVGrid(columns: [
                        GridItem(.adaptive(minimum: (screen_width/3.0 + 1),maximum: screen_width/2.0),spacing: 15)
                    ],spacing: 20,content: {
                        ForEach(cacheStore.store, id: \.id) { webCache in
                            GridCell(webCache: webCache)
                                .id(webCache.id)
                                .background(GeometryReader { geometry in
                                    Color.clear
                                        .preference(key: CellFramePreferenceKey.self, value: [ CellFrameInfo(index: cacheStore.store.firstIndex(of: webCache) ?? 0, frame: geometry.frame(in: .global))])
                                })
                                .onTapGesture {
                                    guard let index = cacheStore.store.firstIndex(of: webCache) else { return }
                                    let currentFrame = cellFrame(at: index)
                                    let geoFrame = geo.frame(in: .global)
                                    print("\(geoFrame.minY) - \(currentFrame.minY), \(geoFrame.maxY) - \(currentFrame.maxY)")
                                    if geoFrame.minY <= currentFrame.minY, geoFrame.maxY >= currentFrame.maxY{
                                        print("inside of grid")

                                        browser.selectedTabIndex = index
                                        addrBarOffset.onX = -CGFloat (index) * screen_width
                                        tabState.showTabGrid = false
                                    }else{
                                        print("outside of grid")
                                        browser.selectedTabIndex = index
                                        addrBarOffset.onX = -CGFloat (index) * screen_width

                                        withAnimation(.easeInOut(duration: 0.3),{
                                            scrollproxy.scrollTo(webCache.id)
                                        })
                                        DispatchQueue.main.asyncAfter(deadline: .now()+0.4, execute: {
                                            tabState.showTabGrid = false
                                        })
                                    }
                                }
                                .shadow(color: Color.gray, radius: 10)
                        }
                    })
                    .padding(15)
                    .onPreferenceChange(CellFramePreferenceKey.self) { newFrames in
                        if tabState.showTabGrid{
                            self.frames = newFrames
                            cellFrames = newFrames.map{ $0.frame }
                        }
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

    @State var runCount = 0
    @ObservedObject var webCache: WebCache
    var body: some View {
        ZStack(alignment: .topTrailing){
            VStack(spacing: 0) {
                Image(uiImage:  .snapshotImage(from: webCache.snapshotUrl))
                    .resizable()
                    .frame(alignment: .top)
                    .cornerRadius(gridcellCornerR)
                    .clipped()
                HStack{
                    webIconImage
                        .onAppear{
                            fetchIconUrl()
                        }
                    Text(webCache.title)
                        .fontWeight(.semibold)
                        .lineLimit(1)
                }.frame(height: gridcellBottomH)
                
            }
            .aspectRatio(2.0/3.2, contentMode: .fit)
            deleteButton
        }
    }
    
    func fetchIconUrl(){
        URL.downloadWebsiteIcon(iconUrl: webCache.lastVisitedUrl) { url in
            print("URL of Favicon: \(url)")
            DispatchQueue.main.async {
                webCache.webIconUrl = url
            }
        }
    }

    var deleteButton: some View{
            Button {
                print("delete this tab, remove data from cache")
                WebCacheMgr.shared.remove(webCache: webCache)
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
    
    var webIconImage: some View{
        KFImage.url(webCache.webIconUrl)
            .fade(duration: 0.1)
            .onProgress { receivedSize, totalSize in print("dowloading icon right now \(receivedSize / totalSize) %") }
            .onSuccess { result in print("dowload icon done \(result)") }
            .onFailure { error in print("dowload icon failed \(error)") }
            .resizable()
            .aspectRatio(contentMode: .fit)
            .frame(width: 22)
    }
}


