//
//  NaviView.swift
//  DWebBrowser
//
//  Created by mac on 2022/6/16.
//

import UIKit
import SDWebImage
import SwiftyJSON

typealias ClickViewCallback = (String) -> Void

class NaviView: UIView {

    var titleString: String? {
        didSet {
            titleLabel.text = titleString
        }
    }
    
    var tineColor: String? {
        didSet {
            guard tineColor != nil else { return }
            titleLabel.textColor = UIColor(tineColor!)
            backButton.setTitleColor(UIColor(tineColor!), for: .normal)
        }
    }
    
    private var buttonList: [UIButton] = []
    private(set) var naviOverlay: Bool = false
    
    private var homePath: String?
    var buttons: [ButtonModel]? {
        didSet {
            guard buttons != nil else { return }
            for button in buttonList {
                button.removeFromSuperview()
            }
            buttonList.removeAll()
            
            let space: CGFloat = 16
            let width: CGFloat = 30
            
            let originX = self.frame.width - CGFloat(buttons!.count) * (width + space)
            
            for i in stride(from: 0, to: buttons!.count, by: 1) {
                let model = buttons![i]
                var button: UIButton
                
                let imageName = model.iconModel?.source ?? ""
                if model.iconModel?.type == "AssetIcon" {
                    button = UIButton(type: .custom)
                    if imageName.hasSuffix("svg") {
                        var imagePath: String = imageName
                        
                        if !imageName.hasPrefix("http") {
                            imagePath = URL(fileURLWithPath: homePath! + imageName).path
                            
                            button.setImage(UIImage.svgImageNamed(imagePath, size: CGSize(width: width, height: width)), for: .normal)
                        } else {
                            button.setImage(UIImage.svgImage(withURL: imagePath, size: CGSize(width: width, height: width)), for: .normal)
                        }
                    } else {
                        button.sd_setImage(with: URL(string: imageName), for: .normal)
                    }
                } else {
                    button = UIButton(type: .contactAdd)
                    button.setImage(UIImage(named: imageName), for: .normal)
                }
                button.tag = i
                button.isEnabled = !(model.disabled ?? false)
                button.addTarget(self, action: #selector(clickAction(sender:)), for: .touchUpInside)
              
                button.showsTouchWhenHighlighted = true
                button.frame = CGRect(x: originX + CGFloat(i) * (width + space), y: 7, width: width, height: width)
                self.addSubview(button)
                buttonList.append(button)
            }
        }
    }
                                            
    // 获取首页目录
    private func getHomePath() -> String {
        let controller = currentViewController() as? WebViewViewController
        
        if controller != nil {
            guard let appId = controller?.appId else {
                return ""
            }
            
            let homePath = documentdir + "/system-app/\(appId)/home"
            
            return homePath
        }
        
        return ""
    }
    
    var callback: ClickViewCallback?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        self.backgroundColor = .white
        self.addSubview(backButton)
        self.addSubview(titleLabel)
        
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    lazy private var backButton: UIButton = {
        let button = UIButton(type: .system)
        button.frame = CGRect(x: 16, y: 0, width: 50, height: self.frame.height)
        button.setImage(UIImage(named: "left_d"), for: .normal)
        button.contentHorizontalAlignment = .left
        button.addTarget(self, action: #selector(backAction), for: .touchUpInside)
        return button
    }()
    
    lazy private var titleLabel: UILabel = {
        let label = UILabel(frame: CGRect(x: (self.frame.width - 200) * 0.5, y: 0, width: 200, height: self.frame.height))
        label.font = UIFont.systemFont(ofSize: 18)
        label.textColor = .black
        label.textAlignment = .center
        return label
    }()
}

extension NaviView {
    
    @objc private func backAction() {
        let controller = currentViewController()
        
        if let controller = controller as? WebViewViewController {
            emitListenBackButton(controller: controller)
        }
        
        controller.navigationController?.popViewController(animated: true)
    }
    
    // 监听返回按钮事件
    func emitListenBackButton(controller: WebViewViewController) {
        controller.jsManager.handleEvaluateEmitScript(wb: "dweb-app", fun: "ListenBackButton", data: "{canGoBack:true}")
    }
    
    @objc private func clickAction(sender: UIButton) {
        guard buttons != nil, sender.tag < buttons!.count else { return }
        let model = buttons![sender.tag]
        let code = model.onClickCode ?? ""
        guard code.count > 0 else { return }
        callback?(code)
    }
}

extension NaviView {
    
    //更新naviView的是否隐藏
    func hiddenNavigationView(hidden: Bool) {
        self.isHidden = hidden
    }
    //返回naviView是否隐藏
    func naviHiddenState() -> Bool {
        return self.isHidden
    }
    //更新naviView的Overlay
    func updateNavigationBarOverlay(overlay: Bool) {
        self.naviOverlay = overlay
    }
    //获取naviView的Overlay
    func naviViewOverlay() -> Bool {
        return naviOverlay
    }
    //更新naviView的背景色
    func updateNavigationBarBackgroundColor(colorString: String) {
        self.backgroundColor = UIColor(colorString)
    }
    //返回naviView的背景色
    func backgroundColorString() -> String {
        return self.backgroundColor?.hexString() ?? "#FFFFFFFF"
    }
    //设置标题
    func setNaviViewTitle(title: String?) {
        self.titleString = title
    }
    //更新naviView的前景色
    func updateNavigationBarTintColor(colorString: String) {
        self.tineColor = colorString
    }
    //返回naviView的前景色
    func foregroundColor() -> String {
        return self.tineColor ?? ""
    }
    //返回naviView的标题
    func titleContent() -> String {
        return self.titleString ?? ""
    }
    //naviView是否有title
    func isTitleExit() -> Bool {
        return self.titleString != nil
    }
    //naviView的高度
    func viewHeight() -> CGFloat {
        return 44
    }
    
    //naviView透明度
    func viewAlpha() -> CGFloat {
        return self.alpha
    }
    //设置naviView透明度
    func setNaviViewAlpha(alpha: CGFloat) {
        self.alpha = alpha
    }
    
    //设置naviView的按钮
    func setNaviButtons(content: String) {
        guard let array = ChangeTools.stringValueArray(content) else { return }
        let list = JSON(array)
        let buttons = list.arrayValue.map { ButtonModel(dict: $0) }
        self.buttons = buttons
    }
    //返回naviView的按钮
    func naviActions() -> String {
        guard let buttons = self.buttons else { return "" }
        var array: [[String:Any]] = []
        for button in buttons {
            array.append(button.buttonDict)
        }
        let actionString = ChangeTools.arrayValueString(array) ?? ""
        return actionString
    }
}
