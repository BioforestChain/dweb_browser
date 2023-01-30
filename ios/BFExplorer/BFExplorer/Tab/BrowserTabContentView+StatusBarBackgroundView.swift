//
//  BrowserTabContentView+StatusBarBackgroundView.swift
//  Browser
//
//    2.
//

import UIKit

extension BrowserTabContentView {
    class StatusBarBackgroundView: UIView {
        private let separatorView = UIView()
        
        override init(frame: CGRect) {
            super.init(frame: frame)
            setupView()
        }
        
        required init?(coder: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }
        
        func setColor(_ color: UIColor?) {
            backgroundColor = color ?? .white
            separatorView.isHidden = color != .white
        }
    }
}

// MARK: Helper methods
private extension BrowserTabContentView.StatusBarBackgroundView {
    func setupView() {
        backgroundColor = .red
        setupSeparatorView()
    }
    
    func setupSeparatorView() {
        separatorView.backgroundColor = UIColor(white: 0.95, alpha: 1)
        addSubview(separatorView)
        separatorView.snp.makeConstraints {
            $0.leading.bottom.trailing.equalToSuperview()
            $0.height.equalTo(0)
        }
    }
}
