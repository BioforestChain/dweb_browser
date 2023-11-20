//
//  DIPOCProxyConfiguration.m
//  DwebPlatformIosKit
//
//  Created by instinct on 2023/11/20.
//

#import "DIPOCProxyConfiguration.h"

@implementation DIPOCProxyConfiguration


- (NSArray <nw_proxy_config_t> *)create:(NSString *)host port:(int)port {
    NSString *hostStr = [host copy];
    NSString *portStr = [NSString stringWithFormat:@"%d", port];
    nw_endpoint_t nwHost = nw_endpoint_create_host([hostStr UTF8String], [portStr UTF8String]);
    nw_proxy_config_t config = nw_proxy_config_create_http_connect(nwHost, nil);
    return @[config];
}

@end
