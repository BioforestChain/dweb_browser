import Combine
import SwiftUI
import WebKit
import DwebShared
import DwebPlatformIosKit

@dynamicMemberLookup
class WebWrapper: ObservableObject, Identifiable, Hashable, Equatable {
    var id = UUID()

    @Published var webView: DwebWKWebView {
        didSet {
            setupObservers()
        }
    }

    init(cacheID: UUID) {
        self.webView = WKWebViewBridge.companion.shared.webviewFactory()
//        self.webView = BrowserWebview()
        self.id = cacheID
        Log("making a WebWrapper: \(self)")

        setupObservers()
    }

    private func setupObservers() {
        func subscriber<Value>(for keyPath: KeyPath<DwebWKWebView, Value>) -> NSKeyValueObservation {
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
            subscriber(for: \.configuration),
            subscriber(for: \.icon),
        ]
    }

    private var observers: [NSKeyValueObservation] = []

    public subscript<T>(dynamicMember keyPath: KeyPath<DwebWKWebView, T>) -> T {
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

// A container for using a BrowserWebview in SwiftUI
struct TabWebView: View, UIViewRepresentable {
    /// The BrowserWebview to display
    let webView: DwebWKWebView

    init(webView: DwebWKWebView) {
        self.webView = webView
    }

    func makeUIView(context: UIViewRepresentableContext<TabWebView>) -> DwebWKWebView {
        webView.scrollView.delegate = context.coordinator // Set the coordinator as the scroll view delegate
        webView.scrollView.isScrollEnabled = true // Enable web view's scrolling
        return webView
    }

    func updateUIView(_ uiView: DwebWKWebView, context: UIViewRepresentableContext<TabWebView>) {
        Log("visiting updateUIView function")
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, UIScrollViewDelegate {
        var presentView: TabWebView

        init(_ presentView: TabWebView) {
            self.presentView = presentView
        }

        func scrollViewDidScroll(_ scrollView: UIScrollView) {
            let verticalOffset = scrollView.contentOffset.y
            // 处理滚动距离的逻辑
//            Log("Vertical Offset: \(verticalOffset)")
        }
    }
}
