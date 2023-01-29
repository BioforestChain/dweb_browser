//
//  BrowserContainerViewController+CollectionView.swift
//  Browser
//
//   /21.
//

import Foundation
import UIKit

let items = [UIColor.red, UIColor.green, UIColor.blue, UIColor.purple]
extension BrowserTabViewController:UICollectionViewDelegate,UICollectionViewDataSource{
    func numberOfSections(in collectionView: UICollectionView) -> Int {
        return 1
    }
    
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
//        self.pageControl.numberOfPages = items.count
        return items.count * 2
    }
    
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: "cell",
                                                      for: indexPath) as! UICollectionViewCell
        
        // Configure the cell
        cell.contentView.backgroundColor = .green

        if indexPath.section / 4 == 0
        {
            cell.contentView.backgroundColor = .red
        }
        
        return cell
    }
}
