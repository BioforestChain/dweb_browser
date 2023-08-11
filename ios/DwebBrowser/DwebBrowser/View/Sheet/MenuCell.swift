//
//  MenuCell.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/6.
//

import SwiftUI

struct MenuCell: View {
    
    var title: String = ""
    var imageName: String = ""
    
    var body: some View {
        HStack {
            Text(title)
                .padding(.leading, 16)
                .foregroundColor(Color.menuTitleColor)
                .font(.system(size: 16))
            Spacer()
            Image(uiImage: .assetsImage(name: imageName))
                .renderingMode(.template)
                .foregroundColor(Color.menuTitleColor)
                .padding(12)
                
        }
        .frame(height: 50)
        .background(Color.menubkColor)
        .cornerRadius(6)
        .padding(.horizontal, 16)
    }
}

struct MenuCell_Previews: PreviewProvider {
    static var previews: some View {
        MenuCell()
    }
}
