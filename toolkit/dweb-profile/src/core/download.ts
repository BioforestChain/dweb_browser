import { decode, encode } from 'cbor-x';
// require('@bokuweb/zstd-wasm/dist/esm/index.web.js')
// import { init, compress, decompress } from '@bokuweb/zstd-wasm/dist/esm/index.web.js';
// import zstd_wasm from './zstd.wasm?init';

export const BACKUP_FILE_EXT = '.dwebackup';

export const encodeToBlob = async (obj: unknown) => {
  // 先进行cbor编码
  const cbor = encode(obj);
  // //@ts-ignore
  // const xx= await zstd_wasm()
  // await init(zstd_wasm_url);
  // 然后进行压缩
  // const compressed = compress(cbor, 10);

  return await new Response(cbor).blob();
};

export const decodeFromBlob = async (blob: Blob) => {
  return decode(new Uint8Array(await blob.arrayBuffer()));
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
