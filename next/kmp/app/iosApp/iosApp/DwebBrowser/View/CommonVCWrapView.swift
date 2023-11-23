//
//  CommonVCWrapView.swift
//  iosApp
//
//  Created by instinct on 2023/11/17.
//  Copyright © 2023 orgName. All rights reserved.
//

import DwebShared
import SwiftUI
import UIKit

struct CommonVCWrapView<T: UIViewController>: UIViewControllerRepresentable {
    let vc: T
    let prop: HelperPlatformDwebUIViewControllerProperty

    func makeUIViewController(context: Context) -> UIViewController {
        vc.view.backgroundColor = .clear
        if prop.vcId == 1 {
            
            DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                print("vcAddress start")
                for (index, subview) in vc.view.subviews.enumerated() {
                    print("vcAddress\(vc) = \(subview)[\(subview.subviews.count)]")
                    if index == 1 {
                        subview.isUserInteractionEnabled = false
                        for (index2, subview2) in subview.subviews.enumerated() {
                            print("    vcAddress\(vc) = \(subview2)[\(subview2.subviews.count)]")
                            if index == 0 {
                                print("   ok")
                            }
                        }
                    }
                }
                print("vcAddress end")
            }
        }

        return vc
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
