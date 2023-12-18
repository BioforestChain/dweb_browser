import Combine
import DwebPlatformIosKit
import DwebShared
import SwiftUI
import WebKit

@dynamicMemberLookup
class WebWrapper: ObservableObject, Identifiable, Hashable, Equatable {
    var id = UUID()
    @Published var webMonitor = WebMonitor()

    @Published var webView: TestWebView {
        didSet {
            setupObservers()
        }
    }

    init(cacheID: UUID) {
#if TestOriginWebView
        self.webView = LocalWebView()
#else
        self.webView = WKWebViewBridge.companion.shared.webviewFactory()
#endif
        self.webView.isInspectable = true

        self.id = cacheID
        Log("making a WebWrapper: \(self)")

        setupObservers()
    }

    private func setupObservers() {
        func subscriber<Value>(for keyPath: KeyPath<TestWebView, Value>) -> NSKeyValueObservation {
            return webView.observe(keyPath, options: [.prior]) { [weak self] _, change in
                if let self = self, change.isPrior {
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
            subscriber(for: \.canGoForward),
            subscriber(for: \.configuration),
            subscriber(for: \.icon),
        ]

        observers.append(webView.observe(\.estimatedProgress, options: [.prior]) { [weak self] _, _ in
            if let self = self {
                self.webMonitor.loadingProgress = self.webView.estimatedProgress
            }
        })
    }

    private var observers: [NSKeyValueObservation] = []

    public subscript<T>(dynamicMember keyPath: KeyPath<TestWebView, T>) -> T {
        webView[keyPath: keyPath]
    }

    public static func == (lhs: WebWrapper, rhs: WebWrapper) -> Bool {
        lhs.id == rhs.id
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(id)
        hasher.combine(webView)
    }

    deinit {
        print("deinitial of webwrapper")
    }
}

// A container for using a BrowserWebview in SwiftUI
struct TabWebView: View, UIViewRepresentable {
    /// The BrowserWebview to display
    let innerWeb: TestWebView

    init(webView: TestWebView) {
        self.innerWeb = webView
    }

    func makeUIView(context: UIViewRepresentableContext<TabWebView>) -> TestWebView {
        return innerWeb
    }

    func updateUIView(_ uiView: TestWebView, context: UIViewRepresentableContext<TabWebView>) {
        Log("visiting updateUIView function")
    }
}

class LocalWebView: WKWebView {
    deinit {
        print("deinit of LocalWebView called")
    }
}

#if TestOriginWebView
typealias TestWebView = LocalWebView
#else
typealias TestWebView = DwebWKWebView
#endif
