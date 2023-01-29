//
//  OverlayShareView.swift
//  OverlayController
//
//  Created by zhanghao on 2020/2/17.
//  Copyright © 2020 zhanghao. All rights reserved.
//

import UIKit

public protocol OverlayShareViewDelegate {
    func overlayShareView(_ shareView: OverlayShareView, selectedType:PageType)
}

public class OverlayShareView: UIView{
    
    public var delegate: OverlayShareViewDelegate?
    var collectionLayout = UICollectionViewFlowLayout()
    var collectionView: UICollectionView!
    var sectionIndex: Int!
    var touches = [TouchView]()
    
    public struct Data {
        var image: UIImage?
        var title: String?
        var clickable: Bool
    }
    
    public private(set) var dataList: [Data]?
    private var tableView: UITableView!
    private var blurView: UIVisualEffectView!
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        blurView = UIVisualEffectView(effect: UIBlurEffect(style: .extraLight))
        addSubview(blurView)
        setupTouches()
        backgroundColor = UIColor(hexColor: "F5F6F7")
    }
    public func update(data: [Data]?) {
        self.dataList = data
        for (i,item) in dataList!.enumerated(){
            let touch = touches[i]
            touch.realImageView.image =  item.image
            touch.realTitleLabel.text =  item.title
            touch.clickable = item.clickable
        }
        setNeedsLayout()
    }
    func setupTouches(){
        let width = screen_w / 6.5
        let horzGap = width
        for i in 0...2{
            let touch = TouchView(frame: CGRect(x: horzGap + CGFloat(i)*(horzGap + width), y: 50, width: width, height: width+5))
            touches.append(touch)
            self.addSubview(touch)
            touch.setupMenu()
            
            touch.maskButton.tag = i
            touch.maskButton.addTarget(self, action: #selector(itemClicked), for: .touchUpInside)
        }
    }
    
    @objc func itemClicked(sender:UIButton){
        self.delegate?.overlayShareView(self, selectedType: PageType(rawValue: sender.tag)!)
    }
    
    required init?(coder: NSCoder) { super.init(coder: coder) }
    
}
