//
//  HomeView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/22.
//

import SwiftUI

struct HomeView: View {
    
    @State private var isPresented = false
    @State private var offsetY: CGFloat = 180
    @State private var bgOpacity: CGFloat = 0.0
    @StateObject private var viewModel = WebViewViewModel()
    
    var body: some View {
        
        ZStack {
            VStack {
                Button {
                    self.isPresented.toggle()
                    withAnimation {
                        offsetY = 0
                        bgOpacity = 1.0
                    }
                } label: {
                    Text("click")
                }
                .padding(20)
                
                SwiftUIWebView(webView: viewModel.webView)
                    .frame(width: 375,height: 400)
                    .onAppear {
//                        viewModel.loadUrl()
                    }
                
                Button("webview") {
//                    viewModel.urlString = "http://www.sina.com.cn"
//                    viewModel.loadUrl()
                }
                
                //点击按钮显示底部弹框
                
            }
            
            if isPresented {
                
                ZStack {
                    SwiftUI.Color.black.opacity(0.1)
                        .opacity(bgOpacity)
                        .edgesIgnoringSafeArea(.all)
                        .onTapGesture {
                            withAnimation {
                                offsetY = 180
                                bgOpacity = 0.0
                            }
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
                                self.isPresented.toggle()
                            }
                        }
                    SheetView(urlString: "http://www.baidu.com")
                        .offset(y: offsetY)
                        .edgesIgnoringSafeArea(.bottom)
                    
                }
            }
        }
    }
}

struct HomeView_Previews: PreviewProvider {
    static var previews: some View {
        HomeView()
    }
}
