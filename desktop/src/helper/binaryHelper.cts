export type $Binary = ArrayBuffer | ArrayBufferView;
export const isBinary = (data: unknown): data is $Binary =>
  data instanceof ArrayBuffer || ArrayBuffer.isView(data);

export const binaryToU8A = (binary: $Binary) => {
  if (binary instanceof ArrayBuffer) {
    return new Uint8Array(binary);
  }
  if (binary instanceof Uint8Array) {
    return binary;
  }
  return new Uint8Array(binary.buffer, binary.byteOffset, binary.byteLength);
};
