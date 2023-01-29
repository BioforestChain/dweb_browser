//
//  WebViewViewController.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/6/20.
//

import UIKit
import WebKit
import UIColor_Hex_Swift
import SwiftyJSON
import JavaScriptCore

class WebViewViewController: UIViewController {

    var urlString: String = ""
    var appId: String = ""
    private var isStatusHidden: Bool = false
    private var statusOverlay: Bool = false
    private var keyboardOverlay: Bool = false
    private var style: UIStatusBarStyle = .default
    var isKeyboardShow: Bool = false
    var keyboardHeight: CGFloat = 0
    var keyboardSafeArea: UIEdgeInsets = .zero
    
    var jsManager: JSCoreManager!
    
    
    private let jsContext = JSContext()
    
    
    override var preferredStatusBarStyle: UIStatusBarStyle {
        return style
    }
    
    override var prefersStatusBarHidden: Bool {
        return isStatusHidden
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
      
        self.navigationController?.isNavigationBarHidden = true
        
        self.view.addSubview(webView)
        self.view.addSubview(naviView)
        self.view.addSubview(statusView)
        self.view.addSubview(bottomView)
        
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        // 开启网络监听
        ReachabilityManager.shared.startMonitoring()
        
        jsManager = JSCoreManager.init(appId: appId, controller: self)
        webView.jsManager = jsManager
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        NotificationCenter.default.removeObserver(self)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()

        self.view.backgroundColor = .white
        
//        webView.openWebView(html: urlString)
        
        NotificationCenter.default.addObserver(self, selector: #selector(interceptAction(noti:)), name: NSNotification.Name.interceptNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(observerShowKeyboard(noti:)), name: UIResponder.keyboardWillShowNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(observerHiddenKeyboard(noti:)), name: UIResponder.keyboardWillHideNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(openDwebAction(noti:)), name: NSNotification.Name.openDwebNotification, object: nil)
        
    }
    
    //释放时，停止网络监听
    func dealloc() {
        ReachabilityManager.shared.stopMonitoring()
    }
    
    @objc private func openDwebAction(noti: Notification) {
        guard let info = noti.userInfo as? [String:String] else { return }
        guard let urlString = info["param"] else { return }
        webView.openWebView(html: urlString)
    }
    
    @objc private func interceptAction(noti: Notification) {
        let info = noti.userInfo as? [String:Any]
        let function = info?["function"] as? String
    }
    
    
    
    //拦截后，重新把数据写入请求
    private func rewriteUrlSchemeTaskResponse(info: [String:Any]?, content: String) {
        guard let urlSchemeTask = info?["scheme"] as? WKURLSchemeTask else { return }
        guard urlSchemeTask.request.url != nil else { return }
        guard let data = content.data(using: .utf8) else { return }
        let type = "text/html"
        let response = URLResponse(url: urlSchemeTask.request.url!, mimeType: type, expectedContentLength: data.count, textEncodingName: nil)
        urlSchemeTask.didReceive(response)
        urlSchemeTask.didReceive(data)
        urlSchemeTask.didFinish()
    }

    lazy var statusView: StatusView = {
        let statusView = StatusView(frame: CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: UIDevice.current.statusBarHeight()))
        return statusView
    }()
    
    lazy private var naviView: NaviView = {
        let naviView = NaviView(frame: CGRect(x: 0, y: self.statusView.frame.maxY, width: UIScreen.main.bounds.width, height: 44))
        naviView.callback = { [weak self] code in
            guard let strongSelf = self else { return }
            strongSelf.webView.handleJavascriptString(inputJS: code)
        }
        return naviView
    }()
    
    lazy private var webView: CustomWebView = {
        let webView = CustomWebView(frame: CGRect(x: 0, y: 44, width: self.view.bounds.width, height: UIScreen.main.bounds.height - 44), jsNames: ["console","network"], appId: appId, urlString: self.urlString)
        webView.superVC = self
        webView.callback = { [weak self] title in
            guard let strongSelf = self else { return }
            strongSelf.naviView.titleString = title
        }
        return webView
    }()
    
    lazy private var bottomView: BottomView = {
        let bottomView = BottomView(frame: CGRect(x: 0, y: UIScreen.main.bounds.height - 49 - UIDevice.current.tabbarSpaceHeight(), width: UIScreen.main.bounds.width, height: 49 + UIDevice.current.tabbarSpaceHeight()))
//        bottomView.isHidden = true
        bottomView.callback = { [weak self] code in
            guard let strongSelf = self else { return }
            strongSelf.webView.handleJavascriptString(inputJS: code)
        }
        return bottomView
    }()

}

extension WebViewViewController {
    
    func evaluateJavaScript(jsString: String) {
        webView.handleJavascriptString(inputJS: jsString)
    }
}

// naviBar和js的交互
extension WebViewViewController {
    //更新naviView的是否隐藏
    func hiddenNavigationBar(isHidden: Bool) {
        naviView.hiddenNavigationView(hidden: isHidden)
    }
    //返回naviView是否隐藏
    func getNaviHiddenState() -> Bool {
        return naviView.naviHiddenState()
    }
    
    //更新naviView的Overlay
    func updateNavigationBarOverlay(overlay: Bool) {
        guard naviView.naviOverlay != overlay else { return }
        naviView.updateNavigationBarOverlay(overlay: overlay)
        var frame = webView.frame
        if overlay {
            frame.origin.y = UIDevice.current.statusBarHeight() + 44
            frame.size.height -= 44
        } else {
            frame.origin.y = UIDevice.current.statusBarHeight()
            frame.size.height += 44
        }
        UIView.animate(withDuration: 0.25) {
            self.webView.frame = frame
            self.webView.updateFrame(frame: frame)
        }
    }
    //获取naviView的Overlay
    func naviViewOverlay() -> Bool {
        return naviView.naviViewOverlay()
    }
    
    //更新naviView的背景色
    func updateNavigationBarBackgroundColor(colorString: String) {
        naviView.updateNavigationBarBackgroundColor(colorString: colorString)
    }
    //返回naviView的背景色
    func naviViewBackgroundColor() -> String {
        return naviView.backgroundColorString()
    }
    //更新naviView的前景色
    func updateNavigationBarTintColor(colorString: String) {
        naviView.updateNavigationBarTintColor(colorString: colorString)
    }
    //返回naviView的前景色
    func naviViewForegroundColor() -> String {
        return naviView.foregroundColor()
    }
    
    //设置标题
    func setNaviViewTitle(title: String?) {
        naviView.setNaviViewTitle(title: title)
    }
    //返回naviView的标题
    func titleString() -> String {
        return naviView.titleContent()
    }
    //naviView是否有title
    func isNaviTitleExit() -> Bool {
        return naviView.isTitleExit()
    }
    //naviView的高度
    func naviViewHeight() -> CGFloat {
        return naviView.viewHeight()
    }
    
    //naviView透明度
    func naviViewAlpha() -> CGFloat {
        return naviView.viewAlpha()
    }
    //设置naviView透明度
    func setNaviViewAlpha(alpha: CGFloat) {
        naviView.setNaviViewAlpha(alpha: alpha)
    }
    
    //设置naviView的按钮
    func setNaviButtons(content: String) {
        naviView.setNaviButtons(content: content)
    }
    //返回naviView的按钮
    func naviActions() -> String {
        return naviView.naviActions()
    }
}

// statusBar和js的交互
extension WebViewViewController {
    //更新状态栏背景色
    func updateStatusBackgroundColor(colorString: String) {
        statusView.backgroundColor = UIColor(colorString)
    }
    //状态栏背景色
    func statusBackgroundColor() -> String {
        return statusView.backgroundColor?.hexString() ?? "#FFFFFFFF"
    }
    
    //更新状态栏状态
    func updateStatusStyle(style: String) {
        if style == "default" {
            self.style = .default
        } else {
            self.style = .lightContent
        }
        setNeedsStatusBarAppearanceUpdate()
    }
    //返回状态栏状态
    func statusBarStyle() -> String {
        if style == .default {
            return "true"
        } else {
            return "false"
        }
    }
    
    //状态栏是否隐藏
    func updateStatusHidden(isHidden: Bool) {
        isStatusHidden = isHidden
        setNeedsStatusBarAppearanceUpdate()
    }
    //返回状态栏是否隐藏
    func statusBarVisible() -> Bool {
        return isStatusHidden
    }
    
    //更新状态栏Overlay
    func updateStatusBarOverlay(overlay: Bool) {
        guard statusOverlay != overlay else { return }
        statusOverlay = overlay
        var naviFrame = naviView.frame
        var webFrame = webView.frame
        if overlay {
            naviFrame.origin.y -= UIDevice.current.statusBarHeight()
            webFrame.size.height += UIDevice.current.statusBarHeight()
        } else {
            naviFrame.origin.y = UIDevice.current.statusBarHeight()
            webFrame.size.height -= UIDevice.current.statusBarHeight()
        }
        webFrame.origin.y = naviFrame.maxY
        UIView.animate(withDuration: 0.25) {
            self.naviView.frame = naviFrame
            self.webView.frame = webFrame
            self.webView.updateFrame(frame: webFrame)
        }
    }
    
    //返回状态栏Overlay
    func statusBarOverlay() -> Bool {
        return statusOverlay
    }
}
// bottomBar和js的交互
extension WebViewViewController {
    //隐藏底部
    func hiddenBottomView(isHidden: Bool) {
        bottomView.hiddenBottomView(hidden: isHidden)
    }
    //返回底部是否隐藏
    func bottombarHidden() -> Bool {
        return bottomView.bottomHiddenState()
    }
    
    //更新底部overlay
    func updateBottomViewOverlay(overlay: Bool) {
        guard bottomView.bottomViewOverlay() != overlay else { return }
        bottomView.updateBottomViewOverlay(overlay: overlay)
        
        var frame = webView.frame
        
        if overlay {
            frame.size.height += 49 + UIDevice.current.tabbarSpaceHeight()
        } else {
            frame.size.height -= 49 + UIDevice.current.tabbarSpaceHeight()
        }
       
        frame = CGRect(x: 0, y: self.naviView.frame.maxY, width: self.view.bounds.width, height: UIScreen.main.bounds.height - self.naviView.frame.maxY - 49 - UIDevice.current.tabbarSpaceHeight() + 100)
        UIView.animate(withDuration: 0.25) {
            self.webView.frame = frame
            self.webView.updateFrame(frame: frame)
        }
    }
    //返回底部overlay
    func bottombarOverlay() -> Bool {
        return bottomView.bottomViewOverlay()
    }
    //设置底部alpha
    func setBottomViewAlpha(alpha: CGFloat) {
        bottomView.updaterBottomViewAlpha(alpha: alpha)
    }
    //返回底部alpha
    func bottomViewAlpha() -> CGFloat {
        return bottomView.bottomViewAlpha()
    }
    
    //更新底部背景色
    func updateBottomViewBackgroundColor(colorString: String) {
        bottomView.updateBottomViewBackgroundColor(colorString: colorString)
    }
    //返回底部背景颜色
    func bottomBarBackgroundColor() -> String {
        return bottomView.bottomBarBackgroundColor()
    }
    
    //更新底部颜色
    func updateBottomViewforegroundColor(colorString: String) {
        bottomView.updateBottomViewforegroundColor(colorString: colorString)
    }
    //返回底部颜色
    func bottomBarForegroundColor() -> String {
        return bottomView.bottomBarForegroundColor()
    }
    
    //更新底部高度
    func updateBottomViewHeight(height: CGFloat) {
        var frame = bottomView.frame
        frame.size.height = height
        frame.origin.y = UIScreen.main.bounds.height - height
        bottomView.frame = frame
        UIView.animate(withDuration: 0.25) {
            self.bottomView.frame = frame
        }
    }
    //返回底部高度
    func bottomViewHeight() -> CGFloat {
        return bottomView.bottomViewHeight()
    }
    
    //隐藏底部按钮
    func hiddenBottomViewButton(hidden: Bool) {
        bottomView.hiddenBottomViewButton(hidden: hidden)
    }
    
    //更新底部按钮
    func updateBottomButtons(content: String) {
        bottomView.updateBottomButtons(content: content)
    }
    //返回底部按钮数组
    func bottomActions() -> String {
        return bottomView.bottomActions()
    }
}

extension WebViewViewController {
    
    @objc private func observerShowKeyboard(noti: Notification) {
        
        guard let keyboardBound = noti.userInfo?["UIKeyboardFrameEndUserInfoKey"] as? CGRect else { return }
        isKeyboardShow = true
        keyboardHeight = keyboardBound.size.height
        keyboardSafeArea = UIEdgeInsets(top: 0, left: 0, bottom: keyboardBound.height, right: 0)
        
        /**
         ([AnyHashable("UIKeyboardAnimationCurveUserInfoKey"): 7,
         AnyHashable("UIKeyboardBoundsUserInfoKey"): NSRect: {{0, 0}, {375, 380}},
         AnyHashable("UIKeyboardCenterBeginUserInfoKey"): NSPoint: {187.5, 1002},
         AnyHashable("UIKeyboardIsLocalUserInfoKey"): 1,
         AnyHashable("UIKeyboardFrameEndUserInfoKey"): NSRect: {{0, 432}, {375, 380}},
         AnyHashable("UIKeyboardFrameBeginUserInfoKey"): NSRect: {{0, 812}, {375, 380}},
         AnyHashable("UIKeyboardAnimationDurationUserInfoKey"): 0.25,
         AnyHashable("UIKeyboardCenterEndUserInfoKey"): NSPoint: {187.5, 622}])
         */
    }
    @objc private func observerHiddenKeyboard(noti: Notification) {
        isKeyboardShow = false
        keyboardHeight = 0
        keyboardSafeArea = .zero
    }
    
    func setKeyboardOverlay(overlay: Bool) {
        keyboardOverlay = overlay
    }
    
    func isKeyboardOverlay() -> Bool {
        return keyboardOverlay
    }
}

extension WebViewViewController {
    
    private func openBeforeUnloadAction(info: [String:Any]?, content: String) {
        guard let bodyDict = ChangeTools.stringValueDic(content) else { return }
        let confirmModel = ConfirmConfiguration(dict: JSON(bodyDict))
        let alertView = CustomConfirmPopView(frame: CGRect(x: 0, y: 0, width: screen_width, height: screen_height))
        alertView.confirmModel = confirmModel
        alertView.callback = { [weak self] type in
            guard let strongSelf = self else { return }
            var jsString: String = ""
            if type == .confirm {
                jsString = confirmModel.confirmFunc ?? ""
            } else if type == .cancel {
                jsString = confirmModel.cancelFunc ?? ""
            }
            guard jsString.count > 0 else { return }
            strongSelf.rewriteUrlSchemeTaskResponse(info: info, content: jsString)
        }
        alertView.show()
    }
}
