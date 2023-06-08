//
//  CustomDeleteButton.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/10.
//

import SwiftUI

struct CustomDeleteButton: View {
    
    private var homeViewModel: HomeContentViewModel
    
    init(viewModel: HomeContentViewModel) {
        self.homeViewModel = viewModel
    }
    
    var body: some View {
        Button {
            self.deleteAction()
        } label: {
            Image(systemName: "xmark.circle.fill")
                .foregroundColor(SwiftUI.Color(.lightGray))
        }
        .padding(.trailing, 8)
        .opacity(self.homeViewModel.linkPlaceHolderString.isEmpty ? 0 : 1)
    }
    
    //点击输入框删除按钮
    private func deleteAction() {
        self.homeViewModel.linkPlaceHolderString = ""
    }
}

