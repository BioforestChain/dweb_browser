//
//  TabHStackView.swift
//  DwebBrowser
//
//  Created by ui06 on 4/27/23.
//

import SwiftUI
import WebKit

struct TabsContainerView: View {
    @EnvironmentObject var selectedTab: SelectedTab
    @EnvironmentObject var toolbarState: ToolBarState
    @EnvironmentObject var addressBar: AddressBarState

    @StateObject var gridState = TabGridState()
    @StateObject var animation = ShiftAnimation()

    @State var geoRect: CGRect = .zero // 定义一个变量来存储geoInGlobal的值
    @State var selectedCellFrame: CGRect = .zero
    @State private var isExpanded = false
    @State private var lastProgress: AnimationProgress = .invisible
    @State var showTabPage: Bool = false

    private var snapshotHeight: CGFloat { geoRect.height - addressBarH }
    private var snapshotMidY: CGFloat { geoRect.minY + snapshotHeight/2 - addressBarH }

    @Namespace private var expandshrinkAnimation
    private let animationId = "expandshrinkAnimation"

    init() {
        print("visiting TabsContainerView init")
    }

    var body: some View {
        Self._printChanges()

        return
            GeometryReader { geo in
                // 层级关系  最前<-- 快照(缩放动画）<-- collecitionview  <--  tabPage ( homepage & webview)

                ZStack {
                    TabGridView(animation: animation, gridState: gridState, selectedCellFrame: $selectedCellFrame)

                    if isExpanded, !animation.progress.isAnimating() {
                        Color.bkColor.ignoresSafeArea()
                    }

                    PagingScrollView(showTabPage: $showTabPage)
                        .environmentObject(animation)
                        .allowsHitTesting(showTabPage) // This line allows TabGridView to receive the tap event, down through click

                    if animation.progress.isAnimating() {
                        if isExpanded {
                            animationImage
                                .transition(.identityHack)
                                .matchedGeometryEffect(id: animationId, in: expandshrinkAnimation)
                                .frame(width: screen_width, height: snapshotHeight, alignment: .top)
                                .position(x: screen_width/2, y: snapshotMidY)
                        } else {
                            animationImage
                                .transition(.identityHack)
                                .matchedGeometryEffect(id: animationId, in: expandshrinkAnimation)
                                .frame(width: gridCellW, height: cellImageH, alignment: .top)
                                .position(x: selectedCellFrame.minX + selectedCellFrame.width/2.0,
                                          y: selectedCellFrame.minY + (selectedCellFrame.height - gridcellBottomH)/2.0 - safeAreaTopHeight)
                        }
                    }
                }
                .background(Color.bkColor)

                .onAppear {
                    geoRect = geo.frame(in: .global)
                    print("z geo: \(geoRect)")
                }

                .onReceive(toolbarState.$createTabTapped) { createTabTapped in
                    if createTabTapped { // 准备放大动画
                        WebCacheMgr.shared.createOne()
                        selectedTab.curIndex = WebCacheMgr.shared.store.count - 1
                        selectedCellFrame = CGRect(origin: CGPoint(x: screen_width/2, y: screen_height/2), size: CGSize(width: 5, height: 5))
                        toolbarState.shouldExpand = true
                    }
                }

                .onChange(of: selectedCellFrame) { newValue in
                    printWithDate(msg: "selecte cell rect changes to : \(newValue)")
                }
            }
    }

    var animationImage: some View {
        Rectangle()
            .overlay(
                Image(uiImage: animation.snapshotImage)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(alignment: .top)
            )
            .cornerRadius(isExpanded ? 0 : gridcellCornerR)
            .clipped()
            .animation(.default, value: isExpanded)
            .onReceive(animation.$progress, perform: { progress in
                guard progress != lastProgress else {
                    return
                }

                lastProgress = progress

                if progress == .startShrinking || progress == .startExpanding {
                    let isExpanding = animation.progress == .startExpanding
                    printWithDate(msg: "animation : \(progress)")
                    if progress == .startShrinking {
                        gridState.scale = 0.8
                    }

                    printWithDate(msg: "start to shifting animation")
                    withAnimation(.easeOut(duration: 0.3)) {
                        addressBar.shouldDisplay = isExpanding
                        gridState.scale = isExpanding ? 0.8 : 1
                        isExpanded = isExpanding
                    }

                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                        showTabPage = isExpanded
                        animation.progress = .invisible // change to expanded or shrinked
                        gridState.scale = 1
                    }
                }
            })
    }
}

// The image in transition changes to fade out/fade in, make sure the image to stay solid and not transparent in the animation
extension AnyTransition {
    static var identityHack: AnyTransition {
        .asymmetric(insertion: .identity, removal: .identity)
    }
}
