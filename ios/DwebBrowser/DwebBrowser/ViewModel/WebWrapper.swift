import SwiftUI
import Combine
import WebKit

var visitCount: Int = 0

class WebWrapperArray: ObservableObject {
    @Published var wrappers: [WebWrapper] = []
}

@dynamicMemberLookup
public class WebWrapper: ObservableObject, Identifiable,Hashable,Equatable{

    public var id = UUID()
//    @Published var webCache: WebCache
    @Published public var webView: WKWebView {
        didSet {
            setupObservers()
        }
    }
    
    init(cacheID: UUID) {
//        let sharedManager = WebWrapperManager.shared // 将shared实例化放在init之前
//        let webWrapper = WebWrapperManager.shared.webWrapper(of: cacheID)
////            self.webView = webWrapper.webView
//
////        let webWrapper = WebWrapperManager.webWrapper(of: cacheID)
        self.webView = WKWebView()
        self.id = cacheID
        print("making a WebWrapper: \(self)")

        setupObservers()
    }
    
    private func setupObservers() {
        func subscriber<Value>(for keyPath: KeyPath<WKWebView, Value>) -> NSKeyValueObservation {
            return webView.observe(keyPath, options: [.prior]) { _, change in
                if change.isPrior {
                    DispatchQueue.main.async {
                        self.objectWillChange.send()
                    }
                }
            }
        }
        // Setup observers for all KVO compliant properties
        observers = [
            subscriber(for: \.title),
            subscriber(for: \.url),
            subscriber(for: \.isLoading),
            subscriber(for: \.estimatedProgress),
            subscriber(for: \.hasOnlySecureContent),
            subscriber(for: \.serverTrust),
            subscriber(for: \.canGoBack),
            subscriber(for: \.canGoForward),
        ]
    }
    
    private var observers: [NSKeyValueObservation] = []
    
    public subscript<T>(dynamicMember keyPath: KeyPath<WKWebView, T>) -> T {
        webView[keyPath: keyPath]
    }
    
    public static func == (lhs: WebWrapper, rhs: WebWrapper) -> Bool {
        lhs.id == rhs.id
     }
     
     public func hash(into hasher: inout Hasher) {
         hasher.combine(id)
         hasher.combine(webView)
     }
}

var webViews = Set<WKWebView>()
/// A container for using a WKWebView in SwiftUI
public struct WebView: View, UIViewRepresentable {
    /// The WKWebView to display
    let url: URL
    public let webView: WKWebView

    public init(webView: WKWebView, url: URL) {
        self.webView = webView
        self.url = url
        webViews.insert(webView)
        print("using a webView: \(self.webView)")
        print("\(webViews.count) have been made")
    }
    
    public func makeUIView(context: UIViewRepresentableContext<WebView>) -> WKWebView {
        if webView.estimatedProgress < 0.2{
            webView.load(URLRequest(url:url))
            print("in WebView makeUIView.....")
        }
        return webView
    }
    
    public func updateUIView(_ uiView: WKWebView, context: UIViewRepresentableContext<WebView>) {
//        uiView.load(URLRequest(url:url))
        print("in WebView updateUIView.....")


    }
}
