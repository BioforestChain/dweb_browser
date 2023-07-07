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

struct ViewOffsetKey: PreferenceKey {
    static var defaultValue: CGFloat = 0

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

struct TabGridView: View {
    @EnvironmentObject var selectedTab: SelectedTab
    @EnvironmentObject var toolbarState: ToolBarState

    @ObservedObject var cacheStore = WebCacheMgr.shared
    @ObservedObject var animation: ShiftAnimation
    @ObservedObject var gridState: TabGridState

    @State var needRecordFrames: Bool = true
    @State var frames: [CellFrameInfo] = []

    @Binding var selectedCellFrame: CGRect

    let detector = CurrentValueSubject<[CellFrameInfo], Never>([])
    var publisher: AnyPublisher<[CellFrameInfo], Never> {
        detector
            .debounce(for: .seconds(0.05), scheduler: DispatchQueue.main)
            .dropFirst()
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
                                        toolbarState.showTabGrid = false
                                    } else {
                                        print("outside of grid")
                                        if selectedTab.curIndex != index {
                                            selectedTab.curIndex = index
                                        }
                                        needRecordFrames = true
                                        withAnimation(.linear(duration: 0.2)) {
                                            scrollproxy.scrollTo(webCache.id, anchor: .center)
                                        }
                                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                                            needRecordFrames = false
                                            printWithDate(msg: "get cell frame for expanding")
                                            selectedCellFrame = cellFrame(at: index)
                                            toolbarState.showTabGrid = false
                                        }
                                    }
                                }
                                .shadow(color: Color.gray, radius: 6)
                        }
                    })
                    .padding(gridHSpace)
                    .onPreferenceChange(CellFramePreferenceKey.self) { newFrames in
                        if toolbarState.showTabGrid {
                            if needRecordFrames { //在动画前代码滚动需要记录
                                self.frames = newFrames
                                printWithDate(msg: "update cell frame for animation")
                            } else {    //手动拖拽，只需在滚动停止的时候记录最后的位置
                                detector.send(newFrames)
                            }
                        }
                    }
                }
                .coordinateSpace(name: "scroll")
                .onReceive(publisher) {
                    self.frames = $0
                    needRecordFrames = false

                    printWithDate(msg: "record frames at scrolling end : \($0)")
                }

                .background(Color(white: 0, opacity: 0.2))
                .onChange(of: animation.progress) { progress in
                    if progress == .preparingShrink {
                        // 设置grid为不可见的状态，并滚动到对应的位置
                        let index = selectedTab.curIndex
                        let webCache = cacheStore.store[index]
                        gridState.scale = 1
                        gridState.opacity = 0.01
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                            printWithDate(msg: "scroll grid on background")
                            needRecordFrames = true
                            scrollproxy.scrollTo(webCache.id, anchor: .center)
                        }
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) { // time for scroll to index
                            printWithDate(msg: "read cell frame for animation")
                            needRecordFrames = false

                            guard let selectedFrame = self.frames.filter({ $0.index == selectedTab.curIndex }).first?.frame else { return }
                            selectedCellFrame = selectedFrame

                            gridState.scale = 0.8
                            gridState.opacity = 1

                            animation.progress = .startShrinking
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
