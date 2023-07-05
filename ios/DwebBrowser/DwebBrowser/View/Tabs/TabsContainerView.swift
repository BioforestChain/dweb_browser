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
    
    @State var geoRect: CGRect = .zero // 定义一个变量来存储geoInGlobal的值
    @State var selectedCellFrame: CGRect = .zero
    @State var gridScale: CGFloat = 1

    @StateObject var gridState = TabGridState()
    @StateObject var animation = ShiftAnimation()

    init() {
        print("visiting TabsContainerView init")
    }
    
    @State private var isExpand = false
    
    var body: some View {
        GeometryReader { geo in
            // 层级关系  最前<-- 快照(缩放动画）<-- collecitionview  <--  tabPage ( homepage & webview)
            
            ZStack {
                TabGridView(animation: animation, gridState: gridState, selectedCellFrame: $selectedCellFrame)
                    .scaleEffect(x: gridState.scale, y: gridState.scale)
                    .opacity(gridState.opacity)

                if !toolbarState.showTabGrid, !animation.progress.isAnimating() {
                    Color(.white)
                }
                if !toolbarState.showTabGrid, !animation.progress.isAnimating() {
                    PagingScrollView()
                        .environmentObject(animation)
                }
                
                if animation.progress.isAnimating() {
                    if isExpand {
                        animationImage
                            .transition(.identityHack)
                            .matchedGeometryEffect(id: animationId, in: expandshrinkAnimation)
                            .frame(width: geoRect.width, height: geoRect.height, alignment: .top)
                            .position(x: geoRect.midX, y: geoRect.midY - geoRect.minY)
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
            .onChange(of: toolbarState.showTabGrid, perform: { shouldShowGrid in
                if shouldShowGrid {
                    animation.progress = .initial
                } else {
                    animation.snapshotImage = UIImage.snapshotImage(from: WebCacheMgr.shared.store[selectedTab.curIndex].snapshotUrl)
                    animation.progress = .startExpanding
                }
                
                withAnimation(.linear) {
                    gridScale = shouldShowGrid ? 1 : 0.8
                }
            })
        }
    }
    
    @Namespace private var expandshrinkAnimation
    private let animationId = "expandshrinkAnimation"
    
    var animationImage: some View {
        Rectangle()
            .overlay(
                Image(uiImage: animation.snapshotImage)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(alignment: .top)
            )
            .cornerRadius(gridcellCornerR)

            .clipped()
            .onReceive(animation.$progress, perform: { progress in
                if progress == .startShrinking || progress == .startExpanding {
                    withAnimation(.easeInOut(duration: 0.8)) {
                        isExpand = animation.progress == .startExpanding
                    }
                    withAnimation(.linear(duration: 0.8)) {
                        gridState.opacity = 1
                        gridState.scale = progress == .startShrinking ? 1 : 0.8
                    }
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.85) {
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

