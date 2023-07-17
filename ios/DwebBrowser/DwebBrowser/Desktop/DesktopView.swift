//
//  Desktopiew.swift
//  SwiftUIDemo
//
//  Created by ui03 on 2023/7/14.
//

import SwiftUI

struct DesktopView: View {
    
    @ObservedObject var viewModel = OpenAppViewModel()
    @ObservedObject var configViewModel = ConfigViewModel()
    
    var body: some View {
        
        ZStack {
            
            Color.white
                .ignoresSafeArea()
//                .onTapGesture {
//                    withAnimation {
//                        configViewModel.showMenu.toggle()
//                    }
//                }
            
            ForEach(viewModel.apps, id: \.id) { config in
                AppContentView(idString: config.id,isExpand: config.isexpand)
            }
            
            HStack {
                Spacer()
                Color.white
                    .frame(maxWidth: 8, maxHeight: .infinity)
                    .ignoresSafeArea()
                    .onSwipe { direction in
                        if direction == .left {
                            withAnimation {
                                configViewModel.showMenu = true
                            }
                        }
                    }
            }
            
            HStack {
                Spacer()
                ScrollView(.vertical, showsIndicators: false) {
                    AppMenuView()
                }
                .padding(.trailing, configViewModel.showMenu ? 0 : -80 )
            }
        }
        .environmentObject(viewModel)
        .environmentObject(configViewModel)
        .onAppear {
            configViewModel.selectedTab = viewModel.apps.first?.id ?? ""
        }
    }
    
}

struct DesktopView_Previews: PreviewProvider {
    static var previews: some View {
        DesktopView()
    }
}
