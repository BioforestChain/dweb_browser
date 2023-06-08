//
//  ContentView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/19.
//

import SwiftUI

struct ContentView: View {
    
    @State private var isShowBookmark: Bool = true
    @State private var isShowApp: Bool = true
    @State private var isHotSearch: Bool = false
    @State private var isShowHotWeb: Bool = true
    @State private var isShowSheet: Bool = false
    @State private var isShowOverlay: Bool = true
    
    @StateObject private var webViewModel = WebViewViewModel()
    @StateObject private var homeViewModel = HomeContentViewModel()
    
    var body: some View {
        
        ZStack {
            VStack {
                Spacer()
                
                if homeViewModel.pageType == .homePage {
                    CustomContainerView(content: {
                        ScrollView {
                            VStack(spacing: 28) {
                                if isShowHotWeb {
                                    HomeHotWebsiteView()
                                }
                                if isShowBookmark {
                                    MyBookmarkView()
                                }
                                
                                if isShowApp {
                                    MyAppView()
                                }
                                if isHotSearch {
                                    HotSearchView()
                                }
                            }
                        }
                    }, viewModel: homeViewModel)
                    
                } else if homeViewModel.pageType == .webPage {
                    SwiftUIWebView(webView: webViewModel.webView)
                        .onAppear {
                            self.isShowSheet = false
                            self.homeViewModel.isPlaceholderObserver = false
                            webViewModel.loadUrl(urlString: homeViewModel.linkString)
                            webViewModel.addWebViewObserver()
                            addWebViewObserver()
                        }
                        .onDisappear {
                            NotificationCenter.default.removeObserver(self)
                            webViewModel.cancelLodUrl()
                            webViewModel.removeWebViewObserver()
                        }
                    
                }
                
                HomeAddressView()
                    .environmentObject(webViewModel)
                HomeBottomView(isShowSheet: $isShowSheet, webViewModel: webViewModel)
                    .edgesIgnoringSafeArea(.bottom)
                Spacer()
            }
            .environmentObject(homeViewModel)
            
        }
        .background(SwiftUI.Color.init(red: 245.0/255, green: 246.0/255, blue: 247.0/255, opacity: 1))
        .overlay(starOverlay, alignment: .topLeading)
        
    }
    
    @ViewBuilder private var starOverlay: some View {
        
        if homeViewModel.isShowOverlay {
            OverlayView()
                .environmentObject(homeViewModel)
                .background(.white)
            
        }
    }
    
    private func addWebViewObserver() {
        NotificationCenter.default.addObserver(forName: Notification.Name.loadUrl, object: nil, queue: nil) { _ in
            webViewModel.loadUrl(urlString: homeViewModel.linkString)
            self.isShowSheet = false
            self.homeViewModel.isPlaceholderObserver = false
            self.homeViewModel.pageType = .webPage
        }
        NotificationCenter.default.addObserver(forName: Notification.Name.webViewTitle, object: nil, queue: nil) { notification in
            let title = notification.userInfo?["title"] as? String ?? ""
            let urlString = notification.userInfo?["urlString"] as? String ?? ""
            homeViewModel.webTitleString = title
            homeViewModel.linkPlaceHolderString = urlString
        }
        NotificationCenter.default.addObserver(forName: Notification.Name.hiddenBottomView, object: nil, queue: nil) { notification in
            self.isShowSheet = false
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}



