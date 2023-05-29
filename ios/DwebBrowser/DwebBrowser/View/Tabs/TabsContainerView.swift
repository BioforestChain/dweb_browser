//
//  TabHStackView.swift
//  DwebBrowser
//
//  Created by ui06 on 4/27/23.
//

import SwiftUI
import WebKit

// observe the showingOptions variety to do the switch animation
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
            .onReceive(animation.$progress) { progress in
                print("animation progress is \(progress)")
            }
        }
    }
    
    var animationImage: some View{
        
        Image(uiImage: animation.snapshotImage)
            .resizable()
            .scaledToFill()
            .frame(width: cellWidth(fullW: geoRect.width),
                   height: cellHeight(fullH: geoRect.height),alignment: .top)
            .cornerRadius(animation.progress.imageIsSmall() ? gridcellCornerR : 0)

            .clipped()
            .position(x: cellCenterX(geoMidX: geoRect.midX),
                      y: cellCenterY(geoMidY: geoRect.midY - geoRect.minY))
        
            .onAppear{
                DispatchQueue.main.asyncAfter(deadline: .now()) {
                    withAnimation(.easeInOut(duration: shiftingDuration)) {
                        animation.progress = animation.progress.next() // change to expanded or shrinked
                    }
                }
                
//                DispatchQueue.main.asyncAfter(deadline: .now() + shiftingDuration + 0.05) {
//                    animation.progress = .fading // change to expanded or shrinked
//                }
                
                DispatchQueue.main.asyncAfter(deadline: .now() + shiftingDuration + 0.1) {
//                    withAnimation(.easeInOut(duration: fadingDuration)) {
                        animation.progress = .invisible
//                    }
                }
            }
    }

    func cellCenterX(geoMidX: CGFloat)-> CGFloat{
        if animation.progress.imageIsSmall(){
            return selectedCellFrame.minX + selectedCellFrame.width/2.0
        }else if animation.progress.imageIsLarge(){
            return geoMidX
        }else{
            return selectedCellFrame.minX + selectedCellFrame.width/2.0
        }
    }

    func cellCenterY(geoMidY: CGFloat)-> CGFloat{
        if animation.progress.imageIsSmall(){
            return selectedCellFrame.minY + (selectedCellFrame.height - gridcellBottomH)/2.0 - topSafeArea
        }else if animation.progress.imageIsLarge(){
            return geoMidY
        }else{
            return selectedCellFrame.minY + (selectedCellFrame.height - gridcellBottomH)/2.0
        }
    }

    func cellWidth(fullW: CGFloat)->CGFloat{
        if animation.progress.imageIsSmall(){
            return selectedCellFrame.width
        }else if animation.progress.imageIsLarge(){
            return fullW
        }else {
            return selectedCellFrame.width
        }
    }

    func cellHeight(fullH: CGFloat)->CGFloat{
        if animation.progress.imageIsSmall(){
            return selectedCellFrame.height - gridcellBottomH
        }else if animation.progress.imageIsLarge(){
            return fullH
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
    
    func takeScreenshot(of rect: CGRect) -> UIImage? {
            guard let window = UIApplication.shared.windows.first else {
                return nil
            }
            let renderer = UIGraphicsImageRenderer(bounds: rect)
            let image = renderer.image { context in
                window.drawHierarchy(in: rect, afterScreenUpdates: false)
            }
            return image
        }
    
    
    func snapshot() -> UIImage? {
        // 创建UIView
        let uiView = UIView(frame: CGRect(origin: .zero, size: CGSize(width: UIScreen.main.bounds.width, height: UIScreen.main.bounds.height)))

        // 将视图添加到UIView上
        let hostingController = UIHostingController(rootView: self)
        hostingController.view.frame = uiView.bounds
        uiView.addSubview(hostingController.view)

        // 绘制屏幕可见区域
        UIGraphicsBeginImageContextWithOptions(uiView.bounds.size, false, UIScreen.main.scale)
        uiView.drawHierarchy(in: uiView.bounds, afterScreenUpdates: true)

        // 获取截图并输出
        let image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return image
    }
    
}

struct TabHStackView_Previews: PreviewProvider {
    static var previews: some View {
        TabsContainerView()
            .environmentObject(BrowerVM())
    }
}

