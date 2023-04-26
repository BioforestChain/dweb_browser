//
//  MainContainerView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI

struct MainContainerView: View {
    @State var adressBarHstackOffset: CGFloat = 0
    @StateObject var expState = TabPagesExpandState()
//    @StateObject var adressBarOffset = AddressBarHStackOffset()

    var body: some View {
        ZStack{
//            Color(UIColor.systemGreen).ignoresSafeArea()
//            Color(.white)
            GeometryReader{ sGgeometry in
                
                VStack(spacing: 0){
                    Color.clear.frame(height: 0.1)  //如果没有这个 向上滚动的时候会和状态栏重合
                    TabPageHStack(adressBarHstackOffset: $adressBarHstackOffset)

                    Divider().background(Color(.darkGray))
                    
                    AddressBarHStack(adressBarHstackOffset: $adressBarHstackOffset)
                        .frame(height: 60)
                    ToolbarView()
                }
                .frame(height: sGgeometry.size.height)
                .coordinateSpace(name: "Root")
                .environmentObject(expState)
//                .environmentObject(adressBarOffset)
            }
        }
    }
}

struct MainContainerView_Previews: PreviewProvider {
    static var previews: some View {
        MainContainerView()
    }
}
