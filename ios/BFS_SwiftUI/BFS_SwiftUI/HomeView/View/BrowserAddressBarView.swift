//
//  BrowserAddressBarView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/26.
//

import SwiftUI

struct BrowserAddressBarView: View {
    
    @FocusState private var isShowLeftImage: Bool
    @State private var urlString = ""
    @State var progress: Float = 0.0
    
    @ObservedObject var webViewModel: WebViewViewModel
    @EnvironmentObject var homeViewModel: HomeContentViewModel
    
    var body: some View {
       
        VStack {
            ZStack {
                HStack {
                    if !isShowLeftImage {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(SwiftUI.Color.init(white: 138 / 255))
                            .frame(width: 22, height: 22)
                            .padding(.leading, 10)
                    } else {
                        Spacer(minLength: 15)
                    }
                    
                    CustomTextFieldView(content: {
                        TextField("搜索或输入网址", text: textAlignmentCoordition() ? $homeViewModel.linkPlaceHolderString : $homeViewModel.hostString)
                            .frame(height: 30)
                            .multilineTextAlignment(textAlignmentCoordition() ? .leading : .center)
                            .focused($isShowLeftImage)
                            .onTapGesture {
                                self.clickTextField()
                            }
                    }, viewModel: homeViewModel)
                    
                    CustomDeleteButton(viewModel: homeViewModel)
                        .opacity(self.homeViewModel.linkPlaceHolderString.isEmpty ? 0 : 1)

                    Spacer()
                }
                CustomProgress(value: progress)
                    .accentColor(.blue)
                    .frame(height: 3)
                    .padding(.top, 42)
                    
            }
        }
        .background(.white)
        .cornerRadius(8)
        .shadow(color: SwiftUI.Color(.lightGray).opacity(0.15), radius: 4)
        .onAppear {
            self.progressObserver()
        }
        .onDisappear {
            NotificationCenter.default.removeObserver(self)
        }
    }
}

extension BrowserAddressBarView {
    
    //输入框显示状态条件
    private func textAlignmentCoordition() -> Bool {
        
        if homeViewModel.linkString.isEmpty {
            return true
        }
        return isShowLeftImage
    }
    
    //添加进度条通知
    private func progressObserver() {
        NotificationCenter.default.addObserver(forName: Notification.Name.progress, object: nil, queue: nil) { notification in
            self.progress = notification.userInfo?["progress"] as? Float ?? 0.0
            if self.progress >= 1.0 {
                self.progress = 0.0
            }
        }
    }
    
    //点击输入框
    private func clickTextField() {
        homeViewModel.isPlaceholderObserver = true
        //如果是首页 或搜索隐藏界面不操作
        if homeViewModel.isShowEngine || homeViewModel.pageType == .homePage {
            return
        }
        
        
        homeViewModel.isShowOverlay = true
        isShowLeftImage = false
    }
}

struct BrowserAddressBarView_Previews: PreviewProvider {
    static var previews: some View {
        BrowserAddressBarView(webViewModel: WebViewViewModel())
    }
}

struct CustomProgress: UIViewRepresentable {
    
    let value: Float
    
    func makeUIView(context: Context) -> UIProgressView {
        let progressView = UIProgressView()
        progressView.progressTintColor = .blue
        progressView.trackTintColor = .clear
        return progressView
    }
    
    func updateUIView(_ uiView: UIProgressView, context: Context) {
        if value <= 0.0 {
            uiView.setProgress(value, animated: false)
        } else {
            uiView.setProgress(value, animated: true)
        }
    }
}
