//
//  TouchView.swift
//  Browser
//
//  Created by ui06 on 2022/8/31.
//

import UIKit
import SwiftUI
class TouchView: UIButton{
    let realImageView = UIImageView()
    let realTitleLabel = UILabel()
    let maskButton = UIButton()
    let redSpot = UIView()
    
    var clickable:Bool = true{
        didSet{
            let image = realImageView.image?.withRenderingMode(.alwaysTemplate)
            realImageView.image = image
            realImageView.tintColor = clickable ? UIColor(hexColor: "0A1626") : UIColor(hexColor: "ACB5BF")
            maskButton.isEnabled = clickable
        }
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
    }

    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setupView(){
        
        let imageWidth = frame.width * 0.78 - 3
        let leftInset = frame.width * 0.1
        let titleHeight = frame.width * 0.2
        
        realImageView.frame = CGRect(x: leftInset, y: 0, width: imageWidth, height: imageWidth)
        realTitleLabel.frame = CGRect(x: 0, y: imageWidth + 5, width: frame.width, height: titleHeight)

        realTitleLabel.textAlignment = .center
        realTitleLabel.font = .systemFont(ofSize: 13)
        addSubview(realImageView)
        addSubview(realTitleLabel)

        realImageView.layer.cornerRadius = 12
        realImageView.layer.masksToBounds = true
        
        redSpot.frame = CGRect(x: frame.maxX-18, y: -2, width: 12, height: 12)
        redSpot.backgroundColor = .systemRed
        redSpot.layer.cornerRadius = 6
        redSpot.layer.masksToBounds = true
        redSpot.isHidden = true

        addSubview(redSpot)

    }
    
    func hideRedSpot(_ hide: Bool){
        redSpot.isHidden = hide
    }
    
    func setImage(image:UIImage){
        realImageView.image = image
    }
    func setTitle(text:String){
        realTitleLabel.text = text
    }
}


extension TouchView{
    
    func setupMenu(){
        
        maskButton.frame = bounds
        maskButton.backgroundColor = .clear
        addSubview(maskButton)
        
        let rect = CGRect(x: 0, y: 0, width: screen_w/7, height: screen_w/7)
        let whiteView = UIView(frame: rect)
        whiteView.backgroundColor = .white
        whiteView.center = self.realImageView.center
        self.insertSubview(whiteView, at: 0)
        whiteView.layer.cornerRadius = 10
        whiteView.layer.masksToBounds = true
        
        let imgVRect = realImageView.frame
        
        let point = CGPoint(x: imgVRect.origin.x + 7, y:imgVRect.origin.y + 7)
        let size = CGSize(width: imgVRect.width - 14, height: imgVRect.height - 14)
        
        realImageView.frame = CGRect(origin: point, size: size)

        let orgRect = self.realTitleLabel.frame
        self.realTitleLabel.frame = CGRect(origin: CGPoint(x: orgRect.origin.x, y: whiteView.frame.maxY + 8), size: orgRect.size)
    }
    

}
