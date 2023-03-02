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

  const cameraView = $<HTMLVideoElement>("#camera-view");
  $<HTMLButtonElement>("#open-camera").onclick = async () => {
    cameraView.setAttribute("playsinline", "");
    cameraView.setAttribute("autoplay", "");
    cameraView.setAttribute("muted", "");
    cameraView.style.width = "200px";
    cameraView.style.height = "200px";

    /* Stream it to video element */
    navigator.mediaDevices
      .getUserMedia(
        /* Setting up the constraint */
        {
          audio: false,
          video: {
            facingMode: "user", // Can be 'user' or 'environment' to access back or front camera (NEAT!)
          },
        }
      )
      .then(
        function success(stream) {
          cameraView.srcObject = stream;
          console.log(stream);
          cameraView.play()
        },
        (err) => {
          console.error(err);
        }
      );
  };
};

export const CODE = async (require: IpcRequest) => {
  return script.toString().match(/\{([\w\W]+)\}/)![1];
};
