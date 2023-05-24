//
//  TabHStackView.swift
//  DwebBrowser
//
//  Created by ui06 on 4/27/23.
//

import SwiftUI
import WebKit

let expandingDuration = 0.5
let fadingDuration = 0.1

enum AnimationProgress: Int{
    case initial
    case startExpanding
    case expanded

    case startShrinking
    case shrinked
    
    case fading
    case invisible
    
    case finished
    
    func isAnimating() -> Bool{
        return self.rawValue > AnimationProgress.initial.rawValue && self.rawValue < AnimationProgress.invisible.rawValue
    }
    
    func next()->AnimationProgress{
        switch self{
        case .startExpanding: return .expanded
        case .startShrinking: return .shrinked
        case .fading: return .invisible
        default: return .finished
        }
    }
    func isLarge() -> Bool{
        return self == .startShrinking || self == .expanded
    }
    func isSmall() -> Bool{
        return self == .startExpanding || self == .shrinked
    }
}
let animationDuration: CGFloat = 0.5

// observe the showingOptions variety to do the switch animation
struct TabsContainerView: View{
    @EnvironmentObject var browser: BrowerVM

    @State var cellFrames: [CGRect] = [.zero]

    @State private var geoRect: CGRect = .zero // 定义一个变量来存储geoInGlobal的值

    @State var imageState: AnimationProgress = .initial

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
                WebPreViewGrid(cellFrames: $cellFrames)
                    .background(.secondary)
                    .scaleEffect(x: gridScale, y: gridScale)
                
                if imageState == .shrinked {
                    Color(white: 0.8)
                }

                if !browser.showingOptions, !imageState.isAnimating(){
                    TabHStackView()
                }

                if imageState.isAnimating(){
                    animationImage
                }
            }
            .onAppear{
                geoRect = geo.frame(in: .global)
                print("z geo: \(geoRect)")
            }
            
            .onChange(of: browser.showingOptions) { shouldShowGrid in
                print("change showOptions to \(shouldShowGrid)")
                if shouldShowGrid{
                    imageState = .startShrinking
                }else{
                    imageState = .startExpanding
                }
                withAnimation(.easeInOut(duration: animationDuration),{
                    gridScale = shouldShowGrid ? 1 : 0.8
                })
            }
        }
    }
    
    var animationImage: some View{
        
        Image(uiImage: browser.currentSnapshotImage!)
            .resizable()
            .scaledToFill()
            .frame(width: cellWidth(fullW: geoRect.width),
                   height: cellHeight(fullH: geoRect.height),alignment: .top)
            .cornerRadius(imageState.isSmall() ? gridcellCornerR : 0)

            .clipped()
            .position(x: cellCenterX(geoMidX: geoRect.midX),
                      y: cellCenterY(geoMidY: geoRect.midY - geoRect.minY))

            .onAppear{
                DispatchQueue.main.asyncAfter(deadline: .now()) {
                    withAnimation(.easeInOut(duration: animationDuration)) {
                        imageState = imageState.next() // change to expanded or shrinked
                        
                    }
                }
                
                DispatchQueue.main.asyncAfter(deadline: .now() + animationDuration + 0.1) {
                    imageState = .fading // change to expanded or shrinked
                }
                
                DispatchQueue.main.asyncAfter(deadline: .now() + animationDuration + 0.1) {
                    withAnimation(.easeInOut(duration: fadingDuration)) {
                        imageState = .invisible
                    }
                }
            }
    }


    func cellCenterX(geoMidX: CGFloat)-> CGFloat{
        if imageState.isSmall(){
            return selectedCellFrame.minX + selectedCellFrame.width/2.0
        }else if imageState.isLarge(){
            return geoMidX
        }else{
            return selectedCellFrame.minX + selectedCellFrame.width/2.0
        }
    }

    func cellCenterY(geoMidY: CGFloat)-> CGFloat{
        if imageState.isSmall(){
            return selectedCellFrame.minY + (selectedCellFrame.height - gridcellBottomH)/2.0 - topSafeArea
        }else if imageState.isLarge(){
            return geoMidY
        }else{
            return selectedCellFrame.minY + (selectedCellFrame.height - gridcellBottomH)/2.0
        }
    }

    func cellWidth(fullW: CGFloat)->CGFloat{
        if imageState.isSmall(){
            return selectedCellFrame.width
        }else if imageState.isLarge(){
            return fullW
        }else {
            return selectedCellFrame.width
        }
    }

    func cellHeight(fullH: CGFloat)->CGFloat{
        if imageState.isSmall(){
            return selectedCellFrame.height - gridcellBottomH
        }else if imageState.isLarge(){
            return fullH
        }else{
            return selectedCellFrame.height
        }
    }
}

struct TabHStackView: View{
    @EnvironmentObject var browser: BrowerVM

    @EnvironmentObject var xoffset: AddressBarOffsetOnX

//    @Binding var offset: CGFloat
    private func webWrapper(at index: Int) -> WebWrapper{
        browser.pages[index].webWrapper
    }
    var body: some View{
        GeometryReader{ geo in
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 0) {
                    ForEach(browser.pages.indices){ index in
                        TabPageView(webWrapper: webWrapper(at:index))
                            .frame(width: screen_width)
                           
                    }
                }
                .offset(x: xoffset.offset)
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

