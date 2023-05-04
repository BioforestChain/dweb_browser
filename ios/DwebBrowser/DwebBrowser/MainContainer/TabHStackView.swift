//
//  TabHStackView.swift
//  DwebBrowser
//
//  Created by ui06 on 4/27/23.
//

import SwiftUI

struct TabsContainerView: View{
    @EnvironmentObject var toolbarStates: ToolbarState
    
    var body: some View{
        ZStack{
            TabHStackView()

            if !toolbarStates.showMenu{
                TabsCollectionView()
                    .background(.secondary)
                    .hidden()
            }else{
                TabsCollectionView()
                    .background(.secondary)
            }
        }
    }
}

struct TabHStackView: View{
    @EnvironmentObject var offsetState: MainViewState
    
    var body: some View{
        
//        if #available(iOS 16.0, *) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 0) {
                    TabPageView()
                        .frame(width: screen_width)
                    TabPageView()
                        .frame(width: screen_width)
                    TabPageView()
                        .frame(width: screen_width)
                }
                .offset(x: offsetState.adressBarHstackOffset)
//                .gesture(DragGesture())
                .onAppear {
                    
                }
            }.scrollDisabled(true)

//        } else {
//            // Fallback on earlier versions
//        }
    }
}


struct TabHStackView_Previews: PreviewProvider {
    static var previews: some View {
        TabHStackView()
    }
}
