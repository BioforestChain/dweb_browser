//
//  TabsCollectionView.swift
//  DwebBrowser
//
//  Created by ui06 on 4/25/23.
//

import SwiftUI
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
    @EnvironmentObject var selectedTab: SelectedTab
    @EnvironmentObject var addrBarOffset: AddrBarOffset
    @EnvironmentObject var toolbarState: ToolBarState
    
    @ObservedObject var cacheStore = WebCacheMgr.shared

    @State var frames: [CellFrameInfo] = []
    @Binding var cellFrames: [CGRect]
    
    var body: some View {
        
        GeometryReader { geo in
            ScrollViewReader{ scrollproxy in
                ScrollView{
                    LazyVGrid(columns: [
                        GridItem(.adaptive(minimum: (screen_width/3.0),maximum: screen_width/2.0), spacing: gridHSpace)
                    ], spacing: gridVSpace, content: {
                        ForEach(cacheStore.store, id: \.id) { webCache in
                            GridCell(webCache: webCache, isSelected: isSelected(webCache: webCache))
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
                                        if selectedTab.curIndex != index{
                                            selectedTab.curIndex = index
                                        }
                                        toolbarState.showTabGrid = false
                                    }else{
                                        print("outside of grid")
                                        if selectedTab.curIndex != index{
                                            selectedTab.curIndex = index
                                        }
                                        withAnimation(.easeInOut(duration: 0.3),{
                                            scrollproxy.scrollTo(webCache.id)
                                        })
                                        DispatchQueue.main.asyncAfter(deadline: .now()+0.4, execute: {
                                            toolbarState.showTabGrid = false
                                        })
                                    }
                                }
                                .shadow(color: Color.gray, radius: 6)
                        }
                    })
                    .padding(gridHSpace)
                    .onPreferenceChange(CellFramePreferenceKey.self) { newFrames in
                        if toolbarState.showTabGrid{
                            self.frames = newFrames
                            cellFrames = newFrames.map{ $0.frame }
                        }
                    }
                }
            }
        }
    }
    func isSelected(webCache: WebCache) -> Bool{
        cacheStore.store.firstIndex(of: webCache) == selectedTab.curIndex
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


struct TabsCollectionView_Previews: PreviewProvider {
    static var previews: some View {
        Text("")
//        WebPreViewGrid(cellFrames: .constant([.zero]))
//            .frame(height: 754)
    }
}
