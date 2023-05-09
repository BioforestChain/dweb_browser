//
//  TabHStackView.swift
//  DwebBrowser
//
//  Created by ui06 on 4/27/23.
//

import SwiftUI
import WebKit

struct TabsContainerView: View{
    @EnvironmentObject var optionsState: BrowerVM
    
    var body: some View{
        ZStack{
            TabHStackView()
            
            if optionsState.showingOptions{
                TabsCollectionView()
                    .background(.secondary)
            }else{
                TabsCollectionView()
                    .background(.secondary)
                    .hidden()
            }
        }
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
                }
            }
            .offset(x: browser.addressBarOffset)
        }
        .scrollDisabled(true)
    }
}

struct TabHStackView_Previews: PreviewProvider {
    static var previews: some View {
        TabHStackView()
            .environmentObject(BrowerVM())

    }
}
