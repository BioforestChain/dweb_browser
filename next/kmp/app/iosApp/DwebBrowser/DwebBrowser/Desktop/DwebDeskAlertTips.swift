//
//  Alert.swift
//  DwebBrowser
//
//  Created by instinct on 2024/1/25.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI

extension View {
    func deskAlertTip() -> some View {
        modifier(NoNetWorkTipModifier())
            .modifier(UpgradleTipModifier())
    }
}

struct NoNetWorkTipModifier: ViewModifier {
    @State private var networkManager = NetworkManager()
    func body(content: Content) -> some View {
        content
            .sheet(isPresented: Binding(get: {
                !networkManager.isNetworkAvailable
            }, set: {
                networkManager.isNetworkAvailable = !$0
            })) {
                NetworkGuidView()
            }
    }
}

struct UpgradleTipModifier: ViewModifier {
    @State private var versionMgr = AppVersionMgr()
    func body(content: Content) -> some View {
        content
            .alert(isPresented: Binding(get: {
                versionMgr.needUpdate
            }, set: {
                versionMgr.needUpdate = $0
            })) {
                Alert(title: Text("更新提示"),
                      message: Text("有新版本可用，请您前往App Store更新。"),
                      primaryButton: .cancel(),
                      secondaryButton: .default(Text("前往更新"), action: {
                          UIApplication.shared.open(URL(string: AppVersionMgr.appStoreURL)!, options: [:], completionHandler: nil)
                      }))
            }
            .task {
                await versionMgr.checkUpdate()
            }
    }
}
