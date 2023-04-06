/**
 * 使用 https://www.typescriptlang.org/play 在线编译
 */
const ALL_PORT = new Map<number, MessagePort>();
let portIdAcc = 1;
const PORTS_ID = new WeakMap<MessagePort, number>();
const getPortId = (port: MessagePort) => {
  let port_id = PORTS_ID.get(port);
  if (port_id === undefined) {
    const current_port_id = portIdAcc++;
    port_id = current_port_id;
    ALL_PORT.set(port_id, port);
    port.onmessage = (event) => {
      webkit.messageHandlers.webMessagePort.postMessage({
        type: 'message',
        id: current_port_id,
        data: event.data,
        ports: event.ports.map(getPortId),
      });
    };
  }
  return port_id;
};

function nativeCreateMessageChannel() {
  const channel = new MessageChannel();
  const port1_id = getPortId(channel.port1);
  const port2_id = getPortId(channel.port2);
  return [port1_id, port2_id];
}
function forceGetPort(port_id: number) {
  const port = ALL_PORT.get(port_id);
  if (port === undefined) {
    throw new Error(`no found messagePort by ref: ${port_id}`);
  }
  return port;
}
function nativePortPostMessage(
  port_id: number,
  data: unknown,
  ports_id: number[]
) {
  const origin_port = forceGetPort(port_id);
  const transfer_ports = ports_id.map(forceGetPort);
  origin_port.postMessage(data, transfer_ports);
}
function nativeStart(port_id: number) {
  const origin_port = forceGetPort(port_id);
  origin_port.start();
}

function nativeWindowPostMessage(data: unknown, ports_id: number[]) {
  const ports = ports_id.map(forceGetPort);
  dispatchEvent(new MessageEvent('message', { data, ports }));
}
