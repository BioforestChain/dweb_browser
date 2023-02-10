export type {
  IncomingMessage as WebServerRequest,
  ServerResponse as WebServerResponse,
} from "node:http";

import type { Ipc } from "../../core/ipc/ipc.cjs";
export interface $GetHostOptions {
  ipc: Ipc;
  port?: number;
  subdomain?: string;
}
