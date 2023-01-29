//
//  BrowserContainerViewController.swift
//  Browser
//
//         
//

import UIKit
import WebKit
import Toast_Swift

let ShowAllBookmarksWhenCountMoreThan8 = Notification.Name("ShowAllBookmarksWhenCountMoreThan8")
let newsLinkClickedNotification = Notification.Name("NewsLinkClickedNotification")
let hideKeyboardAfterScrollviewDidScrolled = Notification.Name("hidekeyboardAfterScrollviewDidScrolled")
let bookmarksWasEditedNotification = Notification.Name("bookmarksWasEditedNotification")
let openAnAppNotification = Notification.Name("openAnAppNotification")

var shouldHideHomePage = true

class BrowserContainerViewController: UIViewController,  OverlayShareViewDelegate {
    func overlayShareView(_ shareView: OverlayShareView, selectedType: PageType) {
        UIApplication.pk.keyWindow?.dissmiss(overlay: .first)
        
        if selectedType == .share{
            shareThePage()
            return
        }
        
        let navVC = UINavigationController(rootViewController: LinkListPageVC(type: selectedType))
        self.present(navVC, animated: true)
    }
    
    let contentView = BrowserContainerContentView()
    var tabViewControllers = [BrowserTabViewController]()
    let viewModel: BrowserContainerViewModel
    
    // MARK: - OverlayShareView
    
    lazy var shareView: OverlayShareView = {
        let view = OverlayShareView(frame: CGRect(x: 0, y: 0, width: UIScreen.pk.width, height: 180))
        view.delegate = self
        
        view.pk.addCorner(radius: 12, byRoundingCorners: [.topLeft, .topRight])
        return view
    }()
    
    lazy var shareOVC: OverlayController = {
        let ovc = OverlayController(view: self.shareView)
        ovc.maskStyle = .black(opacity: 0.35)
        ovc.layoutPosition = .bottom
        ovc.presentationStyle = .fromToBottom
        
        return ovc
    }()
    
    // Address bar animation properties
    var isAddressBarActive = false
    var hasHiddenTab = false
    var currentTabIndex = 0 {
        didSet {
            updateAddressBarsAfterTabChange()
            setNeedsStatusBarAppearanceUpdate()
            updateToolbarButtons()
        }
    }
    
    // Toolbar animation properties
    var collapsingToolbarAnimator: UIViewPropertyAnimator?
    var expandingToolbarAnimator: UIViewPropertyAnimator?
    var isCollapsed = false
    
    var currentAddressBar: BrowserAddressBar {
        contentView.addressBars[currentTabIndex]
    }
    
    var leftAddressBar: BrowserAddressBar? {
        contentView.addressBars[safe: currentTabIndex - 1]
    }
    
    var rightAddressBar: BrowserAddressBar? {
        contentView.addressBars[safe: currentTabIndex + 1]
    }
    
    override var childForStatusBarStyle: UIViewController? {
        tabViewControllers[safe: currentTabIndex]
    }
    
    init(viewModel: BrowserContainerViewModel = .init()) {
        self.viewModel = viewModel
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func loadView() {
        view = contentView
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupCancelButton()
        setupAddressBarsScrollView()
        setupAddressBarsExpandingOnTap()
        setupKeyboardManager()
        openNewTab(isHidden: false)
        NotificationCenter.default.addObserver(forName: newsLinkClickedNotification, object: nil, queue: .main) { notify in
            print("-----******in BrowserContainerViewController newsLinkClickedNotification")

            guard let link = notify.object as? String else { return }
            let addressBar = self.currentAddressBar
            addressBar.updateStateWhenClicked(link: link)
            self.updateStateForKeyboardDisappearing(needHideSideBar: false)
            
            self.addressBar(self.currentAddressBar, didReturnWithText: link)
        }
        
        NotificationCenter.default.addObserver(forName: bookmarksWasEditedNotification, object: nil, queue: .main) {_ in
            print("-----******in BrowserContainerViewController bookmarksWasEditedNotification")

            for vc in self.tabViewControllers{
                vc.contentView.homePageView.reloadBookMarkView()
            }
//            contentView.homePageView.reloadBookMarkView()

        }
        
        NotificationCenter.default.addObserver(forName: hideKeyboardAfterScrollviewDidScrolled, object: nil, queue: .main) { notify in
            print("-----******in BrowserContainerViewController hideKeyboardAfterScrollviewDidScrolled")

            self.dismissKeyboard()
        }
        contentView.toolbar.bottomToolbarDelegate = self
        
        
        NotificationCenter.default.addObserver(forName: openAnAppNotification, object: nil, queue: .main) {notify in
            print("-----******in BrowserContainerViewController openAnAppNotification")

            if let vc = notify.object as? WebViewViewController{
                self.navigationController?.pushViewController(vc, animated: true)
            }
        }
        
        // 用于jscore关闭app应用
        NotificationCenter.default.addObserver(forName: NSNotification.Name.closeAnAppNotification, object: nil, queue: .main) { notify in
            print("-----******in BrowserContainerViewController closeAnAppNotification")
            
            self.navigationController?.popViewController(animated: true)
        }
        
        sharedCachesMgr.addObserver(self, forKeyPath: #keyPath(CachesManager.cachedNewsData), options: .new, context: nil)
        
        NotificationCenter.default.addObserver(forName: NSNotification.Name.progressNotification, object: nil, queue: .main) { notify in
            self.tabViewControllers.map { vc in
                vc.contentView.homePageView.installingProgressUpdate(notify)
            }
            
//            self.progressUpdateNotify(noti: notify)
        }
        
        
        automaticallyAdjustsScrollViewInsets = false
        extendedLayoutIncludesOpaqueBars = true
    }
    
    func setCancelButtonHidden(_ isHidden: Bool) {
//        if shouldHideHomePage{
            UIView.animate(withDuration: 0.1) {
                self.contentView.cancelButton.alpha = isHidden ? 0 : 1
            }
//        }
    }
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        if #available(iOS 13.0, *) {
//            let statusBar = UIView(frame: UIApplication.shared.keyWindow?.windowScene?.statusBarManager?.statusBarFrame ?? CGRect.zero)
//            statusBar.backgroundColor = .darkGray
//            UIApplication.shared.keyWindow?.addSubview(statusBar)
        }
        edgesForExtendedLayout = []
        self.navigationController?.view.backgroundColor = .white
        self.navigationController?.setNavigationBarHidden(true, animated: false)
        view.backgroundColor = .white
//        extendedLayoutIncludesOpaqueBars = false;
//                self.modalPresentationCapturesStatusBarAppearance = false;
    }
    
    override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
        switch keyPath {
            
        case #keyPath(CachesManager.cachedNewsData):
            DispatchQueue.main.async {
                print("-----******in BrowserContainerViewController observeValue")
                self.tabViewControllers.map { controller in
                    guard let newsList = sharedCachesMgr.fetchNews(), newsList.count > 0 else { return }
                    controller.contentView.homePageView.hotArticleView?.updateHotNewsList(newsList: newsList)
                }
            }
        default:
            break
        }
    }
}

// MARK: Helper methods
private extension BrowserContainerViewController {
    func setupAddressBarsScrollView() {
        contentView.addressBarsScrollView.delegate = self
    }
    
    func setupCancelButton() {
        contentView.cancelButton.addTarget(self, action: #selector(cancelButtonTapped), for: .touchUpInside)
    }
    
    func openNewTab(isHidden: Bool) {
        addTabViewController(isHidden: isHidden)
        addAddressBar(isHidden: isHidden)
    }
    
    func addTabViewController(isHidden: Bool) {
        let tabViewController = BrowserTabViewController()
        tabViewController.view.alpha = isHidden ? 0 : 1
        tabViewController.view.transform = isHidden ? CGAffineTransform(scaleX: 0.8, y: 0.8) : .identity
        tabViewController.showEmptyState()
        tabViewController.delegate = self
        tabViewControllers.append(tabViewController)
        contentView.tabsStackView.addArrangedSubview(tabViewController.view)
        tabViewController.view.snp.makeConstraints {
            $0.width.equalTo(contentView)
        }
        addChild(tabViewController)
        tabViewController.didMove(toParent: self)
    }
    
    func addAddressBar(isHidden: Bool) {
        let addressBar = BrowserAddressBar()
        addressBar.delegate = self
        contentView.addressBarsStackView.addArrangedSubview(addressBar)
        addressBar.snp.makeConstraints {
            $0.width.equalTo(contentView).offset(contentView.addressBarWidthOffset)
        }
        
        if isHidden {
            hasHiddenTab = true
            addressBar.containerViewWidthConstraint?.update(offset: contentView.addressBarContainerHidingWidthOffset)
            addressBar.containerView.alpha = 0
            addressBar.plusOverlayView.alpha = 1
        }
    }
    
    func dismissKeyboard() {
        print("homepage alpha observe place --2: \(self.tabViewControllers[currentTabIndex].contentView.homePageView.alpha)")

        view.endEditing(true)
        print("homepage alpha observe place --3: \(self.tabViewControllers[currentTabIndex].contentView.homePageView.alpha)")

    }
    
    func updateAddressBarsAfterTabChange() {
        currentAddressBar.setSideButtonsHidden(false)
        currentAddressBar.isUserInteractionEnabled = true
        leftAddressBar?.setSideButtonsHidden(true)
        leftAddressBar?.isUserInteractionEnabled = false
        rightAddressBar?.setSideButtonsHidden(true)
        rightAddressBar?.isUserInteractionEnabled = false
    }
    
    func setupAddressBarsExpandingOnTap() {
        let tapGestureRecognizer = UITapGestureRecognizer(target: self, action: #selector(addressBarScrollViewTapped))
        contentView.addressBarsScrollView.addGestureRecognizer(tapGestureRecognizer)
    }
}

// MARK: Action methods
private extension BrowserContainerViewController {
    @objc func cancelButtonTapped() {
        if currentAddressBar.textField.isFirstResponder {
            dismissKeyboard()
        }else{
            shouldHideHomePage = true
            tabViewControllers[safe: currentTabIndex]?.hideHomePageIfNeeded()
            setCancelButtonHidden(true)
        }
    }
    
    @objc func addressBarScrollViewTapped() {
        guard isCollapsed else { return }
        setupExpandingToolbarAnimator()
        expandingToolbarAnimator?.startAnimation()
        isCollapsed = false
    }
}

// MARK: BrowserAddressBarDelegate
extension BrowserContainerViewController: BrowserAddressBarDelegate {
    func addressBarReloadButtonClicked() {
        tabViewControllers[currentTabIndex].contentView.webView.reload()
        
    }
    
    func addressBarDidBeginEditing() {
        isAddressBarActive = true
    }
    
    func addressBar(_ addressBar: BrowserAddressBar, didReturnWithText text: String) {
        let tabViewController = tabViewControllers[currentTabIndex]
        let isLastTab = currentTabIndex == tabViewControllers.count - 1
        if isLastTab && !tabViewController.hasLoadedUrl {
            // if we started loading a URL and it is on the last tab then ->
            // open a hidden tab so that we can prepare it for new tab animation if the user swipes to the left
            openNewTab(isHidden: true)
        }
        if let url = self.viewModel.getURL(for: text) {
            addressBar.domainLabel.text = viewModel.getDomain(from: url)
            tabViewController.loadWebsite(from: url)
        }
        print("homepage alpha observe place --1: \(self.tabViewControllers[currentTabIndex].contentView.homePageView.alpha)")
        dismissKeyboard()
    }
}

extension BrowserContainerViewController: BrowserToolBarDelegate {
    
    func showMoreClicked() {
        let homePage = tabViewControllers[currentTabIndex].contentView.homePageView
        let sharable = tabViewControllers[currentTabIndex].contentView.webView.estimatedProgress > 0.2 && homePage.alpha == 0
        var menuItems: [OverlayShareView.Data] {
            let data1 = OverlayShareView.Data(image: UIImage(named: "ico_bottomtab_book_normal"), title: "书签", clickable: true)
            let data2 = OverlayShareView.Data(image: UIImage(named: "ico_menu_history_normal"), title: "历史记录", clickable: true)
            let data3 = OverlayShareView.Data(image: UIImage(named: "ico_menu_share_normal"), title: "分享", clickable: sharable)
            return [data1, data2, data3]
        }
        
        self.shareView.update(data: menuItems)
        
        UIApplication.pk.keyWindow?.present(overlay: self.shareOVC, options: .curveEaseOut)
    }
    
    func goBackClicked() {
        tabViewControllers[currentTabIndex].contentView.webView.goBack()
    }
    
    func goForwardClicked() {
        tabViewControllers[currentTabIndex].contentView.webView.goForward()
        
    }
    
    private func obtainUrlImagelink(on webView: WKWebView) -> String{
        var iconUrl = ""
        webView.evaluateJavaScript("document.querySelector(\'link[rel=\"icon\"]\').href", completionHandler:  { value, error in
            if (error != nil){
                print(error ?? "evaluateJavaScript error occur")
            }else{
                iconUrl = value! as! String
            }
        })
        return iconUrl
    }
    
    //添加历史记录
    func appendLinkToHistory(from webview: WKWebView) {
        
        
        guard let url = webview.url else { return }
        var title = url.host?.deletingPrefix("www.")
        if webview.title != nil {
            title = webview.title
        }
//        title = title ?? url.host?.deletingPrefix("www.")
        
//        if var title = webview.title} else { title = url.host?.deletingPrefix("www.")}
//        let title = webview.title!.count > 0 ? webview.title : url.host?.deletingPrefix("www.")
        let iconUrl = obtainUrlImagelink(on: webview)
        print("history icon: \(iconUrl)-----title:\(title) absoluteString:\(url.absoluteString)", "host: \(url.host)")

        
        sharedCachesMgr.appendLinkTocache(type: .history, iconString: iconUrl, title: title!, linkUrl:  url.absoluteString, completion:{})
    }
    //添加书签
        func appendLinkToBookmark(of webview: WKWebView) {
            
        }
    func clickAppendBookmark(){

        let webView = self.tabViewControllers[currentTabIndex].contentView.webView
        let iconUrl = obtainUrlImagelink(on: webView)
        let url = webView.url
        let title = webView.title ?? "书签"
        print("bookmark icon: \(iconUrl)-----title:\(title) absoluteString:\(url!.absoluteString)")
        
        let block = {
            for vc in self.tabViewControllers{
                vc.contentView.homePageView.reloadBookMarkView()
            }
            
        }
        
        if sharedCachesMgr.appendLinkTocache(type: .bookmark, iconString: iconUrl, title: title, linkUrl:  url!.absoluteString, completion:block){
            
            
            DispatchQueue.main.async {
                //update something
                print("-----******in BrowserContainerViewController clickAppendBookmark")

                var style = ToastStyle()
                
                // this is just one of many style options
                style.messageColor = .white
                style.backgroundColor = .gray
                
                // present the toast with the new style
                self.view.makeToast("已经添加到书签", duration: 3.0, position: .bottom, style: style)
            }
        }
    }
    
    
    func goHomePageClicked() {
        let currentContentView = self.tabViewControllers[self.currentTabIndex].contentView as BrowserTabContentView
        currentContentView.homePageView.alpha = 1
        self.currentAddressBar.domainLabel.text = ""
        self.setCancelButtonHidden(true)
        self.tabViewControllers[self.currentTabIndex].hasLoadedUrl = false
        self.currentAddressBar.updateStateWhenClicked(link: "")
        self.currentAddressBar.textField.activityState = .inactive
        currentContentView.webView.stopLoading()
        print("estimatedProgress 1---", currentContentView.webView.estimatedProgress)
        
        self.tabViewControllers[self.currentTabIndex].setNeedsStatusBarAppearanceUpdate()
        print("estimatedProgress 2---",currentContentView.webView.estimatedProgress)
        updateToolbarButtons()
        self.tabViewControllers[self.currentTabIndex].updateStatusBarBkColor()

        contentView.addressBars[currentTabIndex].setLoadingProgress(1, animated: true)
//        contentView.addressBars[currentTabIndex]?.setLoadingProgress(1, animated: true)

    }
    
    
    private func shareThePage() {
        // Setting url
        guard let shareLink = self.tabViewControllers[currentTabIndex].contentView.webView.url else{ return }
        
        let activityViewController : UIActivityViewController = UIActivityViewController(
            activityItems: [ shareLink ], applicationActivities: nil)
        
        // This line remove the arrow of the popover to show in iPad
        activityViewController.popoverPresentationController?.permittedArrowDirections = UIPopoverArrowDirection.down
        activityViewController.popoverPresentationController?.sourceRect = CGRect(x: 150, y: 150, width: 0, height: 0)
        
        // Pre-configuring activity items
        if #available(iOS 13.0, *) {
            activityViewController.activityItemsConfiguration = [
                UIActivity.ActivityType.message
            ] as? UIActivityItemsConfigurationReading
        } else {
            // Fallback on earlier versions
        }
        
        // Anything you want to exclude
        activityViewController.excludedActivityTypes = [
            UIActivity.ActivityType.postToWeibo,
            UIActivity.ActivityType.print,
            UIActivity.ActivityType.assignToContact,
            UIActivity.ActivityType.saveToCameraRoll,
            UIActivity.ActivityType.addToReadingList,
            UIActivity.ActivityType.postToFlickr,
            UIActivity.ActivityType.postToVimeo,
            UIActivity.ActivityType.postToTencentWeibo,
            UIActivity.ActivityType.postToFacebook
        ]
        
        if #available(iOS 13.0, *) {
            activityViewController.isModalInPresentation = true
        } else {
            // Fallback on earlier versions
        }
        self.present(activityViewController, animated: true, completion: nil)
    }
    
}
