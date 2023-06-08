//
//  OverlayView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/10.
//

import SwiftUI

struct OverlayView: View {
    
    @EnvironmentObject var homeViewModel: HomeContentViewModel
    @State private var urlString = ""
    var body: some View {
        
        NavigationView {
            VStack {
                Spacer()
                
                CustomContainerView(content: {
                    ScrollView {
                        HomeHotWebsiteView()
                            .environmentObject(homeViewModel)
                        Spacer()
                        
                    }
                    .navigationBarTitleDisplayMode(.inline)
                }, viewModel: homeViewModel)
                
                OverlayTextFieldView()
                    .frame(height: 50)
                    .padding(EdgeInsets(top: 1, leading: 24, bottom: 0, trailing: 24))
                    .background(SwiftUI.Color.init(red: 245.0/255, green: 246.0/255, blue: 247.0/255, opacity: 1))
                    .edgesIgnoringSafeArea(.bottom)
                    
            }
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        homeViewModel.isShowOverlay = false
                        homeViewModel.isShowEngine = false
                        homeViewModel.isPlaceholderObserver = false
                        homeViewModel.linkPlaceHolderString = homeViewModel.linkString
                        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                    } label: {
                        Text("取消")
                            .foregroundColor(.gray)
                            .padding(8)
                    }
                }
            }
        }
        
    }
    
    @ViewBuilder private var searchOverlay: some View {
        
        if homeViewModel.isShowEngine {
            SearchHolderView()
                .environmentObject(homeViewModel)
            
        }
    }
    
}

struct OverlayView_Previews: PreviewProvider {
    static var previews: some View {
        OverlayView()
    }
}



