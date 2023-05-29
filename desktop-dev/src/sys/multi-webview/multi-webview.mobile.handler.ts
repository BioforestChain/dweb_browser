// deno-lint-ignore-file ban-types
import type { IpcRequest } from "../../core/ipc/IpcRequest.ts";
import type { Ipc } from "../../core/ipc/index.ts";
import { MicroModule } from "../../core/micro-module.ts";
import type { $Schema1ToType } from "../../helper/types.ts";
import { converRGBAToHexa, hexaToRGBA } from "../plugins/helper.ts";
import {
  apisGetFromMmid,
  deleteWapis,
  forceGetWapis,
} from "./mutil-webview.mobile.wapi.ts";
import type { $BarState, $ToastPosition } from "./types.ts";

type $APIS = typeof import("./assets/multi-webview.html.ts")["APIS"];

/**
 * 打开 应用
 * 如果 是由 jsProcdss 调用 会在当前的 browserWindow 打开一个新的 webview
 * 如果 是由 NMM 调用的 会打开一个新的 borserWindow 同时打开一个新的 webview
 */
export async function open(
  this: MicroModule,
  root_url: string,
  args: $Schema1ToType<{ url: "string" }>,
  clientIpc: Ipc,
  _request: IpcRequest
) {
  const wapis = await forceGetWapis(clientIpc, root_url);
  const webview_id = await wapis.apis.openWebview(args.url);
  return webview_id;
}

/**
 * 关闭当前激活项
 * @param this
 * @param _root_url
 * @param _args
 * @param _clientIpc
 * @param _request
 * @returns
 */
export async function closeFocusedWindow(
  this: MicroModule,
  _args: $Schema1ToType<{}>,
  _clientIpc: Ipc,
  _request: IpcRequest
) {
  deleteWapis((wapi) => {
    return wapi.nww.isFocused();
  });
  return true;
}

export async function openDownloadPage(
  this: MicroModule,
  args: $Schema1ToType<{ url: "string" }>,
  _clientIpc: Ipc,
  request: IpcRequest
) {
  const metadataUrl = JSON.parse(await request.body.text())?.metadataUrl;
  const apis = await apisGetFromMmid(_clientIpc.remote.mmid);
  const targetUrl = `${args.url}&metadataUrl=${metadataUrl}`;
  if (apis === undefined) {
    throw new Error(`apis === undefined`);
  }
  const webview_id = await apis.openWebview(targetUrl);
  return {};
}

/**
 * 设置状态栏
 * @param this
 * @param _root_url
 * @param _args
 * @param _clientIpc
 * @param _request
 * @returns
 */
export async function barGetState<
  $ApiKeyName extends keyof Pick<
    $APIS,
    "statusBarGetState" | "navigationBarGetState"
  >
>(
  this: MicroModule,
  apiksKeyName: $ApiKeyName,
  _args: $Schema1ToType<{}>,
  _clientIpc: Ipc,
  _request: IpcRequest
) {
  const apis = apisGetFromMmid(_clientIpc.remote.mmid);
  if (apis === undefined) throw new Error(`wapi === undefined`);
  const state = await apis[apiksKeyName]();
  return {
    ...state,
    color: hexaToRGBA(state.color),
  };
}

/**
 * 设置状态
 * @param this
 * @param _root_url
 * @param _args
 * @param _clientIpc
 * @param request
 * @returns
 */
export async function barSetState<
  $ApiKeyName extends keyof Pick<
    $APIS,
    "statusBarSetState" | "navigationBarSetState"
  >
>(
  this: MicroModule,
  apiKeyName: $ApiKeyName,
  _args: $Schema1ToType<{}>,
  _clientIpc: Ipc,
  request: IpcRequest
) {
  let state: $BarState | undefined = undefined;
  const apis = apisGetFromMmid(_clientIpc.remote.mmid);
  if (apis === undefined) throw new Error(`wapi === undefined`);
  const color = request.parsed_url.searchParams.get("color");
  if (color) {
    const colorObj = JSON.parse(color);
    const colorHexa = converRGBAToHexa(
      colorObj.red,
      colorObj.green,
      colorObj.blue,
      colorObj.alpha
    );
    state = await apis[apiKeyName]("color", colorHexa);
  }

  const visible = request.parsed_url.searchParams.get("visible");
  if (visible) {
    state = await apis[apiKeyName](
      "visible",
      visible === "true" ? true : false
    );
  }

  const style = request.parsed_url.searchParams.get("style");
  if (style) {
    state = await apis[apiKeyName]("style", style);
  }

  const overlay = request.parsed_url.searchParams.get("overlay");
  if (overlay) {
    state = await apis[apiKeyName](
      "overlay",
      overlay === "true" ? true : false
    );
  }

  if (state) {
    return {
      ...state,
      color: hexaToRGBA(state.color),
    };
  }
}

export async function safeAreaGetState(
  this: MicroModule,
  _args: $Schema1ToType<{}>,
  _clientIpc: Ipc,
  _request: IpcRequest
) {
  const apis = apisGetFromMmid(_clientIpc.remote.mmid);
  if (apis === undefined) throw new Error(`wapi === undefined`);
  const state = await apis.safeAreaGetState();
  return {
    ...state,
  };
}

export async function safeAreaSetState(
  this: MicroModule,
  _args: $Schema1ToType<{}>,
  _clientIpc: Ipc,
  request: IpcRequest
) {
  const apis = apisGetFromMmid(_clientIpc.remote.mmid);
  if (apis === undefined) throw new Error(`wapi === undefined`);
  const overlay = request.parsed_url.searchParams.get("overlay");
  if (overlay === null) throw new Error(`overlay === null`);
  const state = await apis.safeAreaSetOverlay(
    overlay === "true" ? true : false
  );
  return {
    ...state,
  };
}

export async function virtualKeyboardGetState(
  this: MicroModule,
  _args: $Schema1ToType<{}>,
  _clientIpc: Ipc,
  _request: IpcRequest
) {
  const apis = apisGetFromMmid(_clientIpc.remote.mmid);
  if (apis === undefined) throw new Error(`wapi === undefined`);
  const state = await apis.virtualKeyboardGetState();
  return {
    ...state,
  };
}

export async function virtualKeyboardSetState(
  this: MicroModule,
  _args: $Schema1ToType<{}>,
  _clientIpc: Ipc,
  request: IpcRequest
) {
  const apis = apisGetFromMmid(_clientIpc.remote.mmid);
  if (apis === undefined) throw new Error(`wapi === undefined`);
  const overlay = request.parsed_url.searchParams.get("overlay");
  if (overlay === null) throw new Error(`overlay === null`);
  const state = await apis.virtualKeyboardSetOverlay(
    overlay === "true" ? true : false
  );
  return {
    ...state,
  };
}

export async function toastShow(
  this: MicroModule,
  _args: $Schema1ToType<{}>,
  _clientIpc: Ipc,
  request: IpcRequest
) {
  const apis = apisGetFromMmid(_clientIpc.remote.mmid);
  if (apis === undefined) throw new Error(`wapi === undefined`);
  const searchParams = request.parsed_url.searchParams;
  const message = searchParams.get("message");
  const duration = searchParams.get("duration");
  const position = searchParams.get("position");

  if (message === null || duration === null || position === null)
    throw new Error(
      `message === null || duration === null || position === null`
    );
  await apis.toastShow(
    message,
    duration === "short" ? "1000" : "2000",
    position as $ToastPosition
  );
  return true;
}

export async function shareShare(
  this: MicroModule,
  _args: $Schema1ToType<{}>,
  _clientIpc: Ipc,
  request: IpcRequest
) {
  const apis = apisGetFromMmid(_clientIpc.remote.mmid);
  if (apis === undefined) throw new Error(`wapi === undefined`);
  const searchParams = request.parsed_url.searchParams;
  const title = searchParams.get("title");
  const text = searchParams.get("text");
  const link = searchParams.get("url");
  debugger;
  const body = await request.body.u8a();
  console.log("share-body:", body);
  apis.shareShare({
    title: title === null ? "" : title,
    text: text === null ? "" : text,
    link: link === null ? "" : link,
    src: "",
    body: await request.body.u8a(),
    bodyType: request.headers.get("content-type") as string,
  });
  return true;
}

export async function toggleTorch(
  this: MicroModule,
  _args: $Schema1ToType<{}>,
  _clientIpc: Ipc,
  _request: IpcRequest
) {
  const apis = apisGetFromMmid(_clientIpc.remote.mmid);
  if (apis === undefined) throw new Error(`wapi === undefined`);

  return await apis.torchStateToggle();
}

export async function torchState(
  this: MicroModule,
  _args: $Schema1ToType<{}>,
  _clientIpc: Ipc,
  _request: IpcRequest
) {
  const apis = apisGetFromMmid(_clientIpc.remote.mmid);
  if (apis === undefined) throw new Error(`wapi === undefined`);
  return await apis.torchStateGet();
}

export async function haptics(
  this: MicroModule,
  args: $Schema1ToType<{ action: "string" }>,
  _clientIpc: Ipc,
  request: IpcRequest
) {
  const query = request.parsed_url.searchParams;
  let str = "";
  if (args.action === "impactLight" || args.action === "notification") {
    str = `${args.action} : ${query.get("style")}`;
  } else if (
    args.action === "vibrateClick" ||
    args.action === "vibrateDisabled" ||
    args.action === "vibrateDoubleClick" ||
    args.action === "vibrateHeavyClick" ||
    args.action === "vibrateTick"
  ) {
    str = `${args.action}`;
  } else {
    str = `${args.action} : ${query.get("duration")}`;
  }
  const apis = apisGetFromMmid(_clientIpc.remote.mmid);
  if (apis === undefined) throw new Error(`wapi === undefined`);
  return await apis.hapticsSet(str);
}

export async function biometricsMock(
  this: MicroModule,
  _args: $Schema1ToType<{}>,
  _clientIpc: Ipc,
  _request: IpcRequest
) {
  const apis = apisGetFromMmid(_clientIpc.remote.mmid);
  if (apis === undefined) throw new Error(`wapi === undefined`);
  return (await apis.biometricsMock()) as boolean;
}
