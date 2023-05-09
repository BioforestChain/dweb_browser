import SwiftUI
import Combine
import WebKit


var visitCount: Int = 0

@dynamicMemberLookup
public class WebViewStore: ObservableObject,Identifiable,Hashable{
    public static func == (lhs: WebViewStore, rhs: WebViewStore) -> Bool {
         lhs.id == rhs.id
     }
     
     public func hash(into hasher: inout Hasher) {
         hasher.combine(id)
     }
    
    public let id = UUID()
    @Published public var webView: WKWebView {
        didSet {
            setupObservers()
        }
    }
    
    @Published public var web: WebPage
    
    public init(webView: WKWebView = WKWebView(), web: WebPage) {
        self.webView = webView
        self.web = web
        webView.load(URLRequest(url: URL(string: "www.bing.com")!))
        visitCount += 1
        print("has visited \(visitCount) times")

        setupObservers()
    }
    
    private func setupObservers() {
        func subscriber<Value>(for keyPath: KeyPath<WKWebView, Value>) -> NSKeyValueObservation {
            return webView.observe(keyPath, options: [.prior]) { _, change in
                if change.isPrior {
                    self.objectWillChange.send()
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
            subscriber(for: \.canGoForward)
        ]
    }
    
    private var observers: [NSKeyValueObservation] = []
    
    public subscript<T>(dynamicMember keyPath: KeyPath<WKWebView, T>) -> T {
        webView[keyPath: keyPath]
    }
}

/// A container for using a WKWebView in SwiftUI
public struct WebView: View, UIViewRepresentable {
    /// The WKWebView to display
    let url: URL
    public let webView: WKWebView

    public init(webView: WKWebView, url: URL) {
        self.webView = webView
        self.url = url
        
    }
    
    public func makeUIView(context: UIViewRepresentableContext<WebView>) -> WKWebView {
        return webView
    }
    
    public func updateUIView(_ uiView: WKWebView, context: UIViewRepresentableContext<WebView>) {
        uiView.load(URLRequest(url:url))

    }
}
