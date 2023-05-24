/// <reference lib="dom" />
import "./multi-webview-content.html.js";
import "./multi-webview-devtools.html.js";
import "./components/multi-webview-comp-status-bar.html.js";
import "./components/multi-webview-comp-mobile-shell.html.js";
import "./components/multi-webview-comp-navigator-bar.html.js";
import "./components/multi-webview-comp-virtual-keyboard.html.js";
import "./components/multi-webview-comp-toast.html.js";
import "./components/multi-webview-comp-barcode-scanning.html.js";
import "./components/multi-webview-comp-biometrics.html.js";
import "./components/multi-webview-comp-haptics.html.js";
import "./components/multi-webview-comp-share.html.js";
import { LitElement } from "lit";
import type { MultiWebViewContent } from "./multi-webview-content.html.js";
import type { $BarState, $ShareOptions, $VirtualKeyboardState } from "../types.js";
import type { MultiWebViewCompMobileShell } from "./components/multi-webview-comp-mobile-shell.html.js";
import type { MultiWebviewCompVirtualKeyboard } from "./components/multi-webview-comp-virtual-keyboard.html.js";
import type { MultiWebviewCompNavigationBar } from "./components/multi-webview-comp-navigator-bar.html.js";
import type { $OverlayState } from "../types.js";
export declare class ViewTree extends LitElement {
    static styles: import("lit").CSSResult[];
    private _id_acc;
    private webviews;
    statusBarHeight: string;
    navigationBarHeight: string;
    virtualKeyboardAnimationSetIntervalId: unknown;
    _multiWebviewContent: MultiWebViewContent[] | undefined;
    multiWebviewCompMobileShell: MultiWebViewCompMobileShell | undefined | null;
    multiWebviewCompVirtualKeyboard: MultiWebviewCompVirtualKeyboard | undefined;
    multiWebviewCompNavigationBar: MultiWebviewCompNavigationBar | undefined;
    name?: string;
    statusBarState: $BarState[];
    navigationBarState: $BarState[];
    safeAreaState: $OverlayState[];
    isShowVirtualKeyboard: boolean;
    virtualKeyboardState: $VirtualKeyboardState;
    torchState: {
        isOpen: boolean;
    };
    preloadAbsolutePath: string;
    barSetState<$PropertyName extends keyof Pick<this, "statusBarState" | "navigationBarState">, K extends keyof $BarState, V extends $BarState[K]>(propertyName: $PropertyName, key: K, value: V): $BarState;
    barGetState<$PropertyName extends keyof Pick<this, "statusBarState" | "navigationBarState">>(propertyName: $PropertyName): $BarState;
    /**
     * 根据 multi-webview-comp-virtual-keyboard 标签
     * 和 navigationBarState.insets.bottom 的值
     * 设置 virtualKeyboardState.inserts.bottom
     */
    virtualKeyboardStateUpdateInsetsByEl(): void;
    virtualKeyboardFirstUpdated(): void;
    virtualKeyboardHideCompleted(): void;
    virtualKeyboardShowCompleted(): void;
    virtualKeyboardSetOverlay(overlay: boolean): {
        overlay: boolean;
        visible: boolean;
        insets: import("../types.js").$Insets;
    };
    virtualKeyboardGetState(): $VirtualKeyboardState;
    toastShow(message: string, duration: string, position: "top" | "bottom"): void;
    safeAreaGetState: () => {
        overlay: boolean;
        insets: {
            left: number;
            top: number;
            right: number;
            bottom: number;
        };
        cutoutInsets: {
            left: number;
            top: number;
            right: number;
            bottom: number;
        };
        outerInsets: {
            left: number;
            top: number;
            right: number;
            bottom: number;
        };
    };
    safeAreaSetOverlay: (overlay: boolean) => {
        overlay: boolean;
        insets: {
            left: number;
            top: number;
            right: number;
            bottom: number;
        };
        cutoutInsets: {
            left: number;
            top: number;
            right: number;
            bottom: number;
        };
        outerInsets: {
            left: number;
            top: number;
            right: number;
            bottom: number;
        };
    };
    safeAreaNeedUpdate: () => void;
    torchStateToggle(): boolean;
    torchStateGet(): boolean;
    barcodeScanningGetPhoto(): void;
    biometricesResolve: {
        (value: unknown): void;
    } | undefined;
    biometricsMock(): Promise<unknown>;
    biometricessPass(b: boolean): void;
    hapticsSet(value: string): boolean;
    shareShare(options: $ShareOptions): void;
    /**
     * navigation-bar 点击 back 的事件处理器
     * 业务逻辑：
     * 向 webview 添加一个 ipc-message 事件监听器
     * 向 webview 注入执行一段 javascript code
     * @returns
     */
    navigationBarOnBack: () => void;
    /** 对webview视图进行状态整理 */
    private _restateWebviews;
    openWebview(src: string): number;
    closeWebview(webview_id: number): boolean;
    closeWindow(): void;
    private _removeWebview;
    private onWebviewReady;
    private onDevtoolReady;
    private deleteTopBarState;
    private deleteTopSafeAreaState;
    private destroyWebview;
    destroyWebviewByHost(host: string): Promise<boolean>;
    destroyWebviewByOrigin(origin: string): Promise<boolean>;
    restartWebviewByHost(host: string): Promise<boolean>;
    /**
     * 根据host执行 javaScript
     * @param host
     * @param code
     */
    executeJavascriptByHost(host: string, code: string): Promise<void>;
    acceptMessageFromWebview(options: {
        origin: string;
        action: string;
        value: string;
    }): Promise<void>;
    preloadAbsolutePathSet(path: string): void;
    webviewTagOnIpcMessageHandlerBack: (e: Event) => void;
    webviewTagOnIpcMessageHandlerNormal: (e: Event) => void;
    render(): import("lit").TemplateResult<1>;
}
export declare const APIS: {
    openWebview: (src: string) => number;
    closeWebview: (webview_id: number) => boolean;
    closeWindow: () => void;
    destroyWebviewByHost: (host: string) => Promise<boolean>;
    restartWebviewByHost: (host: string) => Promise<boolean>;
    executeJavascriptByHost: (host: string, code: string) => Promise<void>;
    acceptMessageFromWebview: (options: {
        origin: string;
        action: string;
        value: string;
    }) => Promise<void>;
    statusBarSetState: (key: keyof $BarState, value: string | boolean | import("../types.js").$Insets) => $BarState;
    statusBarGetState: () => $BarState;
    navigationBarSetState: (key: keyof $BarState, value: string | boolean | import("../types.js").$Insets) => $BarState;
    navigationBarGetState: () => $BarState;
    safeAreaSetOverlay: (overlay: boolean) => {
        overlay: boolean;
        insets: {
            left: number;
            top: number;
            right: number;
            bottom: number;
        };
        cutoutInsets: {
            left: number;
            top: number;
            right: number;
            bottom: number;
        };
        outerInsets: {
            left: number;
            top: number;
            right: number;
            bottom: number;
        };
    };
    safeAreaGetState: () => {
        overlay: boolean;
        insets: {
            left: number;
            top: number;
            right: number;
            bottom: number;
        };
        cutoutInsets: {
            left: number;
            top: number;
            right: number;
            bottom: number;
        };
        outerInsets: {
            left: number;
            top: number;
            right: number;
            bottom: number;
        };
    };
    virtualKeyboardGetState: () => $VirtualKeyboardState;
    virtualKeyboardSetOverlay: (overlay: boolean) => {
        overlay: boolean;
        visible: boolean;
        insets: import("../types.js").$Insets;
    };
    toastShow: (message: string, duration: string, position: "top" | "bottom") => void;
    shareShare: (options: $ShareOptions) => void;
    torchStateToggle: () => boolean;
    torchStateGet: () => boolean;
    hapticsSet: (value: string) => boolean;
    biometricsMock: () => Promise<unknown>;
    preloadAbsolutePathSet: (path: string) => void;
};
