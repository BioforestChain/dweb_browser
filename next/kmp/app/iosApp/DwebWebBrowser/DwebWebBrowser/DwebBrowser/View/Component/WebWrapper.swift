import Combine
import SwiftUI
import WebKit

@dynamicMemberLookup
class WebWrapper: ObservableObject, Identifiable, Hashable, Equatable {
    var id = UUID()
    @Published var webMonitor = WebMonitor()

    @Published var webView: WebView {
        didSet {
            setupObservers()
        }
    }

    init(cacheID: UUID) {
#if TestOriginWebView
        self.webView = LocalWebView()
#else
        self.webView = browserViewDataSource.getWebView()
#endif
        self.webView.isInspectable = true

        self.id = cacheID
        Log("making a WebWrapper: \(self)")

        setupObservers()
    }

    private func setupObservers() {
        func subscriber<Value>(for keyPath: KeyPath<WebView, Value>) -> NSKeyValueObservation {
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
        print("deinitial of webwrapper")
    }
}

// A container for using a BrowserWebview in SwiftUI
struct TabWebView: View, UIViewRepresentable {
    @EnvironmentObject var scale: WndDragScale
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

    // 创建一个Coordinator
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    // 在这里定义你的Coordinator类
    class Coordinator: NSObject {
        var parent: TabWebView
        var cancellable = Set<AnyCancellable>()

        init(_ parent: TabWebView) {
            self.parent = parent
            // 现在我们可以使用self.parent来设置我们的观察者
            super.init() // 调用super.init()
            self.parent.scale.$onWidth.sink { [weak self] newScale in
                DispatchQueue.main.async {
//                       let jsString = """
//                           document.getElementsByTagName('body')[0].style.transform = 'scale(\(newScale))';
//                           document.getElementsByTagName('body')[0].style.transformOrigin = 'top left';
//                       """
//
//                       self?.parent.innerWeb.evaluateJavaScript(jsString) { result, error in
//                           if let error = error {
//                               print(error.localizedDescription)
//                           }
//                       }

//                    self?.parent.innerWeb.scrollView.zoomScale = newScale // 根据需要调整
                }
            }.store(in: &cancellable)
        }
    }
}

class LocalWebView: WKWebView {
    deinit {
        print("deinit of LocalWebView called")
    }
}

// #if TestOriginWebView
// typealias WebView = LocalWebView
// #else
typealias WebView = WebBrowserViewWebDataSource.WebType
// #endif
