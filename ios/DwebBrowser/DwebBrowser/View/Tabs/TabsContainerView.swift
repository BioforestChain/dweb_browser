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
    
    @StateObject var animation = ShiftAnimation()
    @State var selectedCellFrame: CGRect = .zero
    
    @StateObject var gridState = TabGridState()
    
    @State var gridScale: CGFloat = 1
    
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
                    WebHScrollView(animation: animation)
                        .environmentObject(animation)
                }
                
                if animation.progress.isAnimating() {
                    if isExpand {
                        profileView
                            .transition(.identityHack)
                            .matchedGeometryEffect(id: animationId, in: expandshrinkAnimation)
                            .frame(width: geoRect.width, height: geoRect.height, alignment: .top)
                            .position(x: geoRect.midX, y: geoRect.midY - geoRect.minY)
                    } else {
                        profileView
                            .transition(.identityHack)

                            .matchedGeometryEffect(id: animationId, in: expandshrinkAnimation)
                            .frame(width: gridCellW, height: cellImageH, alignment: .top)
                            .position(x: selectedCellFrame.minX + selectedCellFrame.width/2.0,
                                      y: selectedCellFrame.minY + (selectedCellFrame.height - gridcellBottomH)/2.0 - safeAreaTopHeight)
                    }
                }
            }
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
    
    var profileView: some View {
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

struct WebHScrollView: View {
    @EnvironmentObject var addrBarOffset: AddrBarOffset
    @EnvironmentObject var toolbarState: ToolBarState
    @ObservedObject var animation: ShiftAnimation
    
    var body: some View {
        GeometryReader { _ in
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: 0) {
                    ForEach(WebCacheMgr.shared.store, id: \.id) { webCache in
                        TabPageView(webCache: webCache, webWrapper: WebWrapperMgr.shared.webWrapper(of: webCache.id), toolbarState: toolbarState, animation: animation)
                            .id(webCache.id)
                            .frame(width: screen_width)
                    }
                }
                .offset(x: addrBarOffset.onX)
            }
            .scrollDisabled(true)
        }
    }
}

struct MatchGeometryView: View {
    @State private var showExpand = false
    
    @Namespace private var expandshrinkAnimation
    private let animationId = "expandshrinkAnimation"
    
    var body: some View {
        VStack {
            if showExpand {
                expandedView
            } else {
                shrinkedView
            }
            Spacer()
                .frame(height: 100)
            
            Text("Line 111111111111111")
            Text("Line 22222222222222")
            Text("Line 3333333333")
        }
    }
    
    var profileView: some View {
        Image(uiImage: .bundleImage(name: "snapshot"))
            .resizable()
            .aspectRatio(contentMode: .fill)
            .clipShape(Circle())
            .onTapGesture {
                withAnimation {
                    showExpand.toggle()
                }
            }
    }
    
    var expandedView: some View {
        VStack {
            profileView
                .matchedGeometryEffect(id: animationId, in: expandshrinkAnimation)
                .frame(width: 80, height: 80)
            Text("Jason White")
            Text("Wood worker")
        }
    }
    
    var shrinkedView: some View {
        VStack(alignment: .leading) {
            HStack {
                profileView
                    .matchedGeometryEffect(id: animationId, in: expandshrinkAnimation)
                    .frame(width: 400, height: 400)
                VStack {
                    Text("Jason White")
                    Text("Wood worker")
                }
            }
        }
    }
}

struct TabHStackView_Previews: PreviewProvider {
    static var previews: some View {
        MatchGeometryView()
    }
}
