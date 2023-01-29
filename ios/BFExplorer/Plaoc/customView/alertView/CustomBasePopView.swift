//
//  CustomBasePopView.swift
//  LoveHandbook
//
//  Created by xing on 2022/3/27.
//

import UIKit
import SnapKit

enum AlertClickType {
    case cancel
    case confirm
}

typealias ClickAlertTypeCallback = (AlertClickType) -> Void

class CustomBasePopView: UIView {

    private var isTap: Bool = false
    
    var callback: ClickAlertTypeCallback?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        self.addSubview(backgroundView)
        containView.addSubview(titleLabel)
        containView.addSubview(contentLabel)
        containView.addSubview(lineView)
        containView.addSubview(confirmButton)
        self.addSubview(containView)
        
    }
    
    func addConstraint() {
        
        containView.snp.makeConstraints { make in
            make.left.equalTo(64)
            make.right.equalTo(-64)
            make.center.equalTo(self.snp.center)
        }
        
        titleLabel.snp.makeConstraints { make in
            make.left.equalTo(16)
            make.right.equalTo(-16)
            make.top.equalTo(16)
        }
        
        contentLabel.snp.makeConstraints { make in
            make.left.equalTo(16)
            make.right.equalTo(-16)
            make.top.equalTo(self.titleLabel.snp.bottom).offset(8)
        }
        
        lineView.snp.makeConstraints { make in
            make.left.equalTo(0)
            make.right.equalTo(0)
            make.height.equalTo(0.5)
            make.top.equalTo(self.contentLabel.snp.bottom).offset(20)
        }
        
        confirmButton.snp.makeConstraints { make in
            make.left.right.equalTo(0)
            make.top.equalTo(self.lineView.snp.bottom)
            make.height.equalTo(40)
            make.bottom.equalToSuperview()
        }
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    lazy private var backgroundView: UIView = {
        let backView = UIView(frame: CGRect(x: 0, y: 0, width: screen_width, height: screen_height))
        backView.backgroundColor = UIColor.black.withAlphaComponent(0.3)
        backView.isUserInteractionEnabled = true
        let tap = UITapGestureRecognizer(target: self, action: #selector(tapAction))
        backView.addGestureRecognizer(tap)
        return backView
    }()

    lazy var containView: UIView = {

        let containView = UIView()
        containView.backgroundColor = .white
        containView.layer.cornerRadius = 12
        containView.layer.masksToBounds = true
        return containView
    }()
    
    lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.textColor = UIColor("#0C0A2B")
        label.textAlignment = .center
        label.font = UIFont.systemFont(ofSize: 15, weight: .medium)
        return label
    }()
    
    lazy var contentLabel: UILabel = {
        let label = UILabel()
        label.textColor = UIColor("#0C0A2B")
        label.textAlignment = .center
        label.font = UIFont.systemFont(ofSize: 15, weight: .regular)
        return label
    }()
    
    lazy var lineView: UIView = {
        let lineView = UIView()
        lineView.backgroundColor = UIColor("#DDDDDD")
        return lineView
    }()
    
    lazy var confirmButton: UIButton = {
        let button = UIButton(type: .custom)
        button.setTitleColor(UIColor(red: 66/255.0, green: 138/255.0, blue: 243/255.0, alpha: 1.0), for: .normal)
        button.addTarget(self, action: #selector(confirmAction), for: .touchUpInside)
        return button
    }()
    
}

extension CustomBasePopView {
    
    func show() {
        let app = UIApplication.shared.delegate as! AppDelegate
        let popWindow = app.window
        self.backgroundView.alpha = 0
        self.containView.transform = CGAffineTransform.init(scaleX: 0.1, y: 0.1)
        popWindow?.addSubview(self)

        UIView.animate(withDuration: 0.25) {
            self.backgroundView.alpha = 1
            self.containView.transform = .identity
        }
    }
    
    func hidden() {

        UIView.animate(withDuration: 0.25, animations: {
            self.backgroundView.alpha = 0
            self.containView.transform = CGAffineTransform.init(scaleX: 0.01, y: 0.01)
        }) { (finished) in
            self.removeFromSuperview()
        }
    }
    
    @objc func confirmAction() {
        hidden()
        callback?(.confirm)
    }
    
    @objc func tapAction() {
        guard isTap else { return }
        hidden()
    }
    
    func hiddenWhenTapBackgroundView(isCanTap: Bool) {
        isTap = isCanTap
    }
}

