export class WebviewWindow {
  static allWindows = new Map<number, WebviewWindow>();
  static startListens() {
    const tryGet = (id: number, onGet: (win: WebviewWindow) => void) => {
      const win = WebviewWindow.allWindows.get(id);
      if (win) {
        onGet(win);
      }
    };

    __web_browser_api__.onTitleChange = (id, title) =>
      tryGet(id, (win) => {
        const new_title = decodeURIComponent(title);
        if (win._title !== new_title) {
          win._title = new_title;
          win.willChange();
        }
      });

    __web_browser_api__.onIconChange = (id, href) =>
      tryGet(id, (win) => {
        const new_icon = decodeURIComponent(href);
        if (win._icon !== new_icon) {
          win._icon = new_icon;
          win.willChange();
        }
      });

    __web_browser_api__.onUrlChange = (id, url) =>
      tryGet(id, (win) => {
        const new_url = decodeURIComponent(url);
        if (win._url !== new_url) {
          win._url = new_url;
          win.willChange();
        }
      });
  }
  constructor(readonly id: number, readonly willChange: () => void) {
    this._frame = WindowFrame.from(__web_browser_api__.getFrame(this.id));
    this._visible = __web_browser_api__.getVisible(this.id);
    this._zIndex = __web_browser_api__.getZIndex(this.id);
    this._title = __web_browser_api__.getTitle(this.id);
    this._icon = __web_browser_api__.getIcon(this.id);
    this._url = __web_browser_api__.getUrl(this.id);
    WebviewWindow.allWindows.set(id, this);
  }
  private _visible: boolean;
  private _zIndex: number;
  private _title: string;
  private _icon: string;
  private _url: string;
  private _frame: WindowFrame = {
    x: 0,
    y: 0,
    width: 0,
    height: 0,
    round: 0,
  };

  get x() {
    return this._frame.x;
  }
  set x(value) {
    if (this._frame.x !== value) {
      this._frame.x = value;
      this._setFrame();
    }
  }
  get y() {
    return this._frame.y;
  }
  set y(value) {
    if (this._frame.y !== value) {
      this._frame.y = value;
      this._setFrame();
    }
  }
  get width() {
    return this._frame.width;
  }
  set width(value) {
    if (this._frame.width !== value) {
      this._frame.width = value;
      this._setFrame();
    }
  }
  get height() {
    return this._frame.height;
  }
  set height(value) {
    if (this._frame.height !== value) {
      this._frame.height = value;
      this._setFrame();
    }
  }
  get round() {
    return this._frame.round;
  }
  set round(value) {
    if (this._frame.round !== value) {
      this._frame.round = value;
      this._setFrame();
    }
  }

  private _settingFrame = false;
  private _setFrame() {
    if (this._settingFrame) {
      return;
    }
    this._settingFrame = true;
    queueMicrotask(() => {
      this._settingFrame = false;
      const { x, y, width, height, round } = this._frame;
      WebviewWindow.addSetFramesBatch(this.id, x, y, width, height, round);
      //   __web_browser_api__.setFrame(this.id, x, y, width, height, round);
    });
  }
  private static _batchs = new Map<number, string>();
  static addSetFramesBatch(
    id: number,
    x: number,
    y: number,
    width: number,
    height: number,
    round: number
  ) {
    if (this._batchs.size === 0) {
      queueMicrotask(() => {
        __web_browser_api__.setFramesBatch(
          [...this._batchs.values()].join("\n")
        );
        this._batchs.clear();
      });
    }
    this._batchs.set(id, [id, x, y, width, height, round].join(","));
  }

  get visible() {
    return this._visible;
  }
  set visible(value) {
    __web_browser_api__.setVisible(this.id, (this._visible = value));
  }

  get zIndex() {
    return this._zIndex;
  }
  set zIndex(value) {
    __web_browser_api__.setZIndex(this.id, (this._zIndex = value));
  }

  get url() {
    return this._url;
  }
  set url(value) {
    __web_browser_api__.setUrl(this.id, (this._url = value));
  }

  get title() {
    return this._title;
  }

  get icon() {
    return this._icon;
  }

  close() {
    __web_browser_api__.closeWindow(this.id);
  }
}
WebviewWindow.startListens();
export class WindowFrame {
  constructor(
    public x: number,
    public y: number,
    public width: number,
    public height: number,
    public round: number
  ) {}
  static from(data: string) {
    // @ts-ignore
    return new WindowFrame(...data.split(",").map((v) => +v));
  }
}
