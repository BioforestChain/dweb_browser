// import '../polyfill/blob-asyncIterator.ts';
// import '../polyfill/readablestream-asyncIterator.ts';
import { decode, encode, addExtension } from 'cbor-x';
// require('@bokuweb/zstd-wasm/dist/esm/index.web.js')
// import { init, compress, decompress } from '../zstd/index.ts';
// import zstd_wasm from './zstd.wasm?init';
import init_zstd, { compress, decompress } from '@dweb-browser/zstd-wasm';
import zstd_wasm_url from '@dweb-browser/zstd-wasm/zstd_wasm_bg.wasm?url';
await init_zstd(zstd_wasm_url);

export const BACKUP_FILE_EXT = '.dwebackup';

// addExtension({
//   Class: Blob,
//   tag: 'blob',
//   decode(item) {},
//   encode(value, encodeFn) {},
// });

export const encodeToBlob = async (obj: unknown) => {
  // 先进行cbor编码
  const cbor = encode(obj);
  // 然后进行压缩
  const compressed = compress(cbor, 12);
  const decompressed = decompress(compressed);
  console.log(indexedDB.cmp(cbor, decompressed), decompressed);
  console.log(
    `压缩完成 ${cbor.length}=>${compressed.length}, ⬇️ ${((compressed.length / cbor.length) * 100).toFixed(2)}%`,
  );
  return await new Response(compressed).blob();
};

export const decodeFromBlob = async (blob: Blob) => {
  const compressed = new Uint8Array(await blob.arrayBuffer());
  // 先进行解压缩
  console.log(compress(compressed, 10));
  const cbor = decompress(compressed);
  console.log(
    `解压缩完成 ${compressed.length}=>${cbor.length}, ⬆️ ${((compressed.length / cbor.length) * 100).toFixed(2)}%`,
  );
  // 然后进行解码
  return decode(new Uint8Array(cbor));
};

export const download = (url: string, filename: string) => {
  const aEle = document.createElement('a');
  aEle.href = url;
  aEle.download = filename;
  aEle.click();
};

export const getDateTimeString = () => {
  const dt = new Date();
  const padL = (nr: number, len = 2, chr = `0`) => `${nr}`.padStart(2, chr);

  const date = `${padL(dt.getMonth() + 1)}_${padL(dt.getDate())}_${dt.getFullYear()}`;
  const time = `${padL(dt.getHours())}_${padL(dt.getMinutes())}_${padL(dt.getSeconds())}`;

  return `${date}-${time}`;
};
