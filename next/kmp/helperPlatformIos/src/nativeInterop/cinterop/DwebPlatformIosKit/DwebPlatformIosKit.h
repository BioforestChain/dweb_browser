//
//  DwebPlatformIosKit.h
//  DwebPlatformIosKit
//
//  Created by kzf on 2023/11/16.
//

#import <Foundation/Foundation.h>

//! Project version number for DwebPlatformIosKit.
FOUNDATION_EXPORT double DwebPlatformIosKitVersionNumber;

//! Project version string for DwebPlatformIosKit.
FOUNDATION_EXPORT const unsigned char DwebPlatformIosKitVersionString[];

// In this header, you should import all the public headers of your framework using statements like #import <DwebPlatformIosKit/PublicHeader.h>

NS_INLINE NSException * _Nullable tryBlock(void(^_Nonnull tryBlock)(void)) {
    @try {
        tryBlock();
    }
    @catch (NSException *exception) {
        return exception;
    }
    return nil;
}



@interface SwiftTryCatch : NSObject



/**
 Provides try catch functionality for swift by wrapping around Objective-C
 */
+ (void)tryBlock:(void(^)(void))tryBlock catchBlock:(void(^)(NSException*exception))catchBlock finallyBlock:(void(^)(void))finallyBlock;

+ (void)tryBlock:(void(^)(void))tryBlock catchError:(void(^)(NSError*error))catchError finallyBlock:(void(^)(void))finallyBlock;


+ (void)throwString:(NSString*)s;
+ (void)throwException:(NSException*)e;


@end

@interface NSException ( SwiftTryCatch )
- (NSError *) toError;
@end


@interface ObjC : NSObject

+ (BOOL)catchException:(void(^)(void))tryBlock error:(__autoreleasing NSError **)error;

@end

