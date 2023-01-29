//
//  CustomConfirmPopView.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/6/24.
//

import UIKit

class CustomConfirmPopView: CustomBasePopView {

    var confirmModel: ConfirmConfiguration? {
        didSet {
            titleLabel.text = confirmModel?.title
            contentLabel.text = confirmModel?.content
            cancelButton.setTitle(confirmModel?.cancelText, for: .normal)
            confirmButton.setTitle(confirmModel?.confirmText, for: .normal)
            hiddenWhenTapBackgroundView(isCanTap: confirmModel?.dismissOnClickOutside ?? false)
        }
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        containView.addSubview(cancelButton)
        containView.addSubview(verlineView)
        addConstraint()
    }
    
    override func addConstraint() {
        super.addConstraint()
        
        cancelButton.snp.makeConstraints { make in
            make.left.equalTo(0)
            make.top.equalTo(self.lineView.snp.bottom)
            make.height.equalTo(40)
            make.width.equalToSuperview().multipliedBy(0.5)
            make.bottom.equalToSuperview()
        }
        
        verlineView.snp.makeConstraints { make in
            make.top.equalTo(self.lineView.snp.bottom)
            make.left.equalTo(self.cancelButton.snp.right).offset(-0.5)
            make.width.equalTo(0.5)
            make.height.equalTo(40)
        }
        
        confirmButton.snp.remakeConstraints { make in
            make.left.equalTo(self.cancelButton.snp.right)
            make.right.equalTo(0)
            make.top.equalTo(self.cancelButton)
            make.height.equalTo(40)
        }
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    lazy var cancelButton: UIButton = {
        let button = UIButton(type: .custom)
        button.setTitleColor(UIColor(red: 66/255.0, green: 138/255.0, blue: 243/255.0, alpha: 1.0), for: .normal)
        button.addTarget(self, action: #selector(cancelAction), for: .touchUpInside)
        return button
    }()
    
    lazy var verlineView: UIView = {
        let lineView = UIView()
        lineView.backgroundColor = UIColor("#DDDDDD")
        return lineView
    }()
    
    @objc func cancelAction() {
        hidden()
        callback?(.cancel)
    }
}

