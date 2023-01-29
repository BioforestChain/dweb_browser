//
//  Configuration.swift
//  EmptyDataSet-Swift
//
//  Created by YZF on 2018/2/24.
//  Copyright © 2018年 Xiaoye. All rights reserved.
//

import Foundation
import UIKit

struct Configuration {
    
    var type: PageType
//    var isLoading = false
    
//    weak var controller: UITableViewController!
    
    
    
    init(_ pageType: PageType) {
        type = pageType
//        self.controller = controller
    }
    
    var titleString: NSAttributedString? {
        var text = ["暂无书签", "暂无记录"][type.rawValue]
        var font = UIFont.init(name: "HelveticaNeue-Light", size: 22)!
        var textColor = UIColor(hexColor: "c9c9c9")
        
//        switch type {
//        case .bookmark:
//            text = "暂无书签"
//        case .history:
//            <#code#>
//        case .share:
//            <#code#>
//        } {
//        case .:
//            text = "暂无书签"
//            font = UIFont.init(name: "HelveticaNeue-Light", size: 22)!
//            textColor = UIColor(hexColor: "c9c9c9")
            
//        case .Camera:
//            text = "Please Allow Photo Access"
//            font = UIFont.boldSystemFont(ofSize: 18)
//            textColor = UIColor(hexColor: "5f6978")
//
//        case .Dropbox:
//            text = "Star Your Favorite Files"
//            font = UIFont.boldSystemFont(ofSize: 17.0)
//            textColor = UIColor(hexColor: "25282b")
//
//        case .Facebook:
//            text = "No friends to show."
//            font = UIFont.boldSystemFont(ofSize: 22.0)
//            textColor = UIColor(hexColor: "acafbd")
//
//        case .Fancy:
//            text = "No Owns yet"
//            font = UIFont.boldSystemFont(ofSize: 14.0)
//            textColor = UIColor(hexColor: "494c53")
//
//        case .iCloud:
//            text = "iCloud Photo Sharing"
//
//        case .Instagram:
//            text = "Instagram Direct"
//            font = UIFont.init(name: "HelveticaNeue-Light", size: 26)!
//            textColor = UIColor(hexColor: "444444")
//
//        case .iTunesConnect:
//            text = "No Favorites"
//            font = UIFont.systemFont(ofSize: 16)
//
//        case .Kickstarter:
//            text = "Activity empty"
//            font = UIFont.boldSystemFont(ofSize: 16.0)
//            textColor = UIColor(hexColor: "828587")
//
//        case .Path:
//            text = "Message Your Friends"
//            font = UIFont.boldSystemFont(ofSize: 14.0)
//            textColor = UIColor.white
//
//        case .Pinterest:
//            text = "No boards to display"
//            font = UIFont.boldSystemFont(ofSize: 18.0)
//            textColor = UIColor(hexColor: "666666")
//
//        case .Photos:
//            text = "No Photos or Videos"
//
//        case .Podcasts:
//            text = "No Podcasts"
//
//        case .Remote:
//            text = "Cannot Connect to a Local Network"
//            font = UIFont.init(name: "HelveticaNeue-Medium", size: 18)
//            textColor = UIColor(hexColor: "555555")
//
//        case .Tumblr:
//            text = "This is your Dashboard."
//            font = UIFont.boldSystemFont(ofSize: 18.0)
//            textColor = UIColor(hexColor: "aab6c4")
//
//        case .Twitter:
//            text = "No lists"
//            font = UIFont.boldSystemFont(ofSize: 14.0)
//            textColor = UIColor(hexColor: "292f33")
//
//        case .Vesper:
//            text = "No Notes"
//            font = UIFont.init(name: "IdealSans-Book-Pro", size: 16)
//            textColor = UIColor(hexColor: "d9dce1")
//
//        case .Videos:
//            text = "AirPlay"
//            font = UIFont.systemFont(ofSize: 17)
//            textColor = UIColor(hexColor: "414141")
//
//        case .Vine:
//            text = "Welcome to VMs"
//            font = UIFont.boldSystemFont(ofSize: 22.0)
//            textColor = UIColor(hexColor: "595959")
//
//        case .Whatsapp:
//            text = "No Media"
//            font = UIFont.systemFont(ofSize: 20)
//            textColor = UIColor(hexColor: "808080")
//
//        case .WWDC:
//            text = "No Favorites"
//            font = UIFont.init(name: "HelveticaNeue-Medium", size: 16)
//            textColor = UIColor(hexColor: "b9b9b9")
//        default:
//            break
//        }
//
        if text == nil {
            return nil
        }
        var attributes: [NSAttributedString.Key: Any] = [:]
        if font != nil {
            attributes[NSAttributedString.Key.font] = font
        }
        if textColor != nil {
            attributes[NSAttributedString.Key.foregroundColor] = textColor
        }
        return NSAttributedString.init(string: text, attributes: attributes)
    }
    
    
    var detailString: NSAttributedString? {
        var text: String?
        var font: UIFont?
        var textColor: UIColor?
        
            text = "" //"When you have messages, you’ll see them here."
            font = UIFont.systemFont(ofSize: 13.0)
            textColor = UIColor(hexColor: "cfcfcf")
            
        if text == nil {
            return nil
        }
        var attributes: [NSAttributedString.Key: Any] = [:]
        if font != nil {
            attributes[NSAttributedString.Key.font] = font!
        }
        if textColor != nil {
            attributes[NSAttributedString.Key.foregroundColor] = textColor
        }
        return NSAttributedString.init(string: text!, attributes: attributes)
    }
    
    var image: UIImage? {
//        if isLoading {
//            return UIImage.init(named: "ico_blank_bookmarkr")
//        } else {
////            let imageNamed = ("placeholder_" + app.display_name!).lowercased().replacingOccurrences(of: " ", with: "_")
        return UIImage.init(named: type == .history ? "ico_blank_history" : "ico_blank_bookmark")
//        }
    }
    
    var imageAnimation: CAAnimation? {
        let animation = CABasicAnimation.init(keyPath: "transform")
        animation.fromValue = NSValue.init(caTransform3D: CATransform3DIdentity)
        animation.toValue = NSValue.init(caTransform3D: CATransform3DMakeRotation(.pi/2, 0.0, 0.0, 1.0))
        animation.duration = 0.25
        animation.isCumulative = true
        animation.repeatCount = MAXFLOAT
        
        return nil// animation;
    }
    
    
    func buttonTitle(_ state: UIControl.State) -> NSAttributedString? {
        var text: String?
        var font: UIFont?
        var textColor: UIColor?
            text = "Start Browsing";
            font = UIFont.boldSystemFont(ofSize: 16)
            textColor = UIColor(hexColor: state == .normal ? "05adff" : "6bceff" )
            
        if text == nil {
            return nil
        }
        var attributes: [NSAttributedString.Key: Any] = [:]
        if font != nil {
            attributes[NSAttributedString.Key.font] = font!
        }
        if textColor != nil {
            attributes[NSAttributedString.Key.foregroundColor] = textColor
        }
        text = ""
        return NSAttributedString.init(string: text!, attributes: attributes)
    }
    
    func buttonBackgroundImage(_ state: UIControl.State) -> UIImage? {
        var imageName = "button_background_".lowercased()
        
        if state == .normal {
            imageName = imageName + "_normal"
        }
        if state == .highlighted {
            imageName = imageName + "_highlight"
        }
        
        var capInsets = UIEdgeInsets(top: 10, left: 10, bottom: 10, right: 10)
        var rectInsets = UIEdgeInsets.zero
        
//        switch app {
//        case .Foursquare:
            capInsets = UIEdgeInsets(top: 25, left: 25, bottom: 25, right: 25)
            rectInsets = UIEdgeInsets(top: 0, left: 10, bottom: 0, right: 10)
//        case .iCloud:
//            rectInsets = UIEdgeInsets(top: -19, left: -61, bottom: -19, right: -61)
//        case .Kickstarter:
//            capInsets = UIEdgeInsets(top: 22, left: 22, bottom: 22, right: 22)
//            rectInsets = UIEdgeInsets(top: 0, left: -20, bottom: 0, right: -20)
//        default:
//            break;
//        }
        
        let image = UIImage.init(named: "ico_blank_bookmark")
        
        return image?.resizableImage(withCapInsets: capInsets, resizingMode: .stretch).withAlignmentRectInsets(rectInsets)
    }
    
    var backgroundColor: UIColor? {
        return.clear
//        return UIColor(hexColor: "eceef7")
    }
    
    var verticalOffset: CGFloat {
        return 0

//        switch app  {
//        case .Kickstarter:
//            var offset = UIApplication.shared.statusBarFrame.height
//            offset += (controller.navigationController?.navigationBar.frame.height)!
//            return -offset
//        case .Twitter:
//            return -(CGFloat)(roundf(Float(controller.tableView.frame.height/2.5)))
//        default:
//            return 0
//        }
    }
    
    var spaceHeight: CGFloat {
        return 24.0
//        switch app {
//        case .Airbnb:         return 24.0
//        case .AppStore:       return 34.0
//        case .Facebook:       return 30.0
//        case .Fancy:          return 1.0
//        case .Foursquare:     return 9.0
//        case .Instagram:      return 24.0
//        case .iTunesConnect:  return 9.0
//        case .Kickstarter:    return 15.0
//        case .Path:           return 1.0
//        case .Podcasts:       return 35.0
//        case .Tumblr:         return 10.0
//        case .Twitter:        return 0.1
//        case .Vesper:         return 22.0
//        case .Videos:         return 0.1
//        case .Vine:           return 0.1
//        case .WWDC:           return 18.0
//        default:              return 0.0
//        }
    }
    
}

//extension Configuration {
//    // Configuration NavigationBar
//    func configureNavigationBar() {
//        var barColor: UIColor? = nil
//        var tintColor: UIColor? = nil
//
////        self.controller.navigationController?.navigationBar.titleTextAttributes = nil
//
//        switch app {
//        case .Airbnb:
//            barColor = UIColor(hexColor: "f8f8f8")
//            tintColor = UIColor(hexColor: "08aeff")
//        case .Camera:
//            barColor = UIColor(hexColor: "595959")
//            tintColor = UIColor.white
//            self.controller.navigationController?.navigationBar.titleTextAttributes = [NSAttributedString.Key.foregroundColor: tintColor!]
//        case .Dropbox:
//            barColor = UIColor.white
//            tintColor = UIColor(hexColor: "007ee5")
//        case .Facebook:
//            barColor = UIColor(hexColor: "506da8")
//            tintColor = UIColor.white
//        case .Fancy:
//            barColor = UIColor(hexColor: "353b49")
//            tintColor = UIColor(hexColor: "c4c7cb")
//        case .Foursquare:
//            barColor = UIColor(hexColor: "00aeef")
//            tintColor = UIColor.white
//        case .Instagram:
//            barColor = UIColor(hexColor: "2e5e86")
//            tintColor = UIColor.white
//        case .Kickstarter:
//            barColor = UIColor(hexColor: "f7f8f8")
//            tintColor = UIColor(hexColor: "2bde73")
//        case .Path:
//            barColor = UIColor(hexColor: "544f49")
//            tintColor = UIColor(hexColor: "fffff2")
//        case .Pinterest:
//            barColor = UIColor(hexColor: "f4f4f4")
//            tintColor = UIColor(hexColor: "cb2027")
//        case .Slack:
//            barColor = UIColor(hexColor: "f4f5f6")
//            tintColor = UIColor(hexColor: "3eba92")
//        case .Skype:
//            barColor = UIColor(hexColor: "00aff0")
//            tintColor = UIColor.white
//        case .Tumblr:
//            barColor = UIColor(hexColor: "2e3e53")
//            tintColor = UIColor.white
//        case .Twitter:
//            barColor = UIColor(hexColor: "58aef0")
//            tintColor = UIColor.white
//        case .Vesper:
//            barColor = UIColor(hexColor: "5e7d9a")
//            tintColor = UIColor(hexColor: "f8f8f8")
//        case .Videos:
//            barColor = UIColor(hexColor: "4a4b4d")
//            tintColor = UIColor.black
//        case .Vine:
//            barColor = UIColor(hexColor: "00bf8f")
//            tintColor = UIColor.white
//        case .WWDC:
//            tintColor = UIColor(hexColor: "fc6246")
//        default:
//            barColor = UIColor(hexColor: "f8f8f8")
//            tintColor = UIApplication.shared.keyWindow?.tintColor
//        }
//
//        if let logo = UIImage.init(named: "logo_" + app.display_name!.lowercased()) {
//            self.controller.navigationItem.titleView = UIImageView.init(image: logo)
//        } else {
//            self.controller.navigationItem.titleView = nil
//            self.controller.navigationItem.title = self.app.display_name
//        }
//
//        self.controller.navigationController?.navigationBar.barTintColor = barColor
//        self.controller.navigationController?.navigationBar.tintColor = tintColor
//    }
//
//    func configureStatusBar() {
//        switch app {
//        case .Camera, .Facebook, .Fancy, .Foursquare, .Instagram, .Path, .Skype, .Tumblr, .Twitter, .Vesper, .Vine:
//            UIApplication.shared.statusBarStyle = .lightContent
//        default:
//            UIApplication.shared.statusBarStyle = .default
//        }
//    }
//}
