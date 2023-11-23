//
//  SSSSS.swift
//  iosApp
//
//  Created by instinct on 2023/11/17.
//  Copyright © 2023 orgName. All rights reserved.
//

import DwebShared
import UIKit

class DwebPureViewController{
    var vc: UIViewController
    var prop: HelperPlatformDwebUIViewControllerProperty
    init(vc: UIViewController, prop: HelperPlatformDwebUIViewControllerProperty) {
        self.vc = vc
        self.prop = prop
        Main_iosKt.dwebRootUIViewController_onInit(id: prop.vcId)
    }

    deinit {
        Main_iosKt.dwebRootUIViewController_onDestroy(id: prop.vcId) // onDestroy
    }
}
