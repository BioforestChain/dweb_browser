import { PromiseOut } from "../helper/PromiseOut.ts";

export interface FileData {
  name: string;
  type: string;
  size: number;
  encode: FileDataEncode;
  data: string;
}

export enum FileDataEncode {
  UTF8 = "utf8",
  BASE64 = "base64"
}

export async function normalToBase64String(file: File): Promise<string> {
  const reader = new FileReader();
  const po = new PromiseOut<string>();

  reader.onloadend = () => {
    let binary = "";
    const bytes = new Uint8Array(reader.result as ArrayBuffer);
    for (const byte of bytes) {
      binary += String.fromCharCode(byte);
    }
    po.resolve(btoa(binary));
  };

  reader.readAsArrayBuffer(file);
  return await po.promise;
}