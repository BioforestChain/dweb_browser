//
//  CustomNaviViewController.swift
//  DWebBrowser
//
//  Created by mac on 2022/6/15.
//

import UIKit

class CustomNaviViewController: UINavigationController {

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
    }
    

    override var preferredStatusBarStyle: UIStatusBarStyle {
        let topVC = self.topViewController
        return topVC?.preferredStatusBarStyle ?? .default
    }

    override var prefersStatusBarHidden: Bool {
        let topVC = self.topViewController
        return topVC?.prefersStatusBarHidden ?? false
    }
}
