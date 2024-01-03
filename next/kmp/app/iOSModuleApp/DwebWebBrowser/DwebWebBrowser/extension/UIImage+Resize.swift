//
//  UIImage+resize.swift
//  iosApp
//
//  Created by instinct on 2023/12/8.
//  Copyright © 2023 orgName. All rights reserved.
//

import UIKit

extension UIImage {
    func resize(toSize reSize: CGSize) -> UIImage? {
        UIGraphicsBeginImageContextWithOptions(reSize,false,UIScreen.main.scale);
        draw(in: CGRectMake(0, 0, reSize.width, reSize.height));
        let reSizeImage:UIImage? = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext();
        return reSizeImage;
    }
}
