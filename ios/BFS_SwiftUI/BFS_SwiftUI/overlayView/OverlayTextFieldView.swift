//
//  OverlayTextFieldView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/10.
//

import SwiftUI



struct OverlayTextFieldView: View {
    
    
    @State private var urlString = ""
    @EnvironmentObject var homeViewModel: HomeContentViewModel
    @FocusState private var isFocused: Bool
    @State private var keyboardHeight: CGFloat = 0
    
    var body: some View {
        
        HStack {
            
            CustomTextFieldView(content: {
                TextField("搜索或输入网址", text: $homeViewModel.linkPlaceHolderString)
                    .frame(height: 40)
                    .multilineTextAlignment(.leading)
                    .padding(.leading, 16)
                    .focused($isFocused)
                    .onAppear {
                        isFocused = true
                    }
                    .onTapGesture {
                        homeViewModel.isPlaceholderObserver = true
                    }
            }, viewModel: homeViewModel)
                
            CustomDeleteButton(viewModel: homeViewModel)
                .opacity(self.homeViewModel.linkPlaceHolderString.isEmpty ? 0 : 1)

            Spacer()
        }
        .background(.white)
        .cornerRadius(10)
        
    }
}

struct OverlayTextFieldView_Previews: PreviewProvider {
    static var previews: some View {
        OverlayTextFieldView()
    }
}

