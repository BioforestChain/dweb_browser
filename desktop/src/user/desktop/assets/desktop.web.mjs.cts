import type { IpcRequest } from "../../../core/ipc/IpcRequest.cjs";

/// <reference lib="DOM"/>
const script = () => {
  const logEle = document.querySelector(
    "#readwrite-stream-log"
  ) as HTMLPreElement;
  const log = (...logs: any[]) => {
    logEle.append(document.createTextNode(logs.join(" ") + "\n"));
  };
  const $ = <T extends HTMLElement>(selector: string) =>
    document.querySelector(selector) as T;

  $<HTMLButtonElement>("#open-btn").onclick = async () => {
    open(`/index.html?qaq=${encodeURIComponent(Date.now())}`);
  };
  $<HTMLButtonElement>("#close-btn").onclick = async () => {
    close();
  };
};

export const CODE = async (require: IpcRequest) => {
  return script.toString().match(/\{([\w\W]+)\}/)![1];
};
