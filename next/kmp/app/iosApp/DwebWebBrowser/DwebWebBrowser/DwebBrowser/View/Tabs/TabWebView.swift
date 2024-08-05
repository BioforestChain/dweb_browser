//
//  TabWebView.swift
//  DwebWebBrowser
//
//  Created by linjie on 2024/7/29.
//

import SwiftUI
import WebKit


// A container for using a BrowserWebview in SwiftUI
struct TabWebView: View, UIViewRepresentable {
    let innerWeb: WebView
    
    @Environment(PullingMenu.self) var pullingMenu

    @Binding var xDragOffset: CGFloat
    @Binding var yDragOffset: CGFloat
    @Binding var menuIndex: Int
    @Binding var actionIndex: Int
    
    
    init(webView: WebView,  xOffset: Binding<CGFloat>, yOffset: Binding<CGFloat>, menuIndex: Binding<Int>, actionIndex: Binding<Int>) {
        self.innerWeb = webView
        self._yDragOffset = yOffset
        self._xDragOffset = xOffset
        self._menuIndex = menuIndex
        self._actionIndex = actionIndex
    }
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    func makeUIView(context: UIViewRepresentableContext<TabWebView>) -> WebView {
        innerWeb.scrollView.delegate = context.coordinator
        innerWeb.navigationDelegate = context.coordinator
        
        // 添加手势识别器
        if let panGestureRecognizer = context.coordinator.panGestureRecognizer {
            innerWeb.scrollView.addGestureRecognizer(panGestureRecognizer)
        }
        
        return innerWeb
    }

    func updateUIView(_ uiView: WebView, context: UIViewRepresentableContext<TabWebView>) {
        Log("visiting updateUIView function")
    }
}

class Coordinator: NSObject, WKNavigationDelegate, UIScrollViewDelegate, UIGestureRecognizerDelegate {
    var parent: TabWebView
    var panGestureRecognizer: UIPanGestureRecognizer?
    var chooseIndex = -1
    init(_ parent: TabWebView) {
        self.parent = parent
        super.init()
        self.panGestureRecognizer = UIPanGestureRecognizer(target: self, action: #selector(handlePanGesture))
        self.panGestureRecognizer?.delegate = self
    }
    
    @objc func handlePanGesture(_ gesture: UIPanGestureRecognizer) {
        let webScrollOffset = parent.innerWeb.scrollView.contentOffset.y

        //如果网页没有滚动到顶部，则不会触发
        if webScrollOffset > 0{
            return
        }
        
        let translation = gesture.translation(in: gesture.view)
        if translation.y < 0 {
            return
        }
        if translation.y < minYOffsetToSelectAction{
            chooseIndex = -1
        }else{
            chooseIndex = parent.menuIndex
        }
        if gesture.state == .began {
            parent.menuIndex = 1
            parent.actionIndex = -1
            print("first enter ...")
        }
        
        if gesture.state == .ended {
            withAnimation(.easeInOut) {
                parent.yDragOffset = 0
                parent.xDragOffset = 0
            }
            
            if translation.y > minYOffsetToSelectAction {
                if chooseIndex != -1 {
                    parent.actionIndex = chooseIndex
                }
            }
            return
        }
        
        let draggingX = translation.x // min(max(translation.x, 0), 150)
        let draggingY = translation.y
        
        parent.yDragOffset = draggingY

        if draggingY > minYOffsetToSelectAction {
            if draggingX < -minXOffsetToChangeAction, parent.menuIndex == 0 {
                return
            }
            if draggingX > minXOffsetToChangeAction, parent.menuIndex == 2 {
                return
            }
            if parent.menuIndex == 1 {
                if draggingX > minXOffsetToChangeAction {
                    parent.menuIndex = 2
                    chooseIndex = 2
                    parent.xDragOffset = 0
                }else if draggingX < -minXOffsetToChangeAction {
                    parent.menuIndex = 0
                    chooseIndex = 0
                    parent.xDragOffset = 0
                }else{
                    parent.xDragOffset = draggingX
                }
            } else if (parent.menuIndex == 0) {
                if draggingX > 0 {
                    parent.menuIndex = 1
                    parent.xDragOffset = 0
                    print("menu turned into 1")
                }else{
                    parent.xDragOffset = abs(minXOffsetToChangeAction + draggingX)
                }
            } else {
                if draggingX < 0 {
                    parent.menuIndex = 1
                    parent.xDragOffset = 0
                }else{
                    parent.xDragOffset = -abs(minXOffsetToChangeAction - draggingX)
                }
            }
        } else {
            parent.xDragOffset = 0
        }
    }
    
    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer) -> Bool {
        return true
    }
    
    func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        if !parent.pullingMenu.isActived {
            return false
        }
        guard let panGesture = gestureRecognizer as? UIPanGestureRecognizer else {
            return true
        }
        
        let translation = panGesture.translation(in: panGesture.view)
        
        // 在某些情况下返回 false，让 WebView 自己处理手势
        if translation.y < 0 {
            return false
        }
        
        return true
    }
}

//Dragging menu
extension TabWebView {
    func resetHorizontalPanOffset() {
        if let panGestureRecognizer = makeCoordinator().panGestureRecognizer {
            panGestureRecognizer.setTranslation(CGPoint(x: 0, y: panGestureRecognizer.translation(in: panGestureRecognizer.view).y), in: panGestureRecognizer.view)
        }
    }
}


class LocalWebView: WKWebView {
    deinit {
        print("deinit of LocalWebView called")
    }
}

 #if TestOriginWebView
 typealias WebView = LocalWebView
 #else
typealias WebView = WebBrowserViewWebDataSource.WebType
 #endif
