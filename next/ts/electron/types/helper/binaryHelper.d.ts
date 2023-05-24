export type $Binary = ArrayBuffer | ArrayBufferView;
export declare const isBinary: (data: unknown) => data is $Binary;
export declare const binaryToU8a: (binary: $Binary) => Uint8Array;
export declare const u8aConcat: (binaryList: readonly Uint8Array[]) => Uint8Array;
