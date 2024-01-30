//
//  EnvironmentModel.swift
//  DwebPlatformIosKit
//
//  Created by ios on 2024/1/29.
//

import Foundation
import SwiftUI


let bkColor = UIColor(red: 240/255.0, green: 240/255.0, blue: 246/255.0, alpha: 1.0)
var recordsCount: Int = 0

class EnvironmentModel: ObservableObject {
    @Published var isRecording: Bool = false
}
