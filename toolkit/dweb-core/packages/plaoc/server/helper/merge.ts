export function merge(...buf: Uint8Array[]) {
  let totalLength = buf.reduce((total, arr) => total + arr.length, 0);
  let merged = new Uint8Array(totalLength);
  let offset = 0;
  for (let arr of buf) {
    merged.set(arr, offset);
    offset += arr.length;
  }
  return merged;
}
