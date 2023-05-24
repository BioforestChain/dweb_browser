/**
 * app 容器组件
 * - status-bar
 * - ...其他插件都在这里注入
 * - 用来模拟移动端硬件外壳
 * - 仅仅只有 UI 不提供非 UI 之外的任何交互功能
 */
import { LitElement } from "lit";
import type { $ShareOptions } from "../../types.js";
export declare class MultiWebViewCompMobileShell extends LitElement {
    static styles: import("lit").CSSResult[];
    appContentContainer: HTMLDivElement | undefined | null;
    /**
     *
     * @param message
     * @param duration
     * @param position
     */
    toastShow(message: string, duration: string, position: "top" | "bottom"): void;
    setHostStyle(): void;
    biometricsMock(): void;
    hapticsMock(text: string): void;
    shareShare(options: $ShareOptions): void;
    protected render(): unknown;
}
