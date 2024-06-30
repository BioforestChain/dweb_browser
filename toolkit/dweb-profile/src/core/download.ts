import { decode, encode } from 'cbor-x';

export const encodeToBlob = async (obj: unknown) => {
  return await new Response(encode(obj)).blob();
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
