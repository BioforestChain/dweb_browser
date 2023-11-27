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

struct CommonVCWrapView: UIViewControllerRepresentable {
    
    let vc: UIViewController
    func makeUIViewController(context: Context) -> UIViewController {
        vc.view.backgroundColor = .clear
        return vc
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
