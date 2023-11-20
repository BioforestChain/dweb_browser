//
//  DIPOCProxyConfiguration.h
//  DwebPlatformIosKit
//
//  Created by instinct on 2023/11/20.
//

#import <Foundation/Foundation.h>
#import <WebKit/WebKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface DIPOCProxyConfiguration : NSObject
- (NSArray <nw_proxy_config_t> *)create:(NSString *)host port:(int)port;
@end

NS_ASSUME_NONNULL_END
