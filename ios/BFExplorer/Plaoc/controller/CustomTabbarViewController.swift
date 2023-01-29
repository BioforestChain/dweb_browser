//
//  CustomTabbarViewController.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/7/7.
//

import UIKit
import RxSwift

class CustomTabbarViewController: UITabBarController {

    //第三方 ESTabBarController
    private let disposeBag = DisposeBag()
    var urlString: String = ""
    var isLL: Bool = false
    
    let webVC1 = WebViewViewController()
    let webVC2 = WebViewViewController()
    let webVC3 = WebViewViewController()
    let webVC4 = WebViewViewController()
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        self.navigationController?.isNavigationBarHidden = true
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.view.backgroundColor = .white
        self.tabBar.isTranslucent = false
        self.tabBar.backgroundColor = UIColor.white
        self.tabBar.shadowImage = UIImage()
        self.tabBar.backgroundImage = UIImage()
        
//        self.tabBar.selectionIndicatorImage = UIImage.svgImageNamed("method_photo", size: CGSize(width: 44, height: 22))
        
        addChildViewController(child: webVC1, title: "首页", imageName: "")
        
        operateMonitor.tabBarMonitor.subscribe(onNext: { [weak self] in
            guard let strongSelf = self else { return }
            strongSelf.updateTabbar()
        }).disposed(by: self.disposeBag)
    }
    
    func updateTabbar() {
        
//        self.tabBar.setNeedsDisplay()
//        self.tabBar.layoutIfNeeded()
//        self.tabBar.layoutSubviews()
        
//        let item = self.tabBar.items?.first
//        item?.title = "测试"
//        self.tabBar.unselectedItemTintColor = .red
//        self.tabBar.tintColor = .orange
//        let childVC = self.viewControllers?.first
//        childVC?.tabBarItem.badgeValue = "99+"
//        childVC?.tabBarItem.setTitleTextAttributes([NSAttributedString.Key.foregroundColor:UIColor.orange], for: .selected)
        
        //需要修改先修改自身视图控制器view的frame
        self.tabBar.barTintColor = .orange
        self.tabBar.backgroundColor = .orange
        var frame = webVC1.view.frame
        frame.size.height += 100
        webVC1.view.frame = frame
//        webVC1.updateBottomViewOverlay(overlay: 1)
        
//        let vc1 = SecondViewController()
//        let vc2 = ThirdViewController()
//        for vc in [vc1,vc2] {
//            let childVC = UINavigationController(rootViewController: vc)
//            childVC.tabBarItem.title = "话术"
//
//            let image = UIImage.svgImageNamed("ico_bottom_tab_publisher", size: CGSize(width: 22, height: 22))
//            let selectedImage = UIImage.svgImageNamed("ico_bottom_tab_publisher", size: CGSize(width: 22, height: 22))
//
//            childVC.tabBarItem.image = image.withRenderingMode(UIImage.RenderingMode.alwaysOriginal)
//            childVC.tabBarItem.selectedImage = selectedImage.withRenderingMode(UIImage.RenderingMode.alwaysOriginal)
//            childVC.tabBarItem.setTitleTextAttributes([NSAttributedString.Key.foregroundColor:UIColor( "#C2C5CB"),NSAttributedString.Key.font:UIFont.systemFont(ofSize: 12)], for: .normal)
//            childVC.tabBarItem.setTitleTextAttributes([NSAttributedString.Key.foregroundColor:UIColor("#0A1930"),NSAttributedString.Key.font:UIFont.systemFont(ofSize: 12)], for: .selected)
//            self.viewControllers?.append(childVC)
////            self.addChild(childVC)  这样写。没效果
//        }
        
    }
    
    
    func updateHeight() {
        var frame = self.tabBar.frame
        frame.size.height = 120
        frame.origin.y -= 37
        self.tabBar.frame = frame
        
        let transiView = self.view.subviews.first
        transiView?.frame.size.height = 120
        transiView?.frame.origin.y = frame.origin.y
        
        self.tabBar.layoutSubviews()
        transiView?.layoutSubviews()
        self.tabBar.setNeedsDisplay()
        transiView?.setNeedsDisplay()
    }
  
    
    private func addChildViewController(child: UIViewController,title:String,imageName:String) {

        let childVC = UINavigationController(rootViewController: child)
        childVC.tabBarItem.title = title

        let image = UIImage.svgImageNamed("ico_bottom_tab_publisher", size: CGSize(width: 22, height: 22))
        let selectedImage = UIImage.svgImageNamed("ico_bottom_tab_publisher", size: CGSize(width: 22, height: 22))

        childVC.tabBarItem.image = image.withRenderingMode(UIImage.RenderingMode.alwaysOriginal)
        childVC.tabBarItem.selectedImage = selectedImage.withRenderingMode(UIImage.RenderingMode.alwaysOriginal)
        childVC.tabBarItem.setTitleTextAttributes([NSAttributedString.Key.foregroundColor:UIColor( "#C2C5CB"),NSAttributedString.Key.font:UIFont.systemFont(ofSize: 12)], for: .normal)
        childVC.tabBarItem.setTitleTextAttributes([NSAttributedString.Key.foregroundColor:UIColor("#0A1930"),NSAttributedString.Key.font:UIFont.systemFont(ofSize: 12)], for: .selected)

        self.addChild(childVC)
    }
    
}
