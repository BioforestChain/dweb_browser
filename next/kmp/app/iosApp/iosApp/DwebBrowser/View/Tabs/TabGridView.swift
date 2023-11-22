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
    @EnvironmentObject var webcacheStore: WebCacheStore

    @ObservedObject var animation: ShiftAnimation
    @ObservedObject var gridState: TabGridState

    @State var isFirstRecord: Bool = true
    @State var frames: [CellFrameInfo] = []

    @Binding var selectedCellFrame: CGRect

    @StateObject var deleteCache = DeleteCache()

    let detector = CurrentValueSubject<[CellFrameInfo], Never>([])
    var publisher: AnyPublisher<[CellFrameInfo], Never> {
        detector
            .debounce(for: .seconds(0.06), scheduler: DispatchQueue.main)
            .eraseToAnyPublisher()
    }

    var body: some View {
        GeometryReader { geo in
            ScrollViewReader { scrollproxy in
                ScrollView {
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: gridVSpace) {
                        ForEach(webcacheStore.caches, id: \.id) { webCache in
                            GridCell(webCache: webCache, isSelected: isSelected(webCache: webCache))
                                .frame(maxWidth: .infinity, maxHeight: .infinity) // 使用最大的宽度和高度
                                .aspectRatio(cellWHratio, contentMode: .fit)
                                .id(webCache.id)
                                .background(GeometryReader { geometry in
                                    Color
                                        .clear
                                        .preference(key: CellFramePreferenceKey.self,
                                                    value: [CellFrameInfo(index: webcacheStore.index(of: webCache) ?? 0, 
                                                                          frame: geometry.frame(in: .named("ScrollView")))])
                                })

                                .onTapGesture {
                                    guard let tapIndex = webcacheStore.index(of: webCache) else { return }
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
                        .shadow(color: Color.gray, radius: 2)
                    }
                    .environmentObject(deleteCache)
                    .padding(gridHSpace)
                    .scaleEffect(x: gridState.scale, y: gridState.scale)
                    .onPreferenceChange(CellFramePreferenceKey.self) { newFrames in
                        newFrames.forEach { info in
                            if let index = frames.firstIndex(where: { $0.index == info.index}) {
                                frames[index] = info
                            } else {
                                frames.append(info)
                            }
                        }

                        detector.send(newFrames)
                    }
                }
                .background(Color.bkColor)
                .coordinateSpace(name: "ScrollView")
                .onReceive(publisher) {
                    if $0.count > 0 {
                        self.frames = $0
                    }
                    Log("updating cell frames : \($0)")
                }
                .onChange(of: deleteCache.cacheId, { _, operateId in
                    guard let cache = webcacheStore.caches.filter({ $0.id == operateId }).first else { return }
                    guard let deleteIndex = webcacheStore.index(of: cache) else { return }
                    if deleteIndex <= selectedTab.curIndex {
                        if selectedTab.curIndex > 0 {
                            selectedTab.curIndex -= 1
                        }
                    }
                    selectedCellFrame = cellFrame(at: selectedTab.curIndex)
                    
                    if webcacheStore.cacheCount == 1 {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                            selectedTab.curIndex = 0
                            selectedCellFrame = CGRect(origin: CGPoint(x: screen_width/2, y: screen_height/2), size: CGSize(width: 5, height: 5))
                            toolbarState.shouldExpand = true
                        }
                    }

                    withAnimation(.easeIn) {
                        webcacheStore.remove(by: deleteCache.cacheId)
                    }
                })
                .onChange(of: toolbarState.shouldExpand, { _, shouldExpand in
                    if shouldExpand { // 准备放大动画
                        animation.snapshotImage = webcacheStore.cache(at: selectedTab.curIndex).snapshotImage
                        animation.progress = .startExpanding
                        if cellFrame(at: selectedTab.curIndex) != .zero {
                            selectedCellFrame = cellFrame(at: selectedTab.curIndex)
                        } else {
                            selectedCellFrame = CGRect(x: screen_width/2.0, y: screen_height/2.0, width: 5, height: 5)
                        }
                    }
                })
                .onChange(of: toolbarState.shouldExpand, { _, shouldExpand in
                    if !shouldExpand { //缩小
                        let geoFrame = geo.frame(in: .global)
                        prepareToShrink(geoFrame: geoFrame, scrollproxy: scrollproxy) {
                            if animation.progress == .obtainedSnapshot {
                                animation.progress = .startShrinking

                            } else {
                                animation.progress = .obtainedCellFrame
                            }
                        }
                    }
                })
            }
        }
    }

    func prepareToShrink(geoFrame: CGRect, scrollproxy: ScrollViewProxy, afterObtainCellFrame: @escaping () -> Void) {
        let currentFrame = cellFrame(at: selectedTab.curIndex)

        let needScroll = !(geoFrame.minY <= currentFrame.minY && geoFrame.maxY >= currentFrame.maxY)

        if needScroll {
            Log("star scroll tp adjust")

            let webCache = webcacheStore.cache(at: selectedTab.curIndex)
            withAnimation(.linear(duration: 0.1)) {
                scrollproxy.scrollTo(webCache.id, anchor: .top)
            }
        }

        let waitingDuration = needScroll ? 0.5 : 0.2 // 0.5是试出来的，少于这个时间滚动未完成，cell的位置不正确. 0.2是因为tabpage在某种情况下会收到两次shouldExpand的onchange事件，0.2是为了等第二次截图完成
        DispatchQueue.main.asyncAfter(deadline: .now() + waitingDuration) {
            selectedCellFrame = cellFrame(at: selectedTab.curIndex)
            Log("cell at \(selectedTab.curIndex) frame is:\(selectedCellFrame)")
            afterObtainCellFrame()
        }
    }

    func isSelected(webCache: WebCache) -> Bool {
        webcacheStore.index(of: webCache) == selectedTab.curIndex
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
