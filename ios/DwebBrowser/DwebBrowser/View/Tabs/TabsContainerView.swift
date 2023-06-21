//
//  TabHStackView.swift
//  DwebBrowser
//
//  Created by ui06 on 4/27/23.
//

import SwiftUI
import WebKit

struct TabsContainerView: View{
    @EnvironmentObject var selectedTab: SelectedTab
    
    @EnvironmentObject var toolbarState: ToolBarState
    
    @State var geoRect: CGRect = .zero // 定义一个变量来存储geoInGlobal的值
    
    @StateObject var animation = ShiftAnimation()
    @State var selectedCellFrame: CGRect = .zero

    @StateObject var gridState = TabGridState()
    
    @State var gridScale: CGFloat = 1
    
    init(){
        print("visiting TabsContainerView init")
    }
    
    var body: some View{
        Self._printChanges()
        
        return
        GeometryReader { geo in
            //层级关系  最前<-- 快照(缩放动画）<-- collecitionview  <--  tabPage ( homepage & webview)

            ZStack{
                TabGridView(animation: animation, gridState: gridState, selectedCellFrame: $selectedCellFrame)
                    .scaleEffect(x: gridState.scale, y: gridState.scale)
                    .opacity(gridState.opacity)

                if !toolbarState.showTabGrid, !animation.progress.isAnimating() {
                    Color(.white)
                }
                if !toolbarState.showTabGrid, !animation.progress.isAnimating(){
                    WebHScrollView(animation: animation)
                        .environmentObject(animation)
                }
                if animation.progress.isAnimating(){
                    animationImage
                }
            }
            .onAppear{
                geoRect = geo.frame(in: .global)
                print("z geo: \(geoRect)")
            }
            .onChange(of: toolbarState.showTabGrid, perform: { shouldShowGrid in
                if shouldShowGrid{
                    animation.progress = .initial
                }else{
                    animation.snapshotImage = UIImage.snapshotImage(from: WebCacheMgr.shared.store[selectedTab.curIndex].snapshotUrl)
                    animation.progress = .startExpanding
                }
                
                withAnimation(.linear,{
                    gridScale = shouldShowGrid ? 1 : 0.8
                })
            })
        }
    }
    var animationImage: some View{
            #if DEBUG
            let _ = Self._printChanges()
            #endif
        
        return Rectangle()
            .overlay(
            Image(uiImage: animation.snapshotImage)
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(width: geoRect.width, height: geoRect.height, alignment: .top)
                .scaleEffect(x: imageWidthScale(), y: imageWidthScale())
                .cornerRadius(animation.progress.imageIsSmall() ? gridcellCornerR : 0)
                .clipped()
        )
        .frame(width: imageWidth(),height: imageHeight(), alignment: .top)
        .position(x: imageXcenter(),y: imageYcenter())
        .onReceive(animation.$progress, perform: { progress in
            if progress == .startShrinking || progress == .startExpanding{
                withAnimation(.easeIn(duration: 5)){
                    animation.progress = progress.next() // change to expanded or shrinked
                }
                withAnimation(.easeIn(duration: 5)){
                    gridState.opacity = 1
                    gridState.scale = progress == .startShrinking ? 1:0.8
                }
                DispatchQueue.main.asyncAfter(deadline: .now() + 5.05) {
                    animation.progress = .invisible // change to expanded or shrinked
                }
            }
        })
        .cornerRadius(animation.progress.imageIsSmall() ? gridcellCornerR : 0)
        .clipped()
        
    }
    
    func imageXcenter()-> CGFloat{
        if animation.progress.imageIsSmall(){
            return selectedCellFrame.minX + selectedCellFrame.width/2.0
        }else if animation.progress.imageIsLarge(){
            return geoRect.midX
        }else{
            return selectedCellFrame.minX + selectedCellFrame.width/2.0
        }
    }
    
    func imageYcenter()-> CGFloat{
        if animation.progress.imageIsSmall(){
            return selectedCellFrame.minY + (selectedCellFrame.height - gridcellBottomH)/2.0 - safeAreaTopHeight
        }else if animation.progress.imageIsLarge(){
            return geoRect.midY - geoRect.minY
        }else{
            return selectedCellFrame.minY + (selectedCellFrame.height - gridcellBottomH)/2.0
        }
    }
    
    func imageWidthScale()->CGFloat{
        if animation.progress.imageIsLarge(){
            return 1
        } else {
            return selectedCellFrame.width/geoRect.width
        }
    }
    
    func imageHeightScale()->CGFloat{
        if animation.progress.imageIsLarge(){
            return 1
        } else {
            return selectedCellFrame.width/geoRect.width
        }
    }
    
    
    func imageWidth()->CGFloat{
        if animation.progress.imageIsSmall(){
            return selectedCellFrame.width
        }else if animation.progress.imageIsLarge(){
            return geoRect.width
        }else {
            return selectedCellFrame.width
        }
    }
    
    func imageHeight()->CGFloat{
        if animation.progress.imageIsSmall(){
            return selectedCellFrame.height - gridcellBottomH
        }else if animation.progress.imageIsLarge(){
            return geoRect.height
        }else{
            return selectedCellFrame.height
        }
    }
    
}

var redrawcount = 0


struct WebHScrollView: View{
    @EnvironmentObject var addrBarOffset: AddrBarOffset
    @EnvironmentObject var toolbarState: ToolBarState
    @ObservedObject var animation: ShiftAnimation
    
    var body: some View{
        GeometryReader{ geo in
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 0) {
                    ForEach(WebCacheMgr.shared.store, id: \.id){ webCache in
                        TabPageView(webCache: webCache, webWrapper: WebWrapperMgr.shared.webWrapper(of: webCache.id),toolbarState: toolbarState, animation: animation)
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

struct TabHStackView_Previews: PreviewProvider {
    static var previews: some View {
        TabsContainerView()
            .environmentObject(SelectedTab())
    }
}

