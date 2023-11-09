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
    @EnvironmentObject var webcacheStore: WebCacheStore
    @EnvironmentObject var dragScale: WndDragScale
    @EnvironmentObject var browserArea: BrowserArea

    @StateObject var gridState = TabGridState()
    @StateObject var animation = ShiftAnimation()

    @State var geoRect: CGRect = .zero // 定义一个变量来存储geoInGlobal的值
    @State var selectedCellFrame: CGRect = .zero
    @State private var isExpanded = true
    @State private var lastProgress: AnimationProgress = .invisible
    @State var showTabPage: Bool = true

    private var snapshotHeight: CGFloat { geoRect.height - dragScale.addressbarHeight }

    @Namespace private var expandshrinkAnimation
    private let animationId = "expandshrinkAnimation"

    var body: some View {
        GeometryReader { geo in
            // 层级关系  最前<-- 快照(缩放动画）<-- collecitionview  <--  tabPage ( homepage & webview)

            ZStack {
                TabGridView(animation: animation, gridState: gridState, selectedCellFrame: $selectedCellFrame)
                    .environmentObject(webcacheStore)

                if isExpanded, !animation.progress.isAnimating() {
                    Color.bkColor.ignoresSafeArea()
                }

                PagingScrollView(showTabPage: $showTabPage)
                    .environmentObject(webcacheStore)
                    .environmentObject(animation)
                    .allowsHitTesting(showTabPage) // This line allows TabGridView to receive the tap event, down through click
                if animation.progress.isAnimating() {
                    if isExpanded {
                        animationImage
                            .transition(.identityHack)
                            .matchedGeometryEffect(id: animationId, in: expandshrinkAnimation)
                            .frame(width: browserArea.frame.width,
                                   height: browserArea.frame.height - dragScale.toolbarHeight - dragScale.addressbarHeight)
                            .position(x: geo.frame(in: .global).midX,
                                      y: geo.frame(in: .global).midY - browserArea.frame.minY - dragScale.addressbarHeight/2)
                    } else {
                        animationImage
                            .transition(.identityHack)
                            .matchedGeometryEffect(id: animationId, in: expandshrinkAnimation)
                            .frame(width: selectedCellFrame.width, height: selectedCellFrame.height * cellImageHeightRatio)
                            .position(x: selectedCellFrame.midX - browserArea.frame.minX,
                                      y: selectedCellFrame.midY - browserArea.frame.minY - selectedCellFrame.height * cellTitleHeightRatio / 2)
                    }
                }
            }
            .background(Color.bkColor)

            .onAppear {
                geoRect = geo.frame(in: .global)
                print("tabs contianer rect: \(geoRect)")
            }

            .onReceive(toolbarState.$createTabTapped) { createTabTapped in
                if createTabTapped { // 准备放大动画
                    webcacheStore.createOne()
                    selectedTab.curIndex = webcacheStore.cacheCount - 1
                    selectedCellFrame = CGRect(x: geo.frame(in: .global).midX, y: geo.frame(in: .global).midY, width: 5, height: 5)
                    toolbarState.shouldExpand = true
                }
            }

            .onChange(of: selectedCellFrame) { newValue in
                printWithDate("selecte cell rect changes to : \(newValue)")
            }
            .onChange(of: animation.progress) { progress in
                if progress.isAnimating() {
                    showTabPage = false
                }
            }
        }
    }

    var animationImage: some View {
        Rectangle()
            .overlay(
                ZStack {
                    let image = animation.snapshotImage
                     
                    Image(uiImage: animation.snapshotImage)
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                }
            )
            .cornerRadius(isExpanded ? 0 : gridcellCornerR)
            .animation(.default, value: isExpanded)
            .onReceive(animation.$progress, perform: { progress in
                guard progress != lastProgress else {
                    return
                }
                printWithDate("animation turns into \(animation.progress)")
                lastProgress = progress

                if progress == .startShrinking || progress == .startExpanding {
                    let isExpanding = animation.progress == .startExpanding
                    printWithDate("animation : \(progress)")
                    if progress == .startShrinking {
                        gridState.scale = 0.8
                    }

                    printWithDate("start to shifting animation")
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
