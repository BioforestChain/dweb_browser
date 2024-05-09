export type $Binary = ArrayBuffer | ArrayBufferView;
export const isBinary = (data: unknown): data is $Binary =>
  data instanceof ArrayBuffer ||
  ArrayBuffer.isView(data) ||
  (typeof SharedArrayBuffer === "function" && data instanceof SharedArrayBuffer);

export const binaryToU8a = (binary: $Binary) => {
  if (binary instanceof Uint8Array) {
    return binary;
  }
  if (ArrayBuffer.isView(binary)) {
    return new Uint8Array(binary.buffer, binary.byteOffset, binary.byteLength);
  }
  return new Uint8Array(binary);
};

export const u8aConcat = (_binaryList: readonly Uint8Array[]) => {
  /// Blob 的开销很大 不建议使用
  // const blob = new Blob(binarys);
  // return blob.arrayBuffer();
  if (_binaryList.length === 0) {
    return new Uint8Array();
  }

  let totalLength = 0;
  const binaryList = _binaryList.filter((binary) => {
    const { byteLength } = binary;
    totalLength += byteLength;
    return byteLength > 0;
  });
  if (binaryList.length === 0) {
    return _binaryList[0];
  }
  if (binaryList.length === 1) {
    return binaryList[0];
  }
  const result = new Uint8Array(totalLength);

  let offset = 0;
  for (const binary of binaryList) {
    result.set(binary, offset);
    offset += binary.byteLength;
  }
  return result;
};
