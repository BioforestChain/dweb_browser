export { streamRead } from "../../client/helper/readableStreamHelper.ts";
import { $MMID, IPC_ROLE, ReadableStreamIpc } from "../../../deps.ts";
import { streamRead } from "../../client/helper/readableStreamHelper.ts";
import { X_PLAOC_QUERY } from "../../server/const.ts";
export const EMULATOR = "/emulator";

export function isShadowRoot(o: ShadowRoot | unknown): o is ShadowRoot {
  return typeof o === "object" && o !== null && "host" in o && "mode" in o;
}

export function isHTMLElement(o: HTMLElement | unknown): o is HTMLElement {
  return o instanceof HTMLElement;
}

export function isCSSStyleDeclaration(
  o: CSSStyleDeclaration | unknown
): o is CSSStyleDeclaration {
  return o instanceof CSSStyleDeclaration;
}
export type EmulatorAction = "connect" | "response";

const BASE_URL = new URL(
  new URLSearchParams(location.search).get(X_PLAOC_QUERY.API_INTERNAL_URL)!
);
BASE_URL.pathname = EMULATOR;

export const fetchResponse = Object.assign(Response, {
  FORBIDDEN: () => Response.json("Forbidden", { status: 403 }),
  BAD_REQUEST: () => Response.json("Bad Request", { status: 400 }),
  INTERNAL_SERVER_ERROR: (message = "Internal Server Error") =>
    Response.json(message, { status: 500 }),
});

// 回复信息给后端
export const createStreamIpc = async (mmid: $MMID, apiUrl = BASE_URL) => {
  const csUrl = new URL(apiUrl);
  {
    csUrl.searchParams.set("type", "client2server");
    csUrl.searchParams.set("mmid", mmid);
  }
  const scUrl = new URL(apiUrl);
  {
    scUrl.searchParams.set("type", "server2client");
    scUrl.searchParams.set("mmid", mmid);
  }

  const streamIpc = new ReadableStreamIpc(
    {
      mmid,
      ipc_support_protocols: {
        message_pack: false,
        protobuf: false,
        raw: false,
      },
      dweb_deeplinks: [],
    },
    IPC_ROLE.CLIENT
  );
  (async () => {
    for await (const chunk of streamRead(streamIpc.stream)) {
      console.log("chunk", chunk);
      // await fetch(csUrl, {
      //   method: "POST",
      //   body: chunk,
      // });
    }
  })();

  const scRes = await fetch(scUrl);
  streamIpc.bindIncomeStream(scRes.body!);

  return streamIpc;
};
