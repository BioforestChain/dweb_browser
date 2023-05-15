//
//  TabHStackView.swift
//  DwebBrowser
//
//  Created by ui06 on 4/27/23.
//

import SwiftUI
import WebKit

//observe the showingOptions variety to do the switch animation
struct TabsContainerView: View{
    @EnvironmentObject var browser: BrowerVM
    
    @State var selectedCellFrame: CGRect = .zero
    @Namespace private var zoomAnimation
    
    @State private var geoRect: CGRect = .zero // 定义一个变量来存储geoInGlobal的值

    @State var isZoomed = true
    
    init(){
        print("visiting TabsContainerView init")
    }
    var body: some View{
        GeometryReader { geo in
            
            ZStack{
                WebPreViewGrid(selectedCellFrame: $selectedCellFrame)
                    .background(.secondary)
                    .scaleEffect(x:browser.showingOptions ? 1 : 0.8, y:browser.showingOptions ? 1 : 0.8)
                
                if !browser.showingOptions{
                    Color(white: 0.8)
                }
                
                if !browser.showingOptions{
                    TabHStackView()
                }
                
                if browser.shrinkingSnapshot != nil{
                    Image(uiImage: browser.shrinkingSnapshot!)
                        .resizable()
                        .scaledToFill()
                        .frame(width: cellWidth(fullW: geoRect.width),
                               height: cellHeight(fullH: geoRect.height),alignment: .top)
                        .cornerRadius(isZoomed ? 0 : gridcellCornerR)
                    
                        .clipped()
                        .position(x: cellCenterX(geoMidX: geoRect.midX),
                                  y: cellCenterY(geoMidY: geoRect.midY - geoRect.minY))

                        .onAppear{
                            geoRect = geo.frame(in: .global)
                            print()
                            
                            DispatchQueue.main.asyncAfter(deadline: .now()) {
                                withAnimation(.easeInOut(duration: 0.5)) {
                                    isZoomed = false
                                }
                            }
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                                withAnimation(.easeInOut(duration: 30)) {
                                    browser.shrinkingSnapshot = nil
                                }
                            }
                        }
                }
            }
        }
    }
    
    func cellCenterX(geoMidX: CGFloat)-> CGFloat{
        isZoomed ? geoMidX : selectedCellFrame.minX + selectedCellFrame.width/2.0
    }
    
    func cellCenterY(geoMidY: CGFloat)-> CGFloat{
        isZoomed ? geoMidY : selectedCellFrame.minX + (selectedCellFrame.height - gridcellBottomH)/2.0
    }
    
    func cellWidth(fullW: CGFloat)->CGFloat{
        return isZoomed ? fullW : selectedCellFrame.width
    }
    
    func cellHeight(fullH: CGFloat)->CGFloat{
        return isZoomed ? fullH : selectedCellFrame.height - gridcellBottomH
    }
}

struct TabHStackView: View{
    @EnvironmentObject var browser: BrowerVM
    
    var body: some View{
        
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 0) {
                ForEach(browser.pages, id: \.self) { page in
                    TabPageView(webViewStore: page.webStore)
                        .frame(width: screen_width)
                        .onReceive(browser.$shouldTakeSnapshot) { shouldTake in
                            if shouldTake {
                                if let index = browser.pages.firstIndex(of: page), index == browser.selectedTabIndex{
                                    if browser.shrinkingSnapshot == nil, let image = self.environmentObject(browser).snapshot(){
                                        browser.shrinkingSnapshot = image
                                        browser.shouldTakeSnapshot = false
                                        
                                        print(page.webStore.web.snapshot)
                                        page.webStore.web.snapshot = UIImage.saveSnapShot(image, withName: page.webStore.web.id.uuidString)
                                        print(page.webStore.web.snapshot)
                                        
                                        
                                        


                                        print(page.webStore.web.title)
                                        page.webStore.web.title = "else"
                                        print(page.webStore.web.title)
                                    }
                                }
                            }
                        }
                }
            }
            
            .offset(x: browser.addressBarOffset)
        }
        .scrollDisabled(true)
    }
}

struct TabHStackView_Previews: PreviewProvider {
    static var previews: some View {
        TabsContainerView()
            .environmentObject(BrowerVM())
        
    }
}

