import Combine
import SwiftUI
import WebKit

@dynamicMemberLookup
class WebWrapper: ObservableObject, Identifiable, Hashable, Equatable {
    var id = UUID()
    @Published var webMonitor = WebMonitor()

    @Published var icon = ""
    
    @Published var webView: WebView {
        didSet {
            setupObservers()
        }
    }
    
    private var kvoObserver: KVOObserver? = nil

    init(cacheID: UUID) {
#if TestOriginWebView
        self.webView = LocalWebView()
#else
        webView = browserViewDataSource.getWebView()
#endif
        self.webView.isInspectable = true

        self.id = cacheID
        Log("making a WebWrapper: \(self)")

        setupObservers()
    }

    private func setupObservers() {
        
        kvoObserver = KVOObserver { [weak self] key, change in
            guard key == "icon", let newIcon = change?[.newKey] as? String else { return }
            self?.icon = newIcon
        }
        
        func subscriber<Value>(for keyPath: KeyPath<WKWebView, Value>) -> NSKeyValueObservation {
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
        ]
        
        webView.addObserver(kvoObserver!, forKeyPath: "icon", options: .new, context: nil)
        
        observers.append(webView.observe(\.estimatedProgress, options: [.prior]) { [weak self] _, _ in
            if let self = self {
                self.webMonitor.loadingProgress = self.webView.estimatedProgress
            }
        })
    }
    
    private var observers: [NSKeyValueObservation] = []

    public subscript<T>(dynamicMember keyPath: KeyPath<WebView, T>) -> T {
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
        webView.removeObserver(kvoObserver!, forKeyPath: "icon")
        print("deinitial of webwrapper")
    }
}

// A container for using a BrowserWebview in SwiftUI
struct TabWebView: View, UIViewRepresentable {
    /// The BrowserWebview to display
    let innerWeb: WebView

    init(webView: WebView) {
        self.innerWeb = webView
    }

    func makeUIView(context: UIViewRepresentableContext<TabWebView>) -> WebView {
        return innerWeb
    }

    func updateUIView(_ uiView: WebView, context: UIViewRepresentableContext<TabWebView>) {
        Log("visiting updateUIView function")
    }
}

class KVOObserver: NSObject {
    
    typealias ChangedType = (String, [NSKeyValueChangeKey : Any]?) -> Void
    
    let valueDidChange: ChangedType
    
    init(valueDidChange: @escaping ChangedType) {
        self.valueDidChange = valueDidChange
    }
    
    override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
        guard let keyPath = keyPath else { return }
        valueDidChange(keyPath, change)
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
typealias WebView = WKWebView//WebBrowserViewWebDataSource.WebType
#endif
