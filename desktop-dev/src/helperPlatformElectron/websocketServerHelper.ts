import { Buffer } from "node:buffer";
import { Duplex } from "node:stream";
import _ws from "ws";

const ws = _ws as any;

interface $WebSocketSender {
  ping(): void;
  pong(): void;
  send(data: string | Uint8Array): void;
  close(): void;
}
interface $WebSocketSenderConstructor {
  new (socket: Duplex): $WebSocketSender;
}
interface $WebSocketReceiver extends NodeJS.WritableStream {
  on(type: "conclude", cb: $WebSocketReceiver.OnConclude): this;
  on(type: "drain", cb: $WebSocketReceiver.OnDrain): this;
  on(type: "error", cb: $WebSocketReceiver.OnError): this;
  on(type: "message", cb: $WebSocketReceiver.OnMessage): this;
  on(type: "ping", cb: $WebSocketReceiver.OnPing): this;
  on(type: "pong", cb: $WebSocketReceiver.OnPong): this;
}
interface $WebSocketReceiverConstructor {
  new (options?: { binaryType?: "nodebuffer" | "arraybuffer" | "fragments" }): $WebSocketReceiver;
}

// deno-lint-ignore no-namespace
namespace $WebSocketReceiver {
  export interface OnConclude {
    (code: number, reason: any): void;
  }
  export interface OnDrain {
    (): void;
  }
  export interface OnError {
    (err: unknown): void;
  }
  export interface OnMessage {
    (data: string, isBinary: false): void;
    (data: Buffer, isBinary: true): void;
  }

  export interface OnPing {
    (data: unknown): void;
  }
  export interface OnPong {
    (data: unknown): void;
  }
}

export const Sender = ws.Sender as $WebSocketSenderConstructor;
export const Receiver = ws.Receiver as $WebSocketReceiverConstructor;
export const WebSocketServer = _ws.WebSocketServer;
export const WebSocketClient = _ws.WebSocket;
