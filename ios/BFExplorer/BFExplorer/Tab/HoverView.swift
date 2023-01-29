//
//  HoverView.swift
//  BFExplorer
//
//  Created by ui06 on 9/30/22.
//

import UIKit

class HoverView: UIView{
    let runView = UIView()
    var timer : Timer?
    var visible = false{
        didSet{
            if visible == true{
                timer?.invalidate()
                timer = nil
                isHidden = true
            }
        }
    }
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
        
    }
    
    let gradient = CAGradientLayer()
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setupView(){
        //addSubview(runView)
//        runView.snp.makeConstraints { make in
//            make.left.top.bottom.equalToSuperview()
//            make.width.equalTo(40)
//        }
        
        runView.layer.insertSublayer(gradient, at: 0)
        runView.backgroundColor = UIColor(white: 0.65, alpha: 1)
        
        gradient.frame = CGRect(x: 0, y: 0, width: 40, height: 20)
        gradient.colors = [UIColor.red, UIColor.blue]
        gradient.locations = [0.2,1.0]

        gradient.startPoint = CGPoint(x: 0, y: 0.5)
        gradient.startPoint = CGPoint(x: 1, y: 0.5)
        gradient.drawsAsynchronously = true
        
        backgroundColor = UIColor(white: 0.93, alpha: 1)
        
        UIView.animate(withDuration: 0.5) {
            self.runView.frame = CGRect(x: 160, y: 0, width: 40, height: 20)
        }
        layer.cornerRadius = 3
        layer.masksToBounds = true
        timer = Timer.scheduledTimer(withTimeInterval: 1.5, repeats: true) { timer in
            UIView.animate(withDuration: 1.0) {
                self.backgroundColor = UIColor(white: 0.9, alpha: 1)

            }completion: {_ in
                UIView.animate(withDuration: 0.5) {
                    self.backgroundColor = UIColor(white: 0.93, alpha: 1)
                }

            }
        }
    }
    
    
    
}

