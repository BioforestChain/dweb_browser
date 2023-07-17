//
//  TabsCollectionView.swift
//  DwebBrowser
//
//  Created by ui06 on 4/25/23.
//

import Combine
import SwiftUI

struct CellFrameInfo: Equatable {
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
    @EnvironmentObject var toolbarState: ToolBarState
    
    @ObservedObject var cacheStore = WebCacheMgr.shared
    @ObservedObject var animation: ShiftAnimation
    @ObservedObject var gridState: TabGridState
    
    @State var isFirstRecord: Bool = true
    
    @State var frames: [CellFrameInfo] = []
    @Binding var isScrolling: Bool

    @Binding var selectedCellFrame: CGRect

    @StateObject var deleteCache = DeleteCache()

    let detector = CurrentValueSubject<[CellFrameInfo], Never>([])
    var publisher: AnyPublisher<[CellFrameInfo], Never> {
        detector
            .debounce(for: .seconds(0.06), scheduler: DispatchQueue.main)
//                        .dropFirst()
            .eraseToAnyPublisher()
    }
    
    var body: some View {
        GeometryReader { geo in
            ScrollViewReader { scrollproxy in
                ScrollView {
                    LazyVGrid(columns: [
                        GridItem(.adaptive(minimum: screen_width/3.0, maximum: screen_width/2.0), spacing: gridHSpace),
                    ], spacing: gridVSpace) {
                        ForEach(cacheStore.store, id: \.id) { webCache in
                            GridCell(webCache: webCache, isSelected: isSelected(webCache: webCache))
                                .id(webCache.id)
                                .background(GeometryReader { geometry in
                                    Color.clear
                                        .preference(key: CellFramePreferenceKey.self,
                                                    value: [CellFrameInfo(index: cacheStore.store.firstIndex(of: webCache) ?? 0, frame: geometry.frame(in: .global))])
                                })
                                .onAppear {
                                    if let index = WebCacheMgr.shared.store.firstIndex(of: webCache),
                                       index == selectedTab.curIndex
                                    {
//                                        selectedCellFrame = cellFrame(at: index)
//                                        printWithDate(msg: "cell appears at frame: \(selectedCellFrame)")
                                    }
                                }
                            
                                .onTapGesture {
                                    guard let tapIndex = cacheStore.store.firstIndex(of: webCache) else { return }
                                    let geoFrame = geo.frame(in: .global)
                                    if selectedTab.curIndex != tapIndex {
                                        selectedTab.curIndex = tapIndex
                                    }
                                    prepareToShrink(geoFrame: geoFrame, scrollproxy: scrollproxy) {
                                        withAnimation {
                                            toolbarState.shouldExpand = true
                                        }
                                    }
                                }
                        }
                        .shadow(color: Color.gray, radius: 6)
                    }
                    .environmentObject(deleteCache)
                    .padding(gridHSpace)
                    .scaleEffect(x: gridState.scale, y: gridState.scale)
//                    .ignoresSafeArea()
                    .onPreferenceChange(CellFramePreferenceKey.self) { newFrames in
                        if isFirstRecord {
                            self.frames = newFrames
                            isFirstRecord = false
                        }
                        detector.send(newFrames)
                    }
                }
                .background(Color.bkColor)
                
                .onReceive(publisher) {
                    if $0.count > 0 {
                        self.frames = $0
                    }
                    isScrolling = false
                    printWithDate(msg: "end scrolling and record cell frames : \($0)")
                }
                .onChange(of: deleteCache.cacheId, perform: { cacheId in
                    if let cache = WebCacheMgr.shared.store.filter({ $0.id == cacheId }).first {
                        cacheStore.remove(webCache: cache)
                        if selectedTab.curIndex >= WebCacheMgr.shared.store.count {
                            selectedTab.curIndex = WebCacheMgr.shared.store.count - 1
                            selectedCellFrame = cellFrame(at: selectedTab.curIndex)
                        }
                    }
                })
                .onChange(of: toolbarState.shouldExpand) { shouldExpand in
                    if shouldExpand { // 准备放大动画
                        animation.snapshotImage = WebCacheMgr.shared.store[selectedTab.curIndex].snapshotImage
                        animation.progress = .startExpanding
                        if cellFrame(at: selectedTab.curIndex) != .zero {
                            selectedCellFrame = cellFrame(at: selectedTab.curIndex)
                        }else {
                            selectedCellFrame = CGRect(x: screen_width/2.0, y: screen_height/2.0, width: 5, height: 5)
                        }
                    }
                }
                .onChange(of: toolbarState.shouldExpand) { shouldExpand in
                    if !shouldExpand {
                        let geoFrame = geo.frame(in: .global)
                        prepareToShrink(geoFrame: geoFrame, scrollproxy: scrollproxy) {
                            if animation.progress == .obtainedSnapshot {
                                animation.progress = .startShrinking

                            } else {
                                animation.progress = .obtainedCellFrame
                            }
                        }
                    }
                }
                .onChange(of: isScrolling) { isScrolling in
                    printWithDate(msg: "isScrolling is \(isScrolling)")
                }
            }
        }
    }
    
    func prepareToShrink(geoFrame: CGRect, scrollproxy: ScrollViewProxy, afterObtainCellFrame: @escaping () -> Void) {
        let currentFrame = cellFrame(at: selectedTab.curIndex)
   
        let needScroll = !(geoFrame.minY <= currentFrame.minY && geoFrame.maxY >= currentFrame.maxY)

        if needScroll {
            printWithDate(msg: "star scroll tp adjust")

            let webCache = cacheStore.store[selectedTab.curIndex]
            isScrolling = true
            withAnimation(.linear(duration: 0.1)) {
                scrollproxy.scrollTo(webCache.id)
            }
        }
        
        let waitingDuration = needScroll ? 0.6 : 0
        DispatchQueue.main.asyncAfter(deadline: .now() + waitingDuration) {
            selectedCellFrame = cellFrame(at: selectedTab.curIndex)
            printWithDate(msg: "cell at \(selectedTab.curIndex) frame is:\(selectedCellFrame)")
            afterObtainCellFrame()
        }
    }
    
    func isSelected(webCache: WebCache) -> Bool {
        cacheStore.store.firstIndex(of: webCache) == selectedTab.curIndex
    }
    
    func cellFrame(at index: Int) -> CGRect {
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
