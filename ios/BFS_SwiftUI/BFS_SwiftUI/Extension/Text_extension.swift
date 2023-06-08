//
//  Text_extension.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/25.
//

import SwiftUI


extension Text {
    
    func attributedText(_ attributedString: NSAttributedString) -> some View {
        return AttributedText(attributedString: attributedString)
    }
    
    func LinkAttributedText(_ attributedString: NSAttributedString) -> Text {
        let uiView = UITextView()
        uiView.attributedText = attributedString
        uiView.isUserInteractionEnabled = true
        uiView.isEditable = false
        uiView.isSelectable = true
        uiView.font = UIFont.systemFont(ofSize: 1.0)
        uiView.linkTextAttributes = [.foregroundColor: UIColor.init(white: 138 / 255.0, alpha: 1.0)]
        return Text(uiView.attributedText.string).font(.headline)
    }
}

struct AttributedText: UIViewRepresentable {
    
    let attributedString: NSAttributedString
    
    func makeUIView(context: Context) -> UILabel {
        let label = UILabel()
        label.numberOfLines = 1
        label.textAlignment = .left
        return label
    }
    
    func updateUIView(_ uiView: UILabel, context: Context) {
        uiView.attributedText = attributedString
    }
}
