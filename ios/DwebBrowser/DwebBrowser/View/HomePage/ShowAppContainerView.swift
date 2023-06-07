//
//  ShowAppContainerView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/6/6.
//

import SwiftUI

struct ShowAppContainerView<Content>: View where Content: View {
    
    private let content: Content
    
    private var homeViewModel: ShowHomePageViewModel
    
    init(@ViewBuilder content: () -> Content, viewModel: ShowHomePageViewModel) {
        self.content = content()
        self.homeViewModel = viewModel
    }
    var body: some View {
        content
            .dismissKeyboard()
            .onTapGesture {
                UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
            }
            .overlay(searchOverlay, alignment: .topLeading)
    }
    
    @ViewBuilder private var searchOverlay: some View {
        
        if homeViewModel.isShowEngine {
            ShowSearchHolderView()
                .environmentObject(homeViewModel)
            
        }
    }
}


