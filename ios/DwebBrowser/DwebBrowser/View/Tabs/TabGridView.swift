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
    @State var testIndex = 0

    @Binding var selectedCellFrame: CGRect

    let detector = CurrentValueSubject<[CellFrameInfo], Never>([])
    var publisher: AnyPublisher<[CellFrameInfo], Never> {
        detector
            .debounce(for: .seconds(0.05), scheduler: DispatchQueue.main)
//            .dropFirst()
            .eraseToAnyPublisher()
    }

    var body: some View {
        GeometryReader { geo in
            ScrollViewReader { scrollproxy in
                ScrollView {
                    LazyVGrid(columns: [
                        GridItem(.adaptive(minimum: screen_width/3.0, maximum: screen_width/2.0), spacing: gridHSpace)
                    ], spacing: gridVSpace, content: {
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
                                        selectedCellFrame = cellFrame(at: index)
                                    }
                                }

                                .onTapGesture {
                                    guard let index = cacheStore.store.firstIndex(of: webCache) else { return }
                                    let currentFrame = cellFrame(at: index)
                                    let geoFrame = geo.frame(in: .global)
                                    print("\(geoFrame.minY) - \(currentFrame.minY), \(geoFrame.maxY) - \(currentFrame.maxY)")
                                    if geoFrame.minY <= currentFrame.minY, geoFrame.maxY >= currentFrame.maxY {
                                        print("inside of grid")
                                        if selectedTab.curIndex != index {
                                            selectedTab.curIndex = index
                                        }
                                        selectedCellFrame = currentFrame
                                        withAnimation {
                                            toolbarState.shouldExpand = true
                                        }
                                    } else {
                                        printWithDate(msg: "outside of grid")
                                        if selectedTab.curIndex != index {
                                            selectedTab.curIndex = index
                                        }
                                        withAnimation(.linear(duration: 0.1)) {
                                            printWithDate(msg: "star scroll tp adjust")
                                            var anchor = UnitPoint.center
                                            if selectedTab.curIndex < 4 {
                                                anchor = .top
                                            } else if selectedTab.curIndex >= cacheStore.store.count - 2 {
                                                anchor = .bottom
                                            }
                                            scrollproxy.scrollTo(webCache.id, anchor: anchor)
                                        }
                                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                                            printWithDate(msg: "get cell frame for expanding at index:\(index)")
                                            selectedCellFrame = cellFrame(at: index)

                                            withAnimation {
                                                toolbarState.shouldExpand = true
                                            }
                                        }
                                    }
                                }
                                .shadow(color: Color.gray, radius: 6)
                        }
                    })
                    .padding(gridHSpace)
                    .scaleEffect(x: gridState.scale, y: gridState.scale)

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
                    printWithDate(msg: "end scrolling and record cell frames : \($0)")
                }
                .onChange(of: toolbarState.shouldExpand) { shouldExpand in
                    if !shouldExpand {
                        // 设置grid为不可见的状态，并滚动到对应的位置, 为了获取selectedCellframe
                        let index = selectedTab.curIndex
                        let webCache = cacheStore.store[index]

                        let currentFrame = cellFrame(at: index)
                        let geoFrame = geo.frame(in: .global)
                        print("\(geoFrame.minY) - \(currentFrame.minY), \(geoFrame.maxY) - \(currentFrame.maxY)")

                        let needScroll = !(geoFrame.minY <= currentFrame.minY && geoFrame.maxY >= currentFrame.maxY)
                        let waitDuration = needScroll ? 0.4 : 0

                        if selectedTab.curIndex != index {
                            selectedTab.curIndex = index
                        }

                        if needScroll {
                            printWithDate(msg: "star scroll tp adjust")
                            var anchor = UnitPoint.center
                            if selectedTab.curIndex < 4 {
                                anchor = .top
                            } else if selectedTab.curIndex >= cacheStore.store.count - 2 {
                                anchor = .bottom
                            }

                            withAnimation(.linear(duration: 0.1)) {
                                scrollproxy.scrollTo(webCache.id, anchor: anchor)
                            }
                        }

                        DispatchQueue.main.asyncAfter(deadline: .now() + waitDuration) {
                            selectedCellFrame = cellFrame(at: index)
                            printWithDate(msg: "obtained Cell Frame")

                            if animation.progress == .obtainedSnapshot {
                                animation.progress = .startShrinking

                            } else {
                                animation.progress = .obtainedCellFrame
                            }
                        }
                    }
                }
            }
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
