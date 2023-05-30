// import { crypto, toHashString } from "../../deps.ts";

// /**
//   getFileHash
//   用于为文件生成hash
//   */
// export const getFileHash = async (filePath: string): Promise<string> => {
//   const fileBuffer = await getFileBuffer(filePath);
//   return await getHashfBuffer(fileBuffer);
// };

// const getHashfBuffer = async (data: BufferSource) =>
//   toHashString(await crypto.subtle.digest("SHA-256", data));

// const getFileBuffer = async (filePath: string): Promise<Uint8Array> => {
//   const file = await Deno.open(filePath);
//   const buf = new Uint8Array((await Deno.stat(filePath)).size);
//   await file.read(buf);
//   file.close();
//   return buf;
// };
