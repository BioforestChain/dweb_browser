/// <reference path="../node_modules/vite/client.d.ts"/>

import { prepareInMain } from "./prepare-in-main.ts";
import InlineWorker from "./worker.ts?worker";

const canvasList = Array.from({ length: 3 }, () => {
  const canvas = document.createElement("canvas");
  prepareInMain(canvas);
  const offscreen = canvas.transferControlToOffscreen();
  return offscreen;
});

const worker = new InlineWorker();
const envParams = new URLSearchParams(location.search);
worker.postMessage(
  {
    canvasList,
    width: parseInt(envParams.get("width") || "128"),
    height: parseInt(envParams.get("height") || "128"),
    wsUrl: envParams.get("channel"),
    proxyUrl: envParams.get("proxy"),
  },
  canvasList
);
Object.assign(self, { worker });
