//
//  CustomAlertPopView.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/6/24.
//

import UIKit

class CustomAlertPopView: CustomBasePopView {

    var alertModel: AlertConfiguration? {
        didSet {
            titleLabel.text = alertModel?.title
            contentLabel.text = alertModel?.content
            confirmButton.setTitle(alertModel?.confirmText, for: .normal)
            hiddenWhenTapBackgroundView(isCanTap: alertModel?.dismissOnClickOutside ?? false)
        }
    }
    override init(frame: CGRect) {
        super.init(frame: frame)
        addConstraint()
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
