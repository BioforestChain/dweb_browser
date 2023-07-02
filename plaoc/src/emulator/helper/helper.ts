export { streamRead } from "../../client/helper/readableStreamHelper.ts";
import { $MMID, IPC_ROLE, ReadableStreamIpc } from "../../../deps.ts";
import { streamRead } from "../../client/helper/readableStreamHelper.ts";
import { X_EMULATOR_ACTION, X_PLAOC_QUERY } from "../../server/const.ts";
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

// 回复信息给后端
export const createMockModuleServerIpc = async (
  mmid: $MMID,
  apiUrl = BASE_URL
) => {
  const createUrl = new URL(apiUrl);
  createUrl.searchParams.set("mmid", mmid);
  const mmidStreamUrl = new URL(await (await fetch(createUrl)).text());

  const csUrl = new URL(mmidStreamUrl);
  {
    csUrl.searchParams.set("type", X_EMULATOR_ACTION.CLIENT_2_SERVER);
  }
  const scUrl = new URL(mmidStreamUrl);
  {
    scUrl.searchParams.set("type", X_EMULATOR_ACTION.SERVER_2_CLIENT);
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
      void fetch(csUrl, {
        method: "POST",
        body: chunk,
      });
    }
  })();

  const scRes = await fetch(scUrl);
  streamIpc.bindIncomeStream(scRes.body!);

  return streamIpc;
};
