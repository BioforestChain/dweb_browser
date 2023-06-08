//
//  CustomTextFieldView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/10.
//

import SwiftUI
import Combine

struct CustomTextFieldView<Content>: View where Content: View {
    
    private let content: Content
    @FocusState private var isFocused: Bool
    private var homeViewModel: HomeContentViewModel
    
    init(@ViewBuilder content: () -> Content, viewModel: HomeContentViewModel) {
        self.content = content()
        self.homeViewModel = viewModel
    }
    
    var body: some View {
        content
            .foregroundColor(.black)
            .accentColor(SwiftUI.Color.init(white: 138 / 255))
            .keyboardType(.URL)
            .onSubmit {
                self.textFieldSubmitAction()
            }
            .onChange(of: homeViewModel.linkPlaceHolderString) { newValue in
                if self.homeViewModel.isPlaceholderObserver {
                    self.homeViewModel.isShowEngine = !newValue.isEmpty
                    SearchEngineViewModel.shared.fetchRecordList(placeHolder: newValue)
                }
            }
    }
    
    //点击输入框return按钮
    private func textFieldSubmitAction() {
        homeViewModel.isShowEngine = false
        homeViewModel.isShowOverlay = false
        homeViewModel.pageType = .webPage
        let isLink = isLink(urlString: homeViewModel.linkPlaceHolderString)
        if isLink {
            homeViewModel.linkString = handleURLPrefix(urlString: homeViewModel.linkPlaceHolderString)
        } else {
            homeViewModel.linkString = searchContent(for: .baidu, text: paramURLAbsoluteString(with: homeViewModel.linkPlaceHolderString))
        }
        homeViewModel.hostString = fetchURLHost(urlString: homeViewModel.linkString)
        NotificationCenter.default.post(name: NSNotification.Name.loadUrl, object: nil)
    }
}

