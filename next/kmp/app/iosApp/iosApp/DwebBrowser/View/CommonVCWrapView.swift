//
//  CommonVCWrapView.swift
//  iosApp
//
//  Created by instinct on 2023/11/17.
//  Copyright © 2023 orgName. All rights reserved.
//

import UIKit
import SwiftUI

struct CommonVCWrapView<T: UIViewController>: UIViewControllerRepresentable {
    
  let vc: T
    
  func makeUIViewController(context: Context) -> UIViewController {
    vc
  }
    
  func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
      
  }
}
