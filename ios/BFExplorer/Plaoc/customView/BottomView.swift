//
//  BottomView.swift
//  DWebBrowser
//
//  Created by mac on 2022/6/16.
//

import UIKit
import SwiftyJSON

class BottomView: UIView {

    private let image_width: CGFloat = 30
    var hiddenBtn: Bool = false {
        didSet {
            for button in buttonList {
                button.isHidden = hiddenBtn
            }
        }
    }
    
    private var homePath: String?
    
    var buttons: [BottomBarModel]? {
        didSet {
            guard buttons != nil else { return }
            homePath = getHomePath()
            for button in buttonList {
                button.removeFromSuperview()
            }
            buttonList.removeAll()
            
            buttons?.enumerated().forEach { i, model in
                var button = UIButton(type: .custom)
                button.frame = CGRect(x: CGFloat(i) * self.frame.width / CGFloat(buttons!.count), y: 0, width: self.frame.width / CGFloat(buttons!.count), height: self.frame.height)
                button.tag = i

                button.isEnabled = !(model.disabled ?? false)
                button.isSelected = model.selected ?? false
                button.setTitle(model.titleString, for: .normal)
                button.titleLabel?.font = UIFont.systemFont(ofSize: 13)
                
                // 是否选中状态文字颜色
                var textColor = model.colors?.textColor ?? ""
                if textColor.isEmpty {
                    textColor = "#EEEEEE"
                }
                var textSelectedColor = model.colors?.textColorSelected ?? ""
                if textSelectedColor.isEmpty {
                    textSelectedColor = "#000000"
                }
                button.setTitleColor(UIColor(textColor), for: .normal)
                button.setTitleColor(UIColor(textSelectedColor), for: .selected)
                
                // 是否选中状态图片颜色
                setIconState(model: model, button: &button)
                
                button.addTarget(self, action: #selector(clickAction(sender:)), for: .touchUpInside)
                
                self.addSubview(button)
                buttonList.append(button)
                
            }
        }
    }
    
    private var buttonList: [UIButton] = []
    private(set) var bottomOverlay: Bool = false
    var callback: ClickViewCallback?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        self.backgroundColor = .white
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
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
    
    // 设置icon状态
    private func setIconState(model: BottomBarModel, button: inout UIButton) {
        let imageName = model.iconModel?.source ?? ""
        if model.iconModel?.type == "AssetIcon" {
            if imageName.hasSuffix("svg") {
                var imagePath: String = imageName
                
                if !imageName.hasPrefix("http") {
                    imagePath = URL(fileURLWithPath: homePath! + imageName).path
                    let color = model.colors?.iconColor ?? ""
                    let selectedColor = model.colors?.iconColorSelected ?? ""
                    
                    if !color.isEmpty {
                        button.setImage(UIImage.svgImage(withContentsOfFile: imagePath, size: CGSize(width: image_width, height: image_width)), for: .normal)
                    }
                    if !selectedColor.isEmpty {
                        button.setImage(UIImage.svgImage(withContentsOfFile: imagePath, size: CGSize(width: image_width, height: image_width)), for: .selected)
                    }
                } else {
                    button.setImage(UIImage.svgImage(withURL: imagePath, size: CGSize(width: image_width, height: image_width)), for: .normal)
                }
            }
        }
        
        button.imagePosition(style: .top, spacing: 8)
    }
    
    @objc private func clickAction(sender: UIButton) {
        guard buttons != nil, sender.tag < buttons!.count else { return }

        for i in stride(from: 0, to: buttonList.count, by: 1) {
            let button = buttonList[i]
            let model = buttons![i]
            var code = model.onClickCode ?? ""

            if sender.tag == i {
                sender.isSelected = true
                guard code.count > 0 else { return }
                code = model.onClickCode!.replacingOccurrences(of: ".dispatchEvent(new CustomEvent('click'))", with: ".setAttribute('selected', '');") + code
                callback?(code)
            } else {
                button.isSelected = false
                guard code.count > 0 else { return }
                code = model.onClickCode!.replacingOccurrences(of: ".dispatchEvent(new CustomEvent('click'))", with: ".removeAttribute('selected')")
                callback?(code)
            }
        }
    }
}

extension BottomView {
    //隐藏底部
    func hiddenBottomView(hidden: Bool) {
        self.isHidden = hidden
    }
    //返回底部是否隐藏
    func bottomHiddenState() -> Bool {
        return self.isHidden
    }
    //更新底部overlay
    func updateBottomViewOverlay(overlay: Bool) {
        self.bottomOverlay = overlay
    }
    //返回底部overlay
    func bottomViewOverlay() -> Bool {
        return bottomOverlay
    }
    //设置底部alpha
    func updaterBottomViewAlpha(alpha: CGFloat) {
        self.alpha = alpha
    }
    //返回底部alpha
    func bottomViewAlpha() -> CGFloat {
        return self.alpha
    }
    //更新底部背景色
    func updateBottomViewBackgroundColor(colorString: String) {
        self.backgroundColor = UIColor(colorString)
    }
    //返回底部背景颜色
    func bottomBarBackgroundColor() -> String {
        return self.backgroundColor?.hexString() ?? "#FFFFFFFF"
    }
    //更新底部颜色
    func updateBottomViewforegroundColor(colorString: String) {
        //TODO
    }
    //返回底部颜色
    func bottomBarForegroundColor() -> String {
        //TODO
        return "#FFFFFFFF"
    }
    //返回底部高度
    func bottomViewHeight() -> CGFloat {
        return self.frame.height
    }
    //隐藏底部按钮
    func hiddenBottomViewButton(hidden: Bool) {
        self.hiddenBtn = hidden
    }
    
    //更新底部按钮
    func updateBottomButtons(content: String) {
        guard let array = ChangeTools.stringValueArray(content) else { return }
        let list = JSON(array)
        let buttons = list.arrayValue.map { BottomBarModel(dict: $0) }
        self.buttons = buttons
    }
    //返回底部按钮数组
    func bottomActions() -> String {
        guard let buttons = self.buttons else { return "" }
        var array: [[String:Any]] = []
        for button in buttons {
            array.append(button.buttonDict)
        }
        let actionString = ChangeTools.arrayValueString(array) ?? ""
        return actionString
    }
}

