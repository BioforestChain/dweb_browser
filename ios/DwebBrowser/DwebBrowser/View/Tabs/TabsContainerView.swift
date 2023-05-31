//
//  TabHStackView.swift
//  DwebBrowser
//
//  Created by ui06 on 4/27/23.
//

import SwiftUI
import WebKit

struct TabsContainerView: View{
    @EnvironmentObject var browser: BrowerVM
    
    @EnvironmentObject var tabState: TabState
    
    @State var cellFrames: [CGRect] = [.zero]
    @State private var geoRect: CGRect = .zero // 定义一个变量来存储geoInGlobal的值
    
    @StateObject var animation = Animation()
    
    private var selectedCellFrame: CGRect {
        cellFrames[browser.selectedTabIndex]
    }
    private var topSafeArea: CGFloat{
        if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let topSafeAreaInset = windowScene.windows.first?.safeAreaInsets.top {
            return topSafeAreaInset
        }
        return 0
    }
    @State var gridScale: CGFloat = 1
    
    init(){
        print("visiting TabsContainerView init")
    }
    
    var body: some View{
        GeometryReader { geo in
            
            ZStack{
                TabGridView(cellFrames: $cellFrames)
                    .scaleEffect(x: gridScale, y: gridScale)
                
                if !tabState.showTabGrid, !animation.progress.isAnimating() {
                    Color(.white)
                }
                if !tabState.showTabGrid, !animation.progress.isAnimating(){
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
            .onChange(of: tabState.showTabGrid, perform: { shouldShowGrid in
                if shouldShowGrid{
                    animation.progress = .initial
                }else{
                    animation.snapshotImage = UIImage.snapshotImage(from: WebCacheMgr.shared.store[browser.selectedTabIndex].snapshotUrl)
                    animation.progress = .startExpanding
                }
                
                withAnimation(.easeInOut(duration: shiftingDuration),{
                    gridScale = shouldShowGrid ? 1 : 0.8
                })
            })
        }
    }
    
    var animationImage: some View{
        Image(uiImage: animation.snapshotImage)
            .resizable()
            .aspectRatio(contentMode: .fill)
            .frame(width: imageWidth(),height: imageHeight(), alignment: .top)
            .cornerRadius(animation.progress.imageIsSmall() ? gridcellCornerR : 0)
            .clipped()
            .position(x: imageXcenter(),y: imageYcenter())
            .onReceive(animation.$progress, perform: { progress in
                if progress == .startShrinking || progress == .startExpanding{
                    withAnimation(.easeInOut(duration: shiftingDuration)) {
                        animation.progress = progress.next() // change to expanded or shrinked
                    }
                    DispatchQueue.main.asyncAfter(deadline: .now() + shiftingDuration + 0.05) {
                        animation.progress = .invisible // change to expanded or shrinked
                    }
                }
            })
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
            return selectedCellFrame.minY + (selectedCellFrame.height - gridcellBottomH)/2.0 - topSafeArea
        }else if animation.progress.imageIsLarge(){
            return geoRect.midY - geoRect.minY
        }else{
            return selectedCellFrame.minY + (selectedCellFrame.height - gridcellBottomH)/2.0
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

struct WebHScrollView: View{
    @EnvironmentObject var addrBarOffset: AddrBarOffset
    @EnvironmentObject var state: TabState
    @ObservedObject var animation: Animation
    
    var body: some View{
        GeometryReader{ geo in
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 0) {
                    ForEach(WebCacheMgr.shared.store){ webCache in
                        TabPageView(webCache: webCache, webWrapper: WebWrapperMgr.shared.webWrapper(of: webCache.id),tabState: state, animation: animation)
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
            .environmentObject(BrowerVM())
    }
}

