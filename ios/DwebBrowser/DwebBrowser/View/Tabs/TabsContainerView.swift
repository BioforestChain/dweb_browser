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

    @StateObject var gridState = TabGridState()
    @StateObject var animation = ShiftAnimation()

    @State var geoRect: CGRect = .zero // 定义一个变量来存储geoInGlobal的值
    @State var selectedCellFrame: CGRect = .zero
    @State private var isExpanded = false
    @State private var lastProgress: AnimationProgress = .invisible

    private var snapshotHeight: CGFloat { geoRect.height - addressBarH }
    private var snapshotMidY: CGFloat { geoRect.minY + snapshotHeight/2 - addressBarH }

    @Namespace private var expandshrinkAnimation
    private let animationId = "expandshrinkAnimation"

    init() {
        print("visiting TabsContainerView init")
    }

    var body: some View {
        GeometryReader { geo in
            // 层级关系  最前<-- 快照(缩放动画）<-- collecitionview  <--  tabPage ( homepage & webview)

            ZStack {
                TabGridView(animation: animation, gridState: gridState, selectedCellFrame: $selectedCellFrame)
                    .scaleEffect(x: gridState.scale, y: gridState.scale)
                    .opacity(gridState.opacity)

                if isExpanded, !animation.progress.isAnimating() {
                    Color(.white)
                }
                if isExpanded, !animation.progress.isAnimating() {
                    PagingScrollView()
                        .environmentObject(animation)
                }

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

            .onChange(of: selectedCellFrame) { newValue in
                print("selecte cell rect changes to : \(newValue)")
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
                    printWithDate(msg: "animation : \(progress)")

                    withAnimation(.easeIn(duration: 1)) {
                        gridState.scale = progress == .startShrinking ? 1 : 0.8
                        isExpanded = animation.progress == .startExpanding
                    }

                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.05) {
                        animation.progress = .invisible // change to expanded or shrinked
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
