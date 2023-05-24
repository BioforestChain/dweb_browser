import { decode } from "@msgpack/msgpack";
import { $messageToIpcMessage, } from "./$messageToIpcMessage.js";
export const $messagePackToIpcMessage = (data, ipc) => {
    return $messageToIpcMessage(decode(data), ipc);
};
