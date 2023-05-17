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
    
    @Binding var selectedCellFrame: CGRect
    
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
                                    .preference(key: CellFramePreferenceKey.self, value: [ CellFrameInfo( index:webStores.firstIndex(of: webstore)!, frame: geometry.frame(in: .global))])
                            })
                            .onTapGesture {
                                if let index = webStores.firstIndex(of: webstore){
                                    print("tapped the \(index)th cell", "frame is \(cellFrame(at: index))")
                                }else{
                                    print("can't read the clicked cell information")
                                }
                            }
                            .onAppear{
                                print("show the \(String(describing: webStores.firstIndex(of: webstore)))th cell")
                            }
                    }
                })
                .padding(15)
                .onPreferenceChange(CellFramePreferenceKey.self) { newFrames in
                    if brower.showingOptions{
                        self.frames = newFrames
                        selectedCellFrame = newFrames[brower.selectedTabIndex].frame
                    }
                }
            }
            .background(Color(white: 0.7))
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
    @ObservedObject var webstore: WebViewStore
    @State var runCount = 0
    
    @State private var iconUrl: URL?

    var body: some View {
        ZStack(alignment: .topTrailing){
            VStack(spacing: 0) {
                
                Image(uiImage: .snapshot(from: webstore.webCache.snapshot!))
                    .resizable()
                    .frame(alignment: .top)
                    .cornerRadius(gridcellCornerR)
                    .clipped()
                    .onTapGesture {
                        
                    }
                HStack{
                    if iconUrl != nil{
                        KFImage.url(iconUrl)
                            .fade(duration: 0.1)
                            .onProgress { receivedSize, totalSize in print("\(receivedSize / totalSize) %") }
                            .onSuccess { result in print(result) }
                            .onFailure { error in print(error) }
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 22)
                    }
                    Text(webstore.title ?? "")
                        .fontWeight(.semibold)
                        .lineLimit(1)
                        
                }.frame(height: gridcellBottomH)
                    
            }
            .aspectRatio(2.0/3.2, contentMode: .fit)
            
            Button {
                print("delete this tab, remove data from cache")
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
            if webstore.webCache.lastVisitedUrl != nil{
                Task {
                    do {
                        let favicon = try await FaviconFinder(url: webstore.webCache.lastVisitedUrl!).downloadFavicon()
                        print("URL of Favicon: \(favicon.url)")
                        webstore.webCache.webIcon = favicon.url
                        iconUrl = favicon.url
                    } catch let error {
                        print("Error: \(error)")
                    }
                }
            }
        }
    }
}

struct TabsCollectionView_Previews: PreviewProvider {
    static var previews: some View {
        WebPreViewGrid(selectedCellFrame: .constant(.zero))
            .frame(height: 754)
    }
}
