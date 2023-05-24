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
    @EnvironmentObject var browser: BrowerVM
    @EnvironmentObject var addressbarOffset: AddressBarOffsetOnX

    @State var frames: [CellFrameInfo] = []
    
    @Binding var cellFrames: [CGRect]
    
    var wrappers: [WebWrapper] {
        browser.pages.map { $0.webWrapper}
    }
    
    var body: some View {
        
        GeometryReader { geo in
            ScrollViewReader{ scrollproxy in
                ScrollView{
                    LazyVGrid(columns: [
                        GridItem(.adaptive(minimum: (screen_width/3.0 + 1),maximum: screen_width/2.0),spacing: 15)
                    ],spacing: 20,content: {
                        ForEach(wrappers.indices) { index in
                            GridCell(webWrapper: wrappers[index])
                                .background(GeometryReader { geometry in
                                    Color.clear
                                        .preference(key: CellFramePreferenceKey.self, value: [ CellFrameInfo(index: index, frame: geometry.frame(in: .global))])
                                })
                                .id(index)
                                .onTapGesture {
                                    let currentFrame = cellFrame(at: index)
                                    let geoFrame = geo.frame(in: .global)
                                    print("\(geoFrame.minY) - \(currentFrame.minY), \(geoFrame.maxY) - \(currentFrame.maxY)")
                                    if geoFrame.minY <= currentFrame.minY, geoFrame.maxY >= currentFrame.maxY{
                                        print("inside of grid")
                                        
                                            browser.selectedTabIndex = index
                                            browser.showingOptions = false
                                    }else{
                                        print("outside of grid")
                                        browser.selectedTabIndex = index
                                        addressbarOffset.offset = -CGFloat (index) * screen_width

                                        withAnimation(.easeInOut(duration: 0.3),{
                                            scrollproxy.scrollTo(index)
                                        })
                                        DispatchQueue.main.asyncAfter(deadline: .now()+0.4, execute: {
                                            browser.showingOptions = false
                                        })
                                    }
                                }
                        }
                    })
                    .padding(15)
                    .onPreferenceChange(CellFramePreferenceKey.self) { newFrames in
                        if browser.showingOptions{
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
    @EnvironmentObject var browser: BrowerVM
    @State var runCount = 0
    @ObservedObject var webWrapper: WebWrapper
    @State var iconUrl = URL.defaultWebIconURL
    var body: some View {
        //        GeometryReader{ geo in
        
        ZStack(alignment: .topTrailing){
            VStack(spacing: 0) {
                Image(uiImage:  .snapshotImage(from: webWrapper.webCache.snapshotUrl))
                    .resizable()
                    .frame(alignment: .top)
                    .cornerRadius(gridcellCornerR)
                    .clipped()
                HStack{
                    webIconImage
                    Text(webWrapper.title ?? "")
                        .fontWeight(.semibold)
                        .lineLimit(1)
                    
                }.frame(height: gridcellBottomH)
                
            }
            .aspectRatio(2.0/3.2, contentMode: .fit)
            
            deleteButton
        }
        .onAppear{
            if webWrapper.webCache.webIconUrl.scheme == "file"{
                URL.downloadWebsiteIcon(iconUrl: webWrapper.webCache.lastVisitedUrl) { url in
                    print("URL of Favicon: \(url)")
                    webWrapper.webCache.webIconUrl = url
                    iconUrl = url
                }
            }
        }
        .onChange(of: webWrapper.webCache.snapshotUrl) { newUrl in
            print("new snapshot is \(newUrl)")
            //            snapShotUrl = webWrapper.webCache.snapshotUrl
        }
        //                    }
        
    }
    
    var deleteButton: some View{
        Button {
            print("delete this tab, remove data from cache")
            if let deleteIndex = browser.pages.map({ $0.webWrapper }).firstIndex(of: webWrapper){
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
        WebPreViewGrid(cellFrames: .constant([.zero]))
            .frame(height: 754)
    }
}
