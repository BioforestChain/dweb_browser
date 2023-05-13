import type { Ipc } from "../../core/ipc/index.cjs";
import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs";
import type { $Schema1ToType, $Schema2 } from "../../helper/types.cjs";
import { hexaToRGBA,  converRGBAToHexa} from "../plugins/helper.cjs";
import type { $BarState } from "./assets/types";
import type { MultiWebviewNMM } from "./multi-webview.mobile.cjs"
// @ts-ignore
type $APIS = typeof import("./assets/multi-webview.html.mjs")["APIS"];
/**
 * 设置状态栏
 * @param this 
 * @param root_url 
 * @param args 
 * @param clientIpc 
 * @param request 
 * @returns 
 */
export async function barGetState<
  $ApiKeyName extends keyof Pick<$APIS, "statusBarGetState" | "navigationBarGetState">
>(
  this: MultiWebviewNMM, 
  apiksKeyName: $ApiKeyName,
  root_url: string,
  args: $Schema1ToType<{}>,
  clientIpc: Ipc,
  request: IpcRequest,
){
  
  const apis = this.apisGetFromFocused()
  if(apis === undefined) throw new Error(`wapi === undefined`);
  const state = await apis[apiksKeyName]();
  return {
    ...state,
    color: hexaToRGBA(state.color)
  }
}

/**
 * 设置状态
 * @param this 
 * @param root_url 
 * @param args 
 * @param clientIpc 
 * @param request 
 * @returns 
 */
export async function barSetState<
  $ApiKeyName extends keyof Pick<$APIS, "statusBarSetState" | "navigationBarSetState">
>(
  this: MultiWebviewNMM, 
  apiKeyName: $ApiKeyName,
  root_url: string,
  args: $Schema1ToType<{}>,
  clientIpc: Ipc,
  request: IpcRequest,
){
  let state: $BarState | undefined = undefined;
  const apis = this.apisGetFromFocused()
  if(apis === undefined) throw new Error(`wapi === undefined`);
  const color = request.parsed_url.searchParams.get('color');
  if(color){
    const colorObj = JSON.parse(color);
    const colorHexa = converRGBAToHexa(
      colorObj.red, colorObj.green, colorObj.blue, colorObj.alpha
    );
    state = await apis[apiKeyName]('color', colorHexa);
  }

  const visible = request.parsed_url.searchParams.get("visible")
  if(visible){
    state = await apis[apiKeyName](
      'visible', visible === "true" ? true : false
    );
  }

  const style = request.parsed_url.searchParams.get('style')
  if(style){
    state = await apis[apiKeyName]('style', style);
  }

  const overlay = request.parsed_url.searchParams.get('overlay')
  if(overlay){
    state = await apis[apiKeyName](
      'overlay', overlay === "true" ? true : false
    );
  }

  if(state){
    return {
      ...state,
      color: hexaToRGBA(state.color)
    }
  }
}