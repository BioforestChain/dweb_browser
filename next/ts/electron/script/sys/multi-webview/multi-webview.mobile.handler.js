"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.biometricsMock = exports.haptics = exports.torchState = exports.toggleTorch = exports.shareShare = exports.toastShow = exports.virtualKeyboardSetState = exports.virtualKeyboardGetState = exports.safeAreaSetState = exports.safeAreaGetState = exports.barSetState = exports.barGetState = exports.openDownloadPage = exports.closeFocusedWindow = exports.open = void 0;
const helper_js_1 = require("../plugins/helper.js");
/**
* 打开 应用
* 如果 是由 jsProcdss 调用 会在当前的 browserWindow 打开一个新的 webview
* 如果 是由 NMM 调用的 会打开一个新的 borserWindow 同时打开一个新的 webview
*/
async function open(root_url, args, clientIpc, request) {
    const wapis = await this.forceGetWapis(clientIpc, root_url);
    const webview_id = await wapis.apis.openWebview(args.url);
    return webview_id;
}
exports.open = open;
/**
 * 关闭当前激活项
 * @param this
 * @param root_url
 * @param args
 * @param clientIpc
 * @param request
 * @returns
 */
async function closeFocusedWindow(root_url, args, clientIpc, request) {
    const iterator = this._uid_wapis_map.entries();
    for (let item of iterator) {
        if (item[1].nww?.isFocused()) {
            item[1].nww.close();
            this._uid_wapis_map.delete(item[0]);
        }
    }
    return true;
}
exports.closeFocusedWindow = closeFocusedWindow;
async function openDownloadPage(root_url, args, clientIpc, request) {
    console.log(await request.body.text());
    const metadataUrl = JSON.parse(await request.body.text())?.metadataUrl;
    const apis = await this.apisGetFromFocused();
    const targetUrl = `${args.url}&metadataUrl=${metadataUrl}`;
    if (apis === undefined) {
        throw new Error(`apis === undefined`);
    }
    const webview_id = await apis.openWebview(targetUrl);
    return {};
}
exports.openDownloadPage = openDownloadPage;
/**
 * 设置状态栏
 * @param this
 * @param root_url
 * @param args
 * @param clientIpc
 * @param request
 * @returns
 */
async function barGetState(apiksKeyName, root_url, args, clientIpc, request) {
    const apis = this.apisGetFromFocused();
    if (apis === undefined)
        throw new Error(`wapi === undefined`);
    const state = await apis[apiksKeyName]();
    return {
        ...state,
        color: (0, helper_js_1.hexaToRGBA)(state.color)
    };
}
exports.barGetState = barGetState;
/**
 * 设置状态
 * @param this
 * @param root_url
 * @param args
 * @param clientIpc
 * @param request
 * @returns
 */
async function barSetState(apiKeyName, root_url, args, clientIpc, request) {
    let state = undefined;
    const apis = this.apisGetFromFocused();
    if (apis === undefined)
        throw new Error(`wapi === undefined`);
    const color = request.parsed_url.searchParams.get('color');
    if (color) {
        const colorObj = JSON.parse(color);
        const colorHexa = (0, helper_js_1.converRGBAToHexa)(colorObj.red, colorObj.green, colorObj.blue, colorObj.alpha);
        state = await apis[apiKeyName]('color', colorHexa);
    }
    const visible = request.parsed_url.searchParams.get("visible");
    if (visible) {
        state = await apis[apiKeyName]('visible', visible === "true" ? true : false);
    }
    const style = request.parsed_url.searchParams.get('style');
    if (style) {
        state = await apis[apiKeyName]('style', style);
    }
    const overlay = request.parsed_url.searchParams.get('overlay');
    if (overlay) {
        state = await apis[apiKeyName]('overlay', overlay === "true" ? true : false);
    }
    if (state) {
        return {
            ...state,
            color: (0, helper_js_1.hexaToRGBA)(state.color)
        };
    }
}
exports.barSetState = barSetState;
async function safeAreaGetState(root_url, args, clientIpc, request) {
    const apis = this.apisGetFromFocused();
    if (apis === undefined)
        throw new Error(`wapi === undefined`);
    const state = await apis.safeAreaGetState();
    return {
        ...state,
    };
}
exports.safeAreaGetState = safeAreaGetState;
async function safeAreaSetState(root_url, args, clientIpc, request) {
    const apis = this.apisGetFromFocused();
    if (apis === undefined)
        throw new Error(`wapi === undefined`);
    const overlay = request.parsed_url.searchParams.get('overlay');
    if (overlay === null)
        throw new Error(`overlay === null`);
    const state = await apis.safeAreaSetOverlay(overlay === "true" ? true : false);
    return {
        ...state,
    };
}
exports.safeAreaSetState = safeAreaSetState;
async function virtualKeyboardGetState(root_url, args, clientIpc, request) {
    const apis = this.apisGetFromFocused();
    if (apis === undefined)
        throw new Error(`wapi === undefined`);
    const state = await apis.virtualKeyboardGetState();
    return {
        ...state,
    };
}
exports.virtualKeyboardGetState = virtualKeyboardGetState;
async function virtualKeyboardSetState(root_url, args, clientIpc, request) {
    const apis = this.apisGetFromFocused();
    if (apis === undefined)
        throw new Error(`wapi === undefined`);
    const overlay = request.parsed_url.searchParams.get('overlay');
    if (overlay === null)
        throw new Error(`overlay === null`);
    const state = await apis.virtualKeyboardSetOverlay(overlay === "true" ? true : false);
    return {
        ...state,
    };
}
exports.virtualKeyboardSetState = virtualKeyboardSetState;
async function toastShow(root_url, args, clientIpc, request) {
    const apis = this.apisGetFromFocused();
    if (apis === undefined)
        throw new Error(`wapi === undefined`);
    const searchParams = request.parsed_url.searchParams;
    const message = searchParams.get('message');
    let duration = searchParams.get('duration');
    const position = searchParams.get('position');
    if (message === null || duration === null || position === null)
        throw new Error(`message === null || duration === null || position === null`);
    await apis.toastShow(message, duration === "short" ? "1000" : "2000", position);
    return true;
}
exports.toastShow = toastShow;
async function shareShare(root_url, args, clientIpc, request) {
    const apis = this.apisGetFromFocused();
    if (apis === undefined)
        throw new Error(`wapi === undefined`);
    const searchParams = request.parsed_url.searchParams;
    const title = searchParams.get("title");
    const text = searchParams.get('text');
    const link = searchParams.get('url');
    apis.shareShare({
        title: title === null ? "" : title,
        text: text === null ? "" : text,
        link: link === null ? "" : link,
        src: "",
    });
    return true;
}
exports.shareShare = shareShare;
async function toggleTorch(root_url, args, clientIpc, request) {
    const apis = this.apisGetFromFocused();
    if (apis === undefined)
        throw new Error(`wapi === undefined`);
    return await apis.torchStateToggle();
}
exports.toggleTorch = toggleTorch;
async function torchState(root_url, args, clientIpc, request) {
    const apis = this.apisGetFromFocused();
    if (apis === undefined)
        throw new Error(`wapi === undefined`);
    return await apis.torchStateGet();
}
exports.torchState = torchState;
async function haptics(root_url, args, clientIpc, request) {
    const query = request.parsed_url.searchParams;
    let str = "";
    if (args.action === "impactLight"
        || args.action === "notification") {
        str = `${args.action} : ${query.get('style')}`;
    }
    else if (args.action === "vibrateClick"
        || args.action === "vibrateDisabled"
        || args.action === "vibrateDoubleClick"
        || args.action === "vibrateHeavyClick"
        || args.action === "vibrateTick") {
        str = `${args.action}`;
    }
    else {
        str = `${args.action} : ${query.get("duration")}`;
    }
    const apis = this.apisGetFromFocused();
    if (apis === undefined)
        throw new Error(`wapi === undefined`);
    return await apis.hapticsSet(str);
}
exports.haptics = haptics;
async function biometricsMock(root_url, args, clientIpc, request) {
    const apis = this.apisGetFromFocused();
    if (apis === undefined)
        throw new Error(`wapi === undefined`);
    return await apis.biometricsMock();
}
exports.biometricsMock = biometricsMock;
