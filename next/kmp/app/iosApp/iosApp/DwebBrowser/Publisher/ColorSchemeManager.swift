//
//  ColorSchemeManager.swift
//  iosApp
//
//  Created by ui06 on 1/2/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI

enum LocalColorScheme: Int{
    case unspecified, light, dark
}

class ColorSchemeManager: ObservableObject{
    @AppStorage("colorScheme") var colorScheme: LocalColorScheme = .unspecified {
        didSet{
            applyColorScheme()
        }
    }
    
    func applyColorScheme(){
        keyWindow?.overrideUserInterfaceStyle = UIUserInterfaceStyle(rawValue: colorScheme.rawValue)!
    }
    
    private var keyWindow: UIWindow?{
        return UIApplication.shared.keyWindow
    }
}
