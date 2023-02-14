import type {
  MessageEvent as MessageEventMain,
  MessagePortMain,
} from "electron";
import { MessageChannelMain } from "electron/main";
import {
  updateMainMessageListener,
  updateMainPostMessage,
} from "./electronMainPort.mjs";
import {
  updateRenderMessageListener,
  updateRenderPostMessage,
} from "./electronRenderPort.mjs";

export const postMainMesasgeToRenderMessage = (
  from_port: MessagePortMain,
  to_port: MessagePort,
  event: MessageEventMain
) => {
  let transfer: undefined | Transferable[];
  if (event.ports.length > 0) {
    transfer = event.ports.map(MainPortToRenderPort);
  }
  if (transfer !== undefined) {
    to_port.postMessage(event.data, transfer);
  } else {
    to_port.postMessage(event.data);
  }
};

export const postRenderMessageToMainMesasge = (
  from_port: MessagePort,
  to_port: MessagePortMain,
  event: MessageEvent
) => {
  let ports: undefined | MessagePortMain[];
  if (event.ports.length > 0) {
    ports = event.ports.map(RenderPortToMainPort);
  }
  if (ports) {
    to_port.postMessage(event.data, ports);
  } else {
    to_port.postMessage(event.data);
  }
};

export const MainPortToRenderPort = (main_port: MessagePortMain) => {
  const render_channel = new MessageChannel();
  main_port.addListener(
    "message",
    postMainMesasgeToRenderMessage.bind(null, main_port, render_channel.port1)
  );
  render_channel.port1.addEventListener(
    "message",
    postRenderMessageToMainMesasge.bind(null, render_channel.port1, main_port)
  );

  main_port.addListener("close", () => {
    render_channel.port1.close();
  });
  main_port.start();
  render_channel.port1.start();

  updateRenderMessageListener(render_channel.port2, "addEventListener", 1);
  updateRenderMessageListener(render_channel.port2, "removeEventListener", 1);
  updateRenderPostMessage(render_channel.port2);
  return render_channel.port2;
};

export const RenderPortToMainPort = (render_port: MessagePort) => {
  const main_channel = new MessageChannelMain();
  render_port.addEventListener(
    "message",
    postRenderMessageToMainMesasge.bind(null, render_port, main_channel.port1)
  );
  main_channel.port1.addListener(
    "message",
    postMainMesasgeToRenderMessage.bind(null, main_channel.port1, render_port)
  );

  main_channel.port1.addListener("close", () => {
    render_port.close();
  });
  main_channel.port1.start();
  render_port.start();

  updateMainMessageListener(main_channel.port2, "addListener", 1);
  updateMainMessageListener(main_channel.port2, "on", 1);
  updateMainMessageListener(main_channel.port2, "once", 1);
  updateMainMessageListener(main_channel.port2, "removeListener", 1);
  updateMainMessageListener(main_channel.port2, "off", 1);
  updateMainPostMessage(main_channel.port2);
  return main_channel.port2;
};
