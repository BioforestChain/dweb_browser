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

struct WebPreViewGrid: View {
    @EnvironmentObject var browser: BrowerVM
    @EnvironmentObject var addressbarOffset: AddressBarOffsetOnX
    @EnvironmentObject var tabState: TabState
    
    @ObservedObject var wrapperStore = WebWrapperManager.shared
    @ObservedObject var cacheStore = WebCacheStore.shared

    @State var frames: [CellFrameInfo] = []
    
    @Binding var cellFrames: [CGRect]
  
    var body: some View {
        
        GeometryReader { geo in
            ScrollViewReader{ scrollproxy in
                ScrollView{
                    LazyVGrid(columns: [
                        GridItem(.adaptive(minimum: (screen_width/3.0 + 1),maximum: screen_width/2.0),spacing: 15)
                    ],spacing: 20,content: {
                        ForEach(cacheStore.store, id: \.id) { webcache in

                            GridCell(webcache: webcache)
                                .background(GeometryReader { geometry in
                                    Color.clear
                                        .preference(key: CellFramePreferenceKey.self, value: [ CellFrameInfo(index: cacheStore.store.firstIndex(of: webcache) ?? 0, frame: geometry.frame(in: .global))])
                                })
                                .id(cacheStore.store.firstIndex(of: webcache)!)
                                .onTapGesture {
                                    guard let index = cacheStore.store.firstIndex(of: webcache) else { return }
                                    let currentFrame = cellFrame(at: index)
                                    let geoFrame = geo.frame(in: .global)
                                    print("\(geoFrame.minY) - \(currentFrame.minY), \(geoFrame.maxY) - \(currentFrame.maxY)")
                                    if geoFrame.minY <= currentFrame.minY, geoFrame.maxY >= currentFrame.maxY{
                                        print("inside of grid")

                                        browser.selectedTabIndex = index
                                        tabState.showingOptions = false
                                    }else{
                                        print("outside of grid")
                                        browser.selectedTabIndex = index
                                        addressbarOffset.offset = -CGFloat (index) * screen_width

                                        withAnimation(.easeInOut(duration: 0.3),{
                                            scrollproxy.scrollTo(index)
                                        })
                                        DispatchQueue.main.asyncAfter(deadline: .now()+0.4, execute: {
                                            tabState.showingOptions = false
                                        })
                                    }
                                }
                                .shadow(color: Color.gray, radius: 10)
                        }
                    })
                    .padding(15)
                    .onPreferenceChange(CellFramePreferenceKey.self) { newFrames in
                        if tabState.showingOptions{
                            self.frames = newFrames
                            cellFrames = newFrames.map{ $0.frame }
                        }
                    }
                    .onAppear{
                        print("gridAppeartimes: \(gridAppeartimes += 1)")
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
var cellAppeartimes = 0
var gridAppeartimes = 0

struct GridCell: View {
//    @EnvironmentObject var browser: BrowerVM
    @State var runCount = 0
    @ObservedObject var webcache: WebCache
//    @ObservedObject var webWrapper: WebWrapper
    @State var iconUrl = URL.defaultWebIconURL
    var body: some View {
        //        GeometryReader{ geo in
        
        ZStack(alignment: .topTrailing){
            VStack(spacing: 0) {
                Image(uiImage:  .snapshotImage(from: webcache.snapshotUrl))
                    .resizable()
                    .frame(alignment: .top)
                    .cornerRadius(gridcellCornerR)
                    .clipped()
                HStack{
                    webIconImage
                        .onAppear{
                            print("webIconImage onAppear")

                        }
                        .onReceive(Just(webcache.lastVisitedUrl), perform: { url in
//                            updateIcon()
                        })
                    Text(webcache.title ?? "")
                        .fontWeight(.semibold)
                        .lineLimit(1)
                        .onAppear{
                            
                            print("Text onAppear")
                            
                        }
                    
                }.frame(height: gridcellBottomH)
                
            }
            .aspectRatio(2.0/3.2, contentMode: .fit)
            
            deleteButton
        }
        .onAppear{
            print("cellAppeartimes: \(cellAppeartimes += 1)")
        }
//        .onAppear{
//            if webWrapper.webCache.webIconUrl.scheme == "file"{
//                URL.downloadWebsiteIcon(iconUrl: webWrapper.webCache.lastVisitedUrl) { url in
//                    print("URL of Favicon: \(url)")
//                    webWrapper.webCache.webIconUrl = url
//                    iconUrl = url
//                }
//            }
//        }
    }
    
    func updateIcon(){
        URL.downloadWebsiteIcon(iconUrl: webcache.lastVisitedUrl) { url in
            print("URL of Favicon: \(url)")
            webcache.webIconUrl = url
            iconUrl = url
        }
    }
    
    var deleteButton: some View{
        Button {
            print("delete this tab, remove data from cache")
            WebCacheStore.shared.remove(webCache: webcache)
    
//            browser.remove(webWrapper: webWrapper)
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
        KFImage.url(iconUrl)
            .fade(duration: 0.1)
            .onProgress { receivedSize, totalSize in print("dowloading icon right now \(receivedSize / totalSize) %") }
            .onSuccess { result in print("dowload icon done \(result)") }
            .onFailure { error in print("dowload icon failed \(error)") }
            .resizable()
            .aspectRatio(contentMode: .fit)
            .frame(width: 22)
    }
}

struct TabsCollectionView_Previews: PreviewProvider {
    static var previews: some View {
        Text("")
//        WebPreViewGrid(cellFrames: .constant([.zero]))
//            .frame(height: 754)
    }
}
