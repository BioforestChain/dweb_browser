//
//  ObjcEnum.h
//  BFExplorer
//
//  Created by ui03 on 2023/3/14.
//

#ifndef ObjcEnum_h
#define ObjcEnum_h

typedef NS_ENUM(NSUInteger, IPC_MESSAGE_TYPE) {
    
    /** 类型：请求 */
    REQUEST            = 0,
    /** 类型：相应 */
    RESPONSE           = 1,
    /** 类型：流数据，发送方 */
    STREAM_DATA     = 2,
    /** 类型：流拉取，请求方 */
    STREAM_PULL        = 3,
    /** 类型：流关闭，发送方
         * 可能是发送完成了，也有可能是被中断了
         */
    STREAM_END         = 4,
    /** 类型：流中断，请求方 */
    STREAM_ABORT       = 5,
    /** 类型：事件 */
    STREAM_EVENT       = 6,
    NONE               = 1000
};

typedef NS_ENUM(NSUInteger, IPC_DATA_ENCODING) {
    
    IPC_DATA_ENCODING_NONE            = 0,
    /** UTF8编码的字符串，本质上是 BINARY */
    IPC_DATA_ENCODING_UTF8            = 1 << 1,
    /** BASE64编码的字符串，本质上是 BINARY */
    IPC_DATA_ENCODING_BASE64          = 1 << 2,
    /** 二进制, 与 UTF8/BASE64 是对等关系*/
    IPC_DATA_ENCODING_BINARY          = 1 << 3
};

typedef NS_ENUM(NSUInteger, IPC_META_BODY_TYPE) {
    /** 文本 json html 等 */
    IPC_META_BODY_TYPE_STREAM_ID     = 0,
    /** 内联数据 */
    IPC_META_BODY_TYPE_INLINE         = 1,
    /** 文本 json html 等 */
    STREAM_WITH_TEXT                  = IPC_META_BODY_TYPE_STREAM_ID | IPC_DATA_ENCODING_UTF8,
    /** 使用文本表示的二进制 */
    STREAM_WITH_BASE64                = IPC_META_BODY_TYPE_STREAM_ID | IPC_DATA_ENCODING_BASE64,
    /** 二进制 */
    STREAM_WITH_BINARY                = IPC_META_BODY_TYPE_STREAM_ID | IPC_DATA_ENCODING_BINARY,
    /** 文本 json html 等 */
    INLINE_TEXT                       = IPC_META_BODY_TYPE_INLINE | IPC_DATA_ENCODING_UTF8,
    /** 使用文本表示的二进制 */
    INLINE_BASE64                     = IPC_META_BODY_TYPE_INLINE | IPC_DATA_ENCODING_BASE64,
    /** 二进制 */
    INLINE_BINARY                     = IPC_META_BODY_TYPE_INLINE | IPC_DATA_ENCODING_BINARY
};

#endif /* ObjcEnum_h */
