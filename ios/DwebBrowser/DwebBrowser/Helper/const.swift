//
//  const.swift
//  DwebBrowser
//
//  Created by ui06 on 4/25/23.
//

import UIKit
import Foundation

let screen_width = UIScreen.main.bounds.width
let screen_height = UIScreen.main.bounds.height


@MainActor class TabPagesExpandState: ObservableObject{
    @Published var state = false
}


//@MainActor class AddressBarHStackOffset: ObservableObject{
//    @Published var offset = 0.0
//}
