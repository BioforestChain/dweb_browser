"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.RenderPortToMainPort = exports.MainPortToRenderPort = exports.postRenderMessageToMainMesasge = exports.postMainMesasgeToRenderMessage = void 0;
const electron_1 = require("electron");
const electronMainPort_js_1 = require("./electronMainPort.js");
const electronRenderPort_js_1 = require("./electronRenderPort.js");
const postMainMesasgeToRenderMessage = (from_port, to_port, event) => {
    let transfer;
    if (event.ports.length > 0) {
        transfer = event.ports.map(exports.MainPortToRenderPort);
    }
    if (transfer !== undefined) {
        to_port.postMessage(event.data, transfer);
    }
    else {
        to_port.postMessage(event.data);
    }
};
exports.postMainMesasgeToRenderMessage = postMainMesasgeToRenderMessage;
const postRenderMessageToMainMesasge = (from_port, to_port, event) => {
    let ports;
    if (event.ports.length > 0) {
        ports = event.ports.map(exports.RenderPortToMainPort);
    }
    if (ports) {
        to_port.postMessage(event.data, ports);
    }
    else {
        to_port.postMessage(event.data);
    }
};
exports.postRenderMessageToMainMesasge = postRenderMessageToMainMesasge;
const MainPortToRenderPort = (main_port) => {
    const render_channel = new MessageChannel();
    main_port.addListener("message", exports.postMainMesasgeToRenderMessage.bind(null, main_port, render_channel.port1));
    render_channel.port1.addEventListener("message", exports.postRenderMessageToMainMesasge.bind(null, render_channel.port1, main_port));
    main_port.addListener("close", () => {
        render_channel.port1.close();
    });
    main_port.start();
    render_channel.port1.start();
    (0, electronRenderPort_js_1.updateRenderMessageListener)(render_channel.port2, "addEventListener", 1);
    (0, electronRenderPort_js_1.updateRenderMessageListener)(render_channel.port2, "removeEventListener", 1);
    (0, electronRenderPort_js_1.updateRenderPostMessage)(render_channel.port2);
    return render_channel.port2;
};
exports.MainPortToRenderPort = MainPortToRenderPort;
const RenderPortToMainPort = (render_port) => {
    const main_channel = new electron_1.MessageChannelMain();
    render_port.addEventListener("message", exports.postRenderMessageToMainMesasge.bind(null, render_port, main_channel.port1));
    main_channel.port1.addListener("message", exports.postMainMesasgeToRenderMessage.bind(null, main_channel.port1, render_port));
    main_channel.port1.addListener("close", () => {
        render_port.close();
    });
    main_channel.port1.start();
    render_port.start();
    (0, electronMainPort_js_1.updateMainMessageListener)(main_channel.port2, "addListener", 1);
    (0, electronMainPort_js_1.updateMainMessageListener)(main_channel.port2, "on", 1);
    (0, electronMainPort_js_1.updateMainMessageListener)(main_channel.port2, "once", 1);
    (0, electronMainPort_js_1.updateMainMessageListener)(main_channel.port2, "removeListener", 1);
    (0, electronMainPort_js_1.updateMainMessageListener)(main_channel.port2, "off", 1);
    (0, electronMainPort_js_1.updateMainPostMessage)(main_channel.port2);
    return main_channel.port2;
};
exports.RenderPortToMainPort = RenderPortToMainPort;
