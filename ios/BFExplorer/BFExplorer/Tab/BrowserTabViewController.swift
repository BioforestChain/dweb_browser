//
//  BrowserTabViewController.swift
//  Browser
//
//         
//

import UIKit
import WebKit
import RxSwift

protocol BrowserTabViewControllerDelegate: AnyObject {
    func tabViewController(_ tabViewController: BrowserTabViewController, didStartLoadingURL url: URL)
    func tabViewController(_ tabViewController: BrowserTabViewController, didChangeLoadingProgressTo progress: Float)
    func tabViewControllerDidScroll(yOffsetChange: CGFloat)
    func tabViewControllerDidEndDragging()
    
    func updateToolbarButtons()
    func appendLinkToHistory(from webView: WKWebView)
}

class BrowserTabViewController: UIViewController {
    let contentView = BrowserTabContentView()
    private var isScrolling = false
    private var startYOffset = CGFloat(0)
    var shouldAddToHistory = false
    @objc var hasLoadedUrl = false
    //    {
    //        didSet{
    //            self.delegate?.updateToolbarButtons()
    //        }
    //    }
    weak var delegate: BrowserTabViewControllerDelegate?
    
    
    override var preferredStatusBarStyle: UIStatusBarStyle {
//        contentView.statusBarBackgroundView.backgroundColor = .white
        let isBackgroundColorDark = contentView.statusBarBackgroundView.backgroundColor?.isDark ?? false
//        return isBackgroundColorDark ? .lightContent : .default
        return .darkContent
    }
    
    var style: UIStatusBarStyle = .default
     
    // 重现statusBar相关方法
//    override var preferredStatusBarStyle: UIStatusBarStyle {
//        return self.style
//    }
    
    override func loadView() {
        view = contentView
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupWebView()
        let btn = UIButton(frame: CGRect(x: 100, y: 100, width: 100, height: 30))
        btn.backgroundColor = .clear
//        view.addSubview(btn)
//        btn.addTarget(self, action: #selector(changeStyle), for: .touchUpInside)
        
        
    }
    
    @objc func changeStyle(_ sender: Any) {
          if let isHidden = self.navigationController?.isNavigationBarHidden {
              // 切换导航栏显示或者隐藏
              self.navigationController?.isNavigationBarHidden = !isHidden
              // 更新状态栏颜色
              self.style = !isHidden ? .lightContent : .default
              setNeedsStatusBarAppearanceUpdate()
          }
      }
    func updateStatusBarBkColor(){
        updateStatusBarColor()
    }
    
    override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
        switch keyPath {
        case #keyPath(WKWebView.url):
            if contentView.webView.url != nil{
                delegate?.tabViewController(self, didStartLoadingURL: contentView.webView.url!)
                hasLoadedUrl = true
                shouldAddToHistory = true
            }
        case #keyPath(WKWebView.estimatedProgress):
            print("progress1: \(contentView.webView.estimatedProgress)")
          //  print("====check====", contentView.webView.title, contentView.webView.title?.count)
          //  print("url: \(contentView.webView.url)-- progress: \(contentView.webView.estimatedProgress)--title: \(contentView.webView.title)--")
            delegate?.tabViewController(self, didChangeLoadingProgressTo: Float(contentView.webView.estimatedProgress))
            print("progress2: \(contentView.webView.estimatedProgress)")

            if contentView.webView.estimatedProgress > 0.8 {
                //bookmark button become clickable
                print("before call updateToolbarButtons")

                self.delegate!.updateToolbarButtons()
                print("after call updateToolbarButtons")

                // append link to history
                if shouldAddToHistory{
//                    self.delegate!.appendLinkToBookmark(of: contentView.webView)
                    print("before call appendLinkToHistory")

                    self.delegate!.appendLinkToHistory(from: contentView.webView)
                    print("after call appendLinkToHistory")

                }
                shouldAddToHistory = false
            }
            print("progress3: \(contentView.webView.estimatedProgress)")

            
        case "clickedLink":
            guard let url = URL(string: self.contentView.homePageView.hotArticleView!.clickedLink) else {return}
            loadWebsite(from: url)
            
        case #keyPath(WKWebView.canGoBack):
            self.delegate?.updateToolbarButtons()
            
        case #keyPath(WKWebView.canGoForward):
            self.delegate?.updateToolbarButtons()
            
        default:
            if #available(iOS 15.0, *) {
                if keyPath == #keyPath(WKWebView.themeColor){
                    updateStatusBarColor()
                    
                }else if keyPath == #keyPath(WKWebView.underPageBackgroundColor){
                    updateStatusBarColor()
                    
                }
            } else {
                // Fallback on earlier versions
            }
            break
        }
    }
    
    //    override var preferredStatusBarStyle: UIStatusBarStyle {
    //        return .lightContent // .default
    //    }
    
    func loadWebsite(from url: URL) {

        //if the url is same as last one, the url keypath observer won't be trigered, so the hasLoadUrl is still false, the homepageView alpha won't be the right status
        hasLoadedUrl = true
        contentView.webView.load(URLRequest(url: url))
        hideHomePageIfNeeded()
    }
    
    func showEmptyState() {
        UIView.animate(withDuration: 0.2) {
            self.contentView.homePageView.alpha = 1

        }
    }
    
    func hideHomePageIfNeeded() {
        guard hasLoadedUrl else { return }
        UIView.animate(withDuration: 0.2) {
            self.contentView.homePageView.alpha = 0
        }
    }
    
}

// MARK: Helper methods
private extension BrowserTabViewController {
    
    func setupWebView() {
        contentView.webView.scrollView.panGestureRecognizer.addTarget(self, action: #selector(handlePan(_:)))
        contentView.webView.navigationDelegate = self
        
        contentView.webView.addObserver(self, forKeyPath: #keyPath(WKWebView.estimatedProgress), options: .new, context: nil)
        contentView.webView.addObserver(self, forKeyPath: #keyPath(WKWebView.url), options: .new, context: nil)
        if #available(iOS 15.0, *) {
            contentView.webView.addObserver(self, forKeyPath: #keyPath(WKWebView.themeColor), options: .new, context: nil)
        } else {
            // Fallback on earlier versions
        }
        if #available(iOS 15.0, *) {
            contentView.webView.addObserver(self, forKeyPath: #keyPath(WKWebView.underPageBackgroundColor), options: .new, context: nil)
        } else {
            // Fallback on earlier versions
        }
        
        contentView.webView.addObserver(self, forKeyPath: #keyPath(WKWebView.canGoBack), options: .new, context: nil)
        contentView.webView.addObserver(self, forKeyPath: #keyPath(WKWebView.canGoForward), options: .new, context: nil)
    }
    
    func updateStatusBarColor() {
        var color = UIColor.white
        //首页可见，状态栏为白色
        if contentView.homePageView.alpha > 0.98{
            color = .white
        }else{
            if  #available(iOS 15.0, *) {
                color = (contentView.webView.themeColor ?? contentView.webView.underPageBackgroundColor ?? .white).withAlphaComponent(1)
            }
        }
        contentView.statusBarBackgroundView.backgroundColor = color
        setNeedsStatusBarAppearanceUpdate()
    }
    
    internal override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
//        sharedCachesMgr.addObserver(self, forKeyPath: #keyPath(CachesManager.cachedNewsData), options: .new, context: nil)
        contentView.homePageView.hotArticleView?.addObserver(self, forKeyPath: "clickedLink", options: .new, context: nil)
        
        NotificationCenter.default.addObserver(forName: ShowAllBookmarksWhenCountMoreThan8, object: nil, queue: .main) { notify in
            let navVC = UINavigationController(rootViewController: LinkListPageVC(type: .bookmark))
            self.present(navVC, animated: true)
        }
        
        self.navigationController?.navigationBar.barStyle = .black;

        
    }
//    internal override func viewWillDisappear(_ animated: Bool) {
//        super.viewWillDisappear(animated)
////        sharedCachesMgr.removeObserver(self, forKeyPath: "datas")
////        sharedCachesMgr.removeObserver(self, forKeyPath: "clickedLink")
//    }
}

// MARK: Action methods
private extension BrowserTabViewController {
    @objc func handlePan(_ panGestureRecognizer: UIPanGestureRecognizer) {
        let yOffset = contentView.webView.scrollView.contentOffset.y
        switch panGestureRecognizer.state {
        case .began:
            startYOffset = yOffset
        case .changed:
            delegate?.tabViewControllerDidScroll(yOffsetChange: startYOffset - yOffset)
        case .failed, .ended, .cancelled:
            delegate?.tabViewControllerDidEndDragging()
        default:
            break
        }
    }
}

// MARK: WKNavigationDelegate
extension BrowserTabViewController: WKNavigationDelegate {
    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        if navigationAction.navigationType == .linkActivated {
            // handle redirects
            guard let url = navigationAction.request.url else { return }
            webView.load(URLRequest(url: url))
        }
        decisionHandler(.allow)
    }
}
