//
//  ShowAppView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/6/6.
//

import SwiftUI

struct InnerAppGridView: View {
    //视图左右间距20，列间距10 每列显示4个，
    private let imgWidth = (UIScreen.main.bounds.width - 70) * 0.25 * 0.78 - 3
    
    private let columns: [GridItem] = [
        GridItem(.flexible(), spacing: 10),
        GridItem(.flexible(), spacing: 10),
        GridItem(.flexible(), spacing: 10),
        GridItem(.flexible(), spacing: 10)
    ]
    
    @ObservedObject var appMgr = InstalledAppMgr.shared
    var body: some View {
        VStack(alignment: .leading, content: {
            Text("我的App")
                .font(.system(size: 20, weight: .bold))
                .frame(height: 40)
            LazyVGrid(columns: columns, spacing: 20) {
                ForEach(appMgr.apps) { app in
                    Button {
                        Log("opneing an app")
                        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                        BridgeManager.shared.clickApp(appUrl: app.link)
                    } label: {
                        VStack {
                            Image(uiImage: .assetsImage(name: (app.icon)))
                                .resizable()
                                .frame(width: imgWidth,height: imgWidth)
                                .cornerRadius(12.0)
                            
                            Text(app.title)
                                .font(.system(size: 13.0))
                                .foregroundColor(.black)
                                .lineLimit(1)
                        }
                    }
                    
                }
            }
        })
        .padding(EdgeInsets(top: 0, leading: 20, bottom: 0, trailing: 20))
    }
}

struct ShowAppView_Previews: PreviewProvider {
    static var previews: some View {
        InnerAppGridView()
    }
}
