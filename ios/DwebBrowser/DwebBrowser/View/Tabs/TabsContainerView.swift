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
                        .aspectRatio(contentMode: .fill)
                        .frame(width: cellWidth(fullW: geo.frame(in: .global).width),
                               height: cellHeight(fullH: geo.frame(in: .global).height))
                        .cornerRadius(gridcellCornerR)
                    
                        .clipped()
                        .position(x: cellCenterX(geoMidX: geo.frame(in: .global).midX),
                                  y: cellCenterY(geoMidY: geo.frame(in: .global).midY))
                    
                        .onAppear{
                            print(geo.frame(in: .global))
                            
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.01) {
                                withAnimation(.easeInOut(duration: 30)) {
                                    isZoomed = false
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
    
    var AnimationImage: some View{
        Image(uiImage: UIImage())//browser.shrinkingSnapshot
            .resizable()
            .aspectRatio(contentMode: .fill)
            .onTapGesture {
                withAnimation(.spring()) {
                    //                    isZoomed.toggle()
                }
                
            }
    }
    
    func startShifting(){
        let image = self.environmentObject(self.browser).snapshot()
        print(image)
        
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
                        .onReceive(browser.$shouldTakeSnapshot) { show in
                            if show {
                                if let index = browser.pages.firstIndex(of: page), index == browser.selectedTabIndex{
                                    if let image = self.environmentObject(browser).snapshot(){
                                        browser.shrinkingSnapshot = image
                                    }
                                    
                                    browser.shouldTakeSnapshot = false
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

