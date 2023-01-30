//
//  BrowserToolbar.swift
//  Browser
//
//         
//

import UIKit
import WebKit

protocol BrowserToolBarDelegate: AnyObject {
    func goBackClicked()
    func goForwardClicked()
    func clickAppendBookmark()
    func showMoreClicked()
    func goHomePageClicked()
    
}

extension UIBarButtonItem{
    
}
extension UIBarButtonItem {
    var frame: CGRect? {
        guard let view = self.value(forKey: "view") as? UIView else { return nil }
        return view.frame
    }
}
class MyCustomBarButtonItem: UIView {
    // ...

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        print("touchesBegan...")
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        print("touchesEnded...")
    }
}

class BrowserToolbar: UIToolbar {
    let goBackButton = UIBarButtonItem()
    let goForwardButton = UIBarButtonItem()
    let moreButton = UIBarButtonItem()
    let bookmarkButton = UIBarButtonItem()
    let homeButton = UIBarButtonItem()

    weak var bottomToolbarDelegate: BrowserToolBarDelegate?
    
    override init(frame: CGRect) {
        // If toolbar frame size is not set then autolayout breaks so we need to set it manually
        // https://stackoverflow.com/questions/59700020/layout-constraint-errors-with-simple-uitoolar-for-keyboard-inputaccessoryview
        super.init(frame: CGRect(origin: .zero, size: CGSize(width: 100, height: 100)))
        setupButtons()
        backgroundColor = UIColor(hexColor: "959595")

    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    @objc func btnClicked(sender:UIButton){
        print(sender.tag)
        
        switch sender.tag {
        case 1:
            bottomToolbarDelegate?.goBackClicked()
        case 2:
            bottomToolbarDelegate?.goForwardClicked()
        case 3:
            bottomToolbarDelegate?.clickAppendBookmark()

        case 4:
            bottomToolbarDelegate?.showMoreClicked()
        default:
            bottomToolbarDelegate?.goHomePageClicked()

        }
    }
    
}

private extension BrowserToolbar {
    
    func setupButtons() {
        let barItems = [goBackButton,goForwardButton,bookmarkButton,moreButton,homeButton]
        let images = ["ico_bottomtab_left", "ico_bottomtab_right", "ico_bottomtab_book", "ico_bottomtab_more", "ico_bottomtab_home"]
        for (i,name) in images.enumerated(){
            let button  = UIButton(type: .custom)
            button.setImage(UIImage(named: name + "_normal"), for: .normal)
            button.setImage(UIImage(named: name + "_disabled"), for: .disabled)
            button.imageEdgeInsets = UIEdgeInsets(top: 60, left: 0, bottom: 0, right: 0)
            button.frame = CGRect(x: 0, y: 0, width: 40, height: 100)
            
            button.addTarget(self, action: #selector(btnClicked), for: .touchUpInside)
            button.tag = 1 + i
            barItems[i].customView = button
        }
              
        items = [
            goBackButton,
            UIBarButtonItem(barButtonSystemItem: .flexibleSpace, target: nil, action: nil),
            goForwardButton,
            UIBarButtonItem(barButtonSystemItem: .flexibleSpace, target: nil, action: nil),
            bookmarkButton,
            UIBarButtonItem(barButtonSystemItem: .flexibleSpace, target: nil, action: nil),
            moreButton,
            UIBarButtonItem(barButtonSystemItem: .flexibleSpace, target: nil, action: nil),
            homeButton
        ]

        goBackButton.isEnabled = false
        goForwardButton.isEnabled = false
        bookmarkButton.isEnabled = false
        homeButton.isEnabled = false
    }

    

}
