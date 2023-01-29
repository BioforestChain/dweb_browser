//
//  UIImage+SVG.h
//  TenKit
//
//  Created by hongzs on 2021/3/11.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface UIImage (SVG)

+ (UIImage *)svgImageNamed:(NSString *)name size:(CGSize)size;
+ (UIImage *)svgImageWithURL:(NSString *)urlString size:(CGSize)size;
+ (UIImage*)svgImageWithContentsOfFile:(NSString *)path size:(CGSize)size;
+ (UIImage *)svgImageNamed:(NSString *)name size:(CGSize)size tintColor:(UIColor *)tintColor;

@end

NS_ASSUME_NONNULL_END
