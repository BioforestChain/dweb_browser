import SwiftUI
import Combine
import WebKit

public class BrowserWebview : WKWebView {
    @objc dynamic var icon = NSString("")

    public override init(frame: CGRect, configuration: WKWebViewConfiguration) {
        super.init(frame: frame, configuration: configuration)
        configuration.userContentController.add(self, name: "favicons")
        self.navigationDelegate = self
    }
    
    convenience init() {
        self.init(frame:UIScreen.main.bounds, configuration:WKWebViewConfiguration())
        
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
// 监听获取图标的 JavaScript 代码
let watchIosIconScript = """
function getIosIcon(preference_size = 64) {
  const iconLinks = [
    ...document.head.querySelectorAll(`link[rel*="icon"]`).values(),
  ]
    .map((ele) => {
      return {
        ele,
        rel: ele.getAttribute("rel"),
      };
    })
    .filter((link) => {
      return (
        link.rel === "icon" ||
        link.rel === "shortcut icon" ||
        link.rel === "apple-touch-icon" ||
        link.rel === "apple-touch-icon-precomposed"
      );
    })
    .map((link, index) => {
      const sizes = parseInt(link.ele.getAttribute("sizes")) || 32;
      return {
        ...link,
        // 上古时代的图标默认大小是32
        sizes,
        weight: sizes * 100 + index,
      };
    })
    .sort((a, b) => {
      const a_diff = Math.abs(a.sizes - preference_size);
      const b_diff = Math.abs(b.sizes - preference_size);
      /// 和预期大小接近的排前面
      if (a_diff !== b_diff) {
        return a_diff - b_diff;
      }
      /// 权重大的排前面
      return b.weight - a.weight;
    });

  const href =
    (
      iconLinks
        /// 优先获取 ios 的指定图标
        .filter((link) => {
          return (
            link.rel === "apple-touch-icon" ||
            link.rel === "apple-touch-icon-precomposed"
          );
        })[0] ??
      /// 获取标准网页图标
      iconLinks[0]
    )?.ele.href ?? "favicon.ico";

  const iconUrl = new URL(href, document.baseURI);
  return iconUrl.href;
}
function watchIosIcon(preference_size = 64, message_hanlder_name = "favicons") {
  let preIcon = "";
  const getAndPost = () => {
    const curIcon = getIosIcon(preference_size);
    if (curIcon && preIcon !== curIcon) {
      preIcon = curIcon;
      if (typeof webkit !== "undefined") {
        webkit.messageHandlers[message_hanlder_name].postMessage(curIcon);
      } else {
        console.log("favicon:", curIcon);
      }
      return true;
    }
    return false;
  };

  getAndPost();
  const config = { attributes: true, childList: true, subtree: true };
  const observer = new MutationObserver(getAndPost);
  observer.observe(document.head, config);

  return () => observer.disconnect();
}
void watchIosIcon()
"""
extension BrowserWebview :  WKScriptMessageHandler, WKNavigationDelegate {
    public func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        print("messageHandler: \(message.body as! String)")
        if let value = message .body as? String {
            icon = NSString(string: value.isEmpty ? URL.defaultWebIconURL.absoluteString : value )
        }
    }
    
    public func webView(_ webView: WKWebView, didCommit navigation: WKNavigation!) {
        evaluateJavaScript(watchIosIconScript)
    }
}

@dynamicMemberLookup
public class WebWrapper: ObservableObject, Identifiable, Hashable, Equatable{
    public var id = UUID()

    @Published public var webView: BrowserWebview {
        didSet {
            setupObservers()
        }
    }
    init(cacheID: UUID) {
//        self.webView = BridgeManager.webviewGenerator!(nil)
        self.webView = BrowserWebview()
        self.id = cacheID
        print("making a WebWrapper: \(self)")

        setupObservers()
    }
    
    private func setupObservers() {
        func subscriber<Value>(for keyPath: KeyPath<BrowserWebview, Value>) -> NSKeyValueObservation {
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
    
    public subscript<T>(dynamicMember keyPath: KeyPath<BrowserWebview, T>) -> T {
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

// A container for using a WKWebView in SwiftUI
public struct WebView: View, UIViewRepresentable {
    /// The WKWebView to display
    let url: URL
    public let webView: WKWebView

    public init(webView: WKWebView, url: URL) {
        self.webView = webView
        self.url = url
        if url != self.webView.url {
            webView.load(URLRequest(url:url))
        }
    }
    
    public func makeUIView(context: UIViewRepresentableContext<WebView>) -> WKWebView {
        return webView
    }
    
    public func updateUIView(_ uiView: WKWebView, context: UIViewRepresentableContext<WebView>) {

    }
}
