//
//  DwebPlatformIosKit.m
//  DwebPlatformIosKit
//
//  Created by kzf on 2023/12/1.
//

#import <Foundation/Foundation.h>
#import "DwebPlatformIosKit.h"


@implementation NSException ( SwiftTryCatch )
- (NSError *) toError {
    NSMutableDictionary * info = [NSMutableDictionary dictionary];
    [info setValue:self.name forKey:@"MONExceptionName"];
    [info setValue:self.reason forKey:@"MONExceptionReason"];
    [info setValue:self.callStackReturnAddresses forKey:@"MONExceptionCallStackReturnAddresses"];
    [info setValue:self.callStackSymbols forKey:@"MONExceptionCallStackSymbols"];
    [info setValue:self.userInfo forKey:@"MONExceptionUserInfo"];
    return [[NSError alloc] initWithDomain:@"com.ProtonMail.OpenPGP" code:1000000 userInfo:info];
}
@end


@implementation ObjC

+ (BOOL)catchException:(void(^)(void))tryBlock error:(__autoreleasing NSError **)error {
    @try {
        tryBlock();
        return YES;
    }
    @catch (NSException *exception) {
        if (exception.name != NULL) {
            *error = [[NSError alloc] initWithDomain:exception.name code:1000000 userInfo:exception.userInfo];
        } else {
            *error = [[NSError alloc] initWithDomain:@"ExceptionInObjC" code:1000000 userInfo:exception.userInfo];
        }
    }
}

@end


@implementation SwiftTryCatch
/**
 Provides try catch functionality for swift by wrapping around Objective-C
 */
+ (void)tryBlock:(void(^)(void))tryBlock catchBlock:(void(^)(NSException*exception))catchBlock finallyBlock:(void(^)(void))finallyBlock {
    @try {
        tryBlock ? tryBlock() : nil;
    }
    
    @catch (NSException *exception) {
        catchBlock ? catchBlock(exception) : nil;
    }
    @finally {
        finallyBlock ? finallyBlock() : nil;
    }
}

+ (void)tryBlock:(void(^)(void))tryBlock catchError:(void(^)(NSError*error))catchError finallyBlock:(void(^)(void))finallyBlock {
    @try {
        tryBlock ? tryBlock() : nil;
    }
    @catch (NSException *exception) {
        NSError* e = nil;
        //e = exception.toError()
        catchError ? catchError(e) : nil;
        //catchError ? catchError() : nil;
    }
    @finally {
        finallyBlock ? finallyBlock() : nil;
    }
}

+ (void)throwString:(NSString*)s
{
    @throw [NSException exceptionWithName:s reason:s userInfo:nil];
}

+ (void)throwException:(NSException*)e
{
    @throw e;
}



@end
