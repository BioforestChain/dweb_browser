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
        Main_iosKt.dwebViewController.emitOnInit(vcId: prop.vcId)
    }

    deinit {
        Main_iosKt.dwebViewController.emitOnDestroy(vcId: prop.vcId)
    }
}
