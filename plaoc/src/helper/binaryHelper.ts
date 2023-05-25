export type $Binary = ArrayBuffer | ArrayBufferView;
export const isBinary = (data: unknown): data is $Binary =>
  data instanceof ArrayBuffer || ArrayBuffer.isView(data);

export const binaryToU8a = (binary: $Binary) => {
  if (binary instanceof ArrayBuffer) {
    return new Uint8Array(binary);
  }
  if (binary instanceof Uint8Array) {
    return binary;
  }
  return new Uint8Array(binary.buffer, binary.byteOffset, binary.byteLength);
};

export const u8aConcat = (binaryList: readonly Uint8Array[]) => {
  /// Blob 的开销很大 不建议使用
  // const blob = new Blob(binarys);
  // return blob.arrayBuffer();

  let totalLength = 0;
  for (const binary of binaryList) {
    totalLength += binary.byteLength;
  }
  const result = new Uint8Array(totalLength);

  let offset = 0;
  for (const binary of binaryList) {
    result.set(binary, offset);
    offset += binary.byteLength;
  }
  return result;
};
