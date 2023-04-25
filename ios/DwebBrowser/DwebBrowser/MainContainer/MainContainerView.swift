//
//  MainContainerView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI

struct MainContainerView: View {
    @State var adressBarHstackOffset: CGFloat = 0
    
    var body: some View {
        ZStack{
//            Color(UIColor.systemGreen).ignoresSafeArea()
            Color(.white)
            GeometryReader{ sGgeometry in
                
                VStack(spacing: 0){
                    TabPageHStack(adressBarHstackOffset: $adressBarHstackOffset)
                    Divider().background(Color(.darkGray))
                    
                    AddressBarHStack(adressBarHstackOffset: $adressBarHstackOffset)
                        .frame(height: 60)
                    ToolbarView()
                }
                .frame(height: sGgeometry.size.height)
                .coordinateSpace(name: "Root")
            }
        }
    }
}

struct MainContainerView_Previews: PreviewProvider {
    static var previews: some View {
        MainContainerView()
    }
}
