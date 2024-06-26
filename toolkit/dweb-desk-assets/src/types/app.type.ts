import type { $MMID, $MicroModuleManifest } from "@dweb-browser/core/types.ts";

export interface $WidgetAppData extends $MicroModuleManifest {
  running: boolean;
  /**
   * 当前进程所拥有的窗口的状态
   */
  winStates: $WindowState[];
  targetType?: string;
  minTarget?: Int;
  maxTarget?: Int;
}

type Int = number;
type Float = number;
type $UUID = string;

/**
 * 单个窗口的状态集
 */
export interface $WindowState {
  /**
   * 窗口全局唯一编号，属于UUID的格式
   */
  wid: $UUID;
  /**
   * 窗口持有者
   *
   * 窗口创建者
   */
  owner: $MMID;
  /**
   * 内容提提供方
   *
   * 比如若渲染的是web内容，那么应该是 mwebview.browser.dweb
   */
  provider: $MMID;
  /**
   * 窗口位置和大小
   *
   * 窗口会被限制最小值，会被限制显示区域。
   * 终止，窗口最终会被绘制在用户可见可控的区域中
   */
  bounds: $Rectangle;
  /**
   * 窗口状态
   */
  mode: $WindowMode;
  /**
   * 当前是否缩放窗口
   */
  resizable: boolean;
  /**
   * 是否聚焦
   *
   * 目前只会有一个窗口被聚焦，未来实现多窗口联合显示的时候，就可能会有多个窗口同时focus，但这取决于所处宿主操作系统的支持。
   */
  focus: boolean;
  /**
   * 是否可见
   */
  visible: boolean;
  /**
   * 当前窗口层叠顺序
   */
  zIndex: Int;
  /**
   * 子窗口
   */
  children: $UUID[];
  /**
   * 父窗口
   */
  parent: undefined | $UUID;
  /**
   * 是否在闪烁提醒
   *
   * > 类似 macos 中的图标弹跳、windows 系统中的窗口闪烁。
   * 在 taskbar 中， running-dot 会闪烁变色
   */
  flashing: boolean;
  /**
   * 闪烁的颜色
   *
   * 可以通过接口配置该颜色
   */
  flashColor: string;
  /**
   * 进度条
   *
   * 范围为 `[0～1]`
   * 如果小于0（通常为 -1），那么代表没有进度条信息，否则将会在taskbar中显示它的进度信息
   */
  progressBar: Float;
  /**
   * 是否置顶显示
   *
   * 这与 zIndex 不冲突，置顶只是一个优先渲染的层级，可以简单理解成 `zIndex+1000`
   *
   * > 前期我们应该不会在移动设备上开放这个接口，因为移动设备的可用空间非常有限，如果允许任意窗口置顶，那么用户体验将会非常糟。
   * > 如果需要置顶功能，可以考虑使用 pictureInPicture
   */
  alwaysOnTop: boolean;
  /**
   * 当前窗口所属的桌面
   * 目前有 0 和 1 两个桌面，其中 0 为 taskbar 中的 toogleDesktopButton 开关所代表的 “临时桌面”。
   * 目前，点击 toogleDesktopButton 的效果就是将目前打开的窗口都收纳入“临时桌面”；
   * 如果“临时桌面”中存在暂存的窗口，那么此时点击“临时桌面”，这些暂存窗口将恢复到“当前桌面”。
   *
   * 未来会实现将窗口拖拽到“临时桌面”中，这样可以实现在多个桌面中移动窗口
   *
   * 默认是 1
   */
  desktopIndex: Int;
  /**
   * 当前窗口所在的屏幕
   *
   * > 配合 getScreens 接口，就能获得当前屏幕的详细信息。
   * > 未来实现多设备互联时，可以实现窗口的多设备流转
   * > 屏幕与桌面是两个独立的概念
   */
  screenId: Int;
  /**
   * 所支持运行时最小版本号
   */
  minTarget?: Int;
  /**
   * 所支持运行时最大版本号
   */
  maxTarget?: Int;
}

/**
 * 浮动模式，默认值
 * 最大化
 * 最小化
 * 全屏
 *
 * 画中画模式
 * 是否处于画中画模式
 * 与原生的 PIP 不同，原生的PIP有一些限制，比如 IOS 上只能渲染 Media。
 * 而在 desk 中的 PIP 原理简单粗暴，就是将视图进行 clip+scale，因此它本质上还是渲染一整个 win-view。
 * 并且此时这个被裁切的窗口将无法接收到任何用户的手势、键盘等输入，却而代之的，接口中允许一些自定义1～4个的 icon-button，这些按钮将会被渲染在 pip-controls-bar （PIP 的下方） 中方便用户操控当前的 PIP。
 *
 * 多个 PIP 视图会被叠在一起，在一个 max-width == max-height 的空间中，所有 PIP 以 contain 的显示形式居中放置。
 *    只会有一个 PIP 视图显示在最前端，其它层叠显示在它后面
 * PIP 可以全局拖动。
 * PIP 会有两个状态：聚焦和失焦。
 *    点击后，进入聚焦模式，视图轻微放大，pip-controlls-bar 会从 PIP 视图的 Bottom 与 End 侧 显示出来；
 *        其中 Bottom 侧显示的是用户自定义的 icon-button，以 space-around 的显示形式水平放置；
 *        同时 End 侧显示的是 PIP 的 dot-scroll-bar（应该是拟物设计，否则用户认知成本可能会不低），桌面端可以点击或者滚轮滚动、移动端可以上下滑动，从而切换最前端的 PIP 视图
 *        聚焦模式下 PIP 仍然可以全局拖动，但是一旦造成拖动，会同时切换成失焦模式。
 *    在聚焦模式下，再次点击 PIP，将会切换到失焦模式，此时 pip-controlls-bar 隐藏，视图轻微缩小；
 * PIP 的视图的 End-Corner 是一个 resize 区域，用户可以随意拖动这个来对视图进行resize，同时开发者会收到resize指令，从而作出“比例响应式”变更。如果开发者不响应该resize，那么 PIP 会保留 win-view 的渲染比例。
 *
 * > 注意：该模式下，alwaysOnTop 为 true，并且将不再显示 win-controls-bar。
 * > 提示：如果不想 PIP 功能把当前的 win-view  吃掉，那么可以打开一个子窗口来申请 PIP 模式。
 *
 * 窗口关闭
 */
export type $WindowMode = "floating" | "maximize" | "minimize" | "fullscreen" | "picture-in-picture";

export interface $Rectangle {
  left: Int;
  top: Int;
  width: Int;
  height: Int;
}

export interface $TaskBarState {
  /**
   * 是否聚焦到taskBar
   */
  focus: boolean;
  appId: `${string}.dweb`;
}

/**
 * 自定义组件名称
 */
export type $WidgetCustomData = {
  /**
   * 所属应用名称
   *
   * 会有相关的安全性（未来加入签名验证）
   * 会有相关的限制（基于应用的配置）
   * 会有相关的偏好性（deeplink的偏好）
   */
  appId: string;
  /**
   * 组件名称
   */
  widgetName: string;
  templateHtml: string;
  scopedStyle: string;
  size: $TileSize;
  sizeList: $TileSize[];
};
export interface $TileSize {
  row: $TileSizeType;
  column: $TileSizeType;
}

export type $TileSizeType = number | `${number}%`;

// export interface $DeskLinkMetaData extends $MicroModuleManifest {
//   running: boolean;
//   icon:$AppIconInfo,
//   /**
//    * 当前进程所拥有的窗口的状态
//    */
//   winStates: $WindowState[];
// }
