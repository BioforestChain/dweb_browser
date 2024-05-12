/// <reference path="../node_modules/vite/client.d.ts"/>

import { prepareInMain } from "./prepare-in-main.ts";
import InlineWorker from "./worker.ts?worker";

const canvas = document.createElement("canvas");
prepareInMain(canvas);

const worker = new InlineWorker();
const offscreen = canvas.transferControlToOffscreen();
const envParams = new URLSearchParams(location.search);
worker.postMessage(
  {
    canvas: offscreen,
    width: parseInt(envParams.get("width") || "") || canvas.clientWidth,
    height: parseInt(envParams.get("height") || "") || canvas.clientHeight,
    wsUrl: envParams.get("channel"),
    proxyUrl: envParams.get("proxy"),
  },
  [offscreen]
);
Object.assign(self, { worker });
