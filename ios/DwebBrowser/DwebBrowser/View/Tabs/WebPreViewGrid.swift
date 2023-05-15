//
//  TabsCollectionView.swift
//  DwebBrowser
//
//  Created by ui06 on 4/25/23.
//

import SwiftUI
import Kingfisher

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
    
    var caches: [WebCache] {
        brower.pages.map { $0.webStore.web}
    }
    
    var body: some View {
        GeometryReader { geo in
            ScrollView{
                LazyVGrid(columns: [
                    GridItem(.adaptive(minimum: (screen_width/3.0 + 1),maximum: screen_width/2.0),spacing: 15)
                ],spacing: 20,content: {
                    ForEach(caches, id: \.self) {cache in
                        GridCell(cache: cache)
                            .background(GeometryReader { geometry in
                                Color.clear
                                    .preference(key: CellFramePreferenceKey.self, value: [ CellFrameInfo( index:caches.firstIndex(of: cache)!, frame: geometry.frame(in: .global))])
                            })
                            .onTapGesture {
                                if let index = caches.firstIndex(of: cache){
                                    print("tapped the \(index)th cell", "frame is \(cellFrame(at: index))")
                                }else{
                                    print("can't read the clicked cell information")
                                }
                                
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
    var cache: WebCache
    @State var runCount = 0
    var body: some View {
        ZStack(alignment: .topTrailing){
            VStack(spacing: 0) {
                
                Image(uiImage: snapshotImage(url: cache.snapshot!))
                    .resizable()
                    .shadow(color: .secondary, radius: 3)
                    .cornerRadius(gridcellCornerR)
                    .onTapGesture {
                        
                    }
                HStack{
                    KFImage.url(cache.webIcon)
                        .fade(duration: 0.1)
                        .onProgress { receivedSize, totalSize in  }
                        .onSuccess { result in  }
                        .onFailure { error in }
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 25)
                    Text(cache.title)
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
        .background(.cyan)
    }
    
    func snapshotImage(url: URL)->UIImage{
        do{
            return UIImage(data: try Data(contentsOf: url))!
        }catch{
            print("")
            return UIImage(named: "snapshot")!
        }
    }
}

struct TabsCollectionView_Previews: PreviewProvider {
    static var previews: some View {
        WebPreViewGrid(selectedCellFrame: .constant(.zero))
            .frame(height: 754)
    }
}
