//
//  UIImage+SVG.m
//  TenKit
//
//  Created by hongzs on 2021/3/11.
//

#import "UIImage+SVG.h"
#import <SVGKit/SVGKImage.h>

@implementation UIImage (SVG)

+ (UIImage *)svgImageNamed:(NSString *)name size:(CGSize)size {
//    SVGKImage *svgImage = [SVGKImage imageNamed:name];
    SVGKImage *svgImage = [SVGKImage imageWithContentsOfURL:[NSURL URLWithString:@"https://objectjson.waterbang.top/test-vue3/land.svg"]];
    svgImage.size = size;
    
    return svgImage.UIImage;
}

+ (UIImage *)svgImageWithURL:(NSString *)urlString size:(CGSize)size {

    SVGKImage *svgImage = [SVGKImage imageWithContentsOfURL:[NSURL URLWithString:urlString]];
    svgImage.size = size;
    
    return svgImage.UIImage;
}

+ (UIImage*)svgImageWithContentsOfFile:(NSString *)path size:(CGSize)size {
    SVGKImage *svgImage = [SVGKImage imageWithContentsOfFile:path];
    svgImage.size = size;
    
    return svgImage.UIImage;
}

+ (UIImage *)svgImageNamed:(NSString *)name size:(CGSize)size tintColor:(UIColor *)tintColor {
    SVGKImage *svgImage = [SVGKImage imageNamed:name];
    svgImage.size = size;
    CGRect rect = CGRectMake(0, 0, svgImage.size.width, svgImage.size.height);
    CGImageAlphaInfo alphaInfo = CGImageGetAlphaInfo(svgImage.UIImage.CGImage);
    BOOL opaque = alphaInfo == kCGImageAlphaNoneSkipLast || alphaInfo == kCGImageAlphaNoneSkipFirst || alphaInfo == kCGImageAlphaNone;
    UIGraphicsBeginImageContextWithOptions(svgImage.size, opaque, svgImage.scale);
    CGContextRef context = UIGraphicsGetCurrentContext();
    CGContextTranslateCTM(context, 0, svgImage.size.height);
    CGContextScaleCTM(context, 1.0, -1.0);
    CGContextSetBlendMode(context, kCGBlendModeNormal);
    CGContextClipToMask(context, rect, svgImage.UIImage.CGImage);
    CGContextSetFillColorWithColor(context, tintColor.CGColor);
    CGContextFillRect(context, rect);
    UIImage *imageOut = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    return imageOut;
}

@end
