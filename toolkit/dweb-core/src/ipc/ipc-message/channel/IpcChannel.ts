import { mapHelper } from "@dweb-browser/helper/fun/mapHelper.ts";
import type { Ipc } from "../../ipc.ts";
import { PURE_CHANNEL_EVENT_PREFIX } from "./PureChannel.ts";

export const X_IPC_UPGRADE_KEY = "X-Dweb-Ipc-Upgrade-Key";
export const headers_ipc_channel_wm = new WeakMap<Headers, Promise<Ipc>>();
export const getIpcChannel = (headers: Headers) => headers_ipc_channel_wm.get(headers);

export const prepareIpcChannel = (ipc: Ipc, headers: Headers, channelIpc?: Ipc | Promise<Ipc>) => {
  return mapHelper.getOrPut(headers_ipc_channel_wm, headers, () => {
    if (channelIpc === undefined) {
      channelIpc = ipc.fork();
    }
    return Promise.resolve(channelIpc).then((ipc) => {
      headers.set(X_IPC_UPGRADE_KEY, `${PURE_CHANNEL_EVENT_PREFIX}${ipc.pid}`);
      return ipc;
    });
  });
};
