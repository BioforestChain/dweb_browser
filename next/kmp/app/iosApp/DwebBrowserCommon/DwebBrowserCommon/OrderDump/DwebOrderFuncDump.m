//
//  DwebOrderFuncDump.m
//  DwebOrderFuncDump
//
//  Created by instinct on 2024/1/11
//

#import "DwebOrderFuncDump.h"
#import <dlfcn.h>
#import <libkern/OSAtomicQueue.h>
#import <pthread.h>
#import <stdio.h>
#import <DwebBrowserCommon/DwebBrowserCommon-Swift.h>

static OSQueueHead qHead = OS_ATOMIC_QUEUE_INIT;
static BOOL stopCollecting = NO;

typedef struct {
    void *pointer;
    void *next;
} PointerNode;

void __sanitizer_cov_trace_pc_guard_init(uint32_t *start, uint32_t *stop) {
    static uint32_t N;
    if (start == stop || *start) return;
    for (uint32_t *x = start; x < stop; x++)
        *x = ++N;
}

void __sanitizer_cov_trace_pc_guard(uint32_t *guard) {
    if (stopCollecting) {
        return;
    }

    void *PC = __builtin_return_address(0);
    PointerNode *node = malloc(sizeof(PointerNode));
    *node = (PointerNode){PC, NULL};
    OSAtomicEnqueue(&qHead, node, offsetof(PointerNode, next));
}

const char * getSybomsOutputPath(void) {
    NSString *path = [DwebConfigInfo orderOutputPath];
    return [path UTF8String];
}

extern NSArray <NSString *> *getAllFunctions(NSString *currentFuncName) {
    NSMutableSet<NSString *> *unqSet = [NSMutableSet setWithObject:currentFuncName];
    NSMutableArray <NSString *> *functions = [NSMutableArray array];
    while (YES) {
        PointerNode *front = OSAtomicDequeue(&qHead, offsetof(PointerNode, next));
        if(front == NULL) {
            break;
        }
        Dl_info info = {0};
        dladdr(front->pointer, &info);
        NSString *name = @(info.dli_sname);
        
        if ([unqSet containsObject:name]) {
            continue;
        }
        
        if (![[name lowercaseString] containsString:@"dweb"]) {
            NSLog(@"[iOS] Dump skip: %@", name);
            continue;
        }
        
        if ([[name lowercaseString] containsString:@"dump"]) {
            NSLog(@"[iOS] Dump skip: %@", name);
            continue;
        }
                
        BOOL isObjc = [name hasPrefix:@"+["] || [name hasPrefix:@"-["];
        NSString *symbolName = isObjc ? name : [@"_" stringByAppendingString:name];
        [unqSet addObject:name];
        [functions addObject:symbolName];
    }
    return [[functions reverseObjectEnumerator] allObjects];;
}

#pragma mark - public

extern void dumpFile(void) {
    stopCollecting = YES;
    __sync_synchronize();
    NSString* curFuncationName = [NSString stringWithUTF8String:__FUNCTION__];
    dispatch_queue_t serialQueue = dispatch_queue_create("com.dweb.dump", DISPATCH_QUEUE_SERIAL);
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(3 * NSEC_PER_SEC)), serialQueue, ^{
        const char * outputPath = getSybomsOutputPath();
        if (outputPath == nil) {
            NSLog(@"[iOS] Dump ❌❌❌: outputPath == null");
            [[NSNotificationCenter defaultCenter] postNotificationName:@"DWeb_Debug_Notification" object:@"outputPath == null"];
            return ;
        }
        
        NSArray *functions = getAllFunctions(curFuncationName);
        NSString *orderFileContent = [functions.reverseObjectEnumerator.allObjects componentsJoinedByString:@"\n"];
        NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
        formatter.dateFormat = @"yyyy-MM-dd hh:mm:ss";
        NSString *time = [formatter stringFromDate:[NSDate date]];
        NSString *git = [DwebConfigInfo gitInfo];
        NSString *tag = [NSString stringWithFormat:@"#Attention:%@\n#Create Time:%@\n#Git:%@", 
                         @"内容是自动生成的，请不要手动修改。操作: 请看LinkOrder_README.md",
                         time,
                         git];
        orderFileContent = [NSString stringWithFormat:@"%@\n%@", tag, orderFileContent];
        NSString *filePath = [NSString stringWithCString:outputPath encoding:NSUTF8StringEncoding];
        BOOL isYES = [[NSFileManager defaultManager] fileExistsAtPath:filePath];
        if (isYES) {
            [[NSFileManager defaultManager] removeItemAtPath:filePath error:nil];
        }
        NSError *error = nil;
        [orderFileContent writeToFile:filePath
                           atomically:YES
                             encoding:NSUTF8StringEncoding
                                error:&error];

        if (error) {
            NSLog(@"[iOS] Dump ❌❌❌: %@", error);
            [[NSNotificationCenter defaultCenter] postNotificationName:@"DWeb_Debug_Notification" object: error];
        } else {
            [[NSNotificationCenter defaultCenter] postNotificationName:@"DWeb_Debug_Notification" object:@"✅✅✅"];
            NSLog(@"[iOS] Dump ✅✅✅: %@", filePath);
        }
    });
}
