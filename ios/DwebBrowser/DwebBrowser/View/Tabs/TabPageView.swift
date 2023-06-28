//
//  TabPageView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI
import Combine

struct TabPageView: View {
    @ObservedObject var webCache: WebCache
    @ObservedObject var webWrapper: WebWrapper
    @ObservedObject var toolbarState: ToolBarState
    @ObservedObject var animation: ShiftAnimation
    
    @EnvironmentObject var selectedTab: SelectedTab
    @EnvironmentObject var openingLink: OpeningLink
    
    @State var hasTook = false
    
    @State var snapshotHeight: CGFloat = 0 // CGFloat{ screen_height - addressBarH - toolBarH - safeAreaTopHeight - safeAreaBottomHeight}
    
    private var isVisible: Bool { let index = WebWrapperMgr.shared.store.firstIndex(of: webWrapper); return index == selectedTab.curIndex}
    var  body: some View {
        GeometryReader{geo in
            ZStack{
                HomePageView()
                if webCache.shouldShowWeb {
                    webComponent
                }
            }
            .onAppear{
                snapshotHeight = geo.frame(in: .global).height
            }
        }
        
        .onReceive(animation.$progress, perform: { progress in
            if progress == .initial, toolbarState.showTabGrid, !hasTook{
                let index = WebWrapperMgr.shared.store.firstIndex(of: webWrapper)
                if index == selectedTab.curIndex{
                    if let image = self.environmentObject(selectedTab).environmentObject(openingLink).snapshot(){
                        print(image)
                        let scale = image.scale
                        let cropRect = CGRect(x: 0, y: 0, width: screen_width * scale, height: snapshotHeight * scale)
                        if let croppedCGImage = image.cgImage?.cropping(to: cropRect) {
                            let croppedImage = UIImage(cgImage: croppedCGImage)
                            animation.snapshotImage = croppedImage
                            webCache.snapshotUrl = UIImage.createLocalUrl(withImage: croppedImage, imageName: webCache.id.uuidString)
                        }
                        hasTook = true  //avoid a dead run loop
                        animation.progress = .preparingShrink
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) { hasTook = false } // reset the state var for the next snapshot
                    }
                }
            }
        })
    }
    
    var webComponent: some View{
        WebView(webView: webWrapper.webView, url: webCache.lastVisitedUrl)
            .onChange(of: webWrapper.canGoBack, perform: { canGoBack in
                if isVisible{
                    toolbarState.canGoBack = canGoBack
                }
            })
            .onChange(of: webWrapper.canGoForward, perform: { canGoForward in
                if isVisible{
                    toolbarState.canGoForward = canGoForward
                }
            })
        
            .onChange(of: webWrapper.estimatedProgress){ newValue in
                if newValue >= 1{
                    webWrapper.webView.evaluateJavaScript(watchIosIconScript) { (iconUrl, error) in
                        if let error = error {
                            // 处理执行 JavaScript 错误
                            print("JavaScript error: \(error)")
                            return
                        }
                        guard let urlString = iconUrl as? String, let newUrl = URL(string: urlString) else { return }
                        webCache.webIconUrl = newUrl
                    }
                }
                
                if newValue >= 1.0{
                    WebCacheMgr.shared.saveCaches()
                }
            }
        
            .onChange(of: webWrapper.title!) { title in
                webCache.title = title
            }
            .onChange(of: webWrapper.url) { visitingUrl in
                if let url = visitingUrl{
                    webCache.lastVisitedUrl = url
                    if !TraceLessMode.shared.isON{
                        let manager = HistoryCoreDataManager()
                        let history = LinkRecord(link: webCache.lastVisitedUrl.absoluteString, imageName: webCache.webIconUrl.absoluteString, title: webCache.title, createdDate: Date().milliStamp)
                        manager.insertHistory(history: history)
                    }
                }
            }
        
            .onChange(of: toolbarState.goForwardTapped) { tapped in
                if tapped{
                    goForward()
                    toolbarState.goForwardTapped = false
                }
            }
            .onChange(of: toolbarState.goBackTapped) { tapped in
                if tapped{
                    goBack()
                    toolbarState.goBackTapped = false
                }
            }
        
            .onChange(of: openingLink.clickedLink) { url in
                print("clickedLink has changed: \(url)")
                if isVisible{
                    webWrapper.webView.load(URLRequest(url: url))
                }
            }
    }
    
    func goBack() {
        webWrapper.webView.goBack()
    }
    
    func goForward() {
        webWrapper.webView.goForward()
    }
}

struct TabPageView_Previews: PreviewProvider {
    static var previews: some View {
        Text("problem")
    }
}

// 单次获取图标的 JavaScript 代码
let getIosIconScript = """

"""

// 轮询获取图标的 JavaScript 代码
let watchIosIconScript = """
function getIosIcon(preference_size = 120) {
        webkit.messageHandlers.testlog.postMessage(JSON.stringify([
    ...document.querySelectorAll(`link[rel~="icon"]`).values(),
  ]));

        webkit.messageHandlers.testlog.postMessage(document.documentElement.outerHTML);
  const iconLinks = [
    ...document.querySelectorAll(`link[rel~="icon"]`).values(),
  ].map((ele) => {
    webkit.messageHandlers.testlog.postMessage(JSON.stringify(ele));
    return {
      ele,
      rel: ele.getAttribute("rel"),
      sizes: parseInt(ele.getAttribute("sizes")) || 0,
    };
  });

  const href = (
    iconLinks
      .filter((link) => {
        return link.rel === "apple-touch-icon" || link.rel === "apple-touch-icon-precomposed";
      })
      .sort(
        (a, b) =>
          Math.abs(a.size - preference_size) -
          Math.abs(b.size - preference_size)
      )[0] ??
    iconLinks.findLast(
      (link) => link.rel === "icon" || link.rel === "shortcut icon"
    )
  )?.ele.href ?? 'favicon.ico';

  if (href) {
    const iconUrl = new URL(href, document.baseURI);
    return iconUrl.href;
  }
  return "";
}


getIosIcon()
"""
