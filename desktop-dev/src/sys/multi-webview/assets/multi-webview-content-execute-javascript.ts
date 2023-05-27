const code = () => {
  const virtualkeyboard = {
    inputBindVirtualKeyboard(el: Element) {
      el.removeEventListener("focusin", this.bindVirtualKeyboardFocusin);
      el.removeEventListener("focusout", this.bindVirtualKeyboardFocusout);
      el.addEventListener("focusin", this.bindVirtualKeyboardFocusin);
      el.addEventListener("focusout", this.bindVirtualKeyboardFocusout);
    },
    bindVirtualKeyboardFocusin() {
      globalThis.electron.ipcRenderer.sendToHost("virtual_keyboard_open");
    },
    bindVirtualKeyboardFocusout() {
      globalThis.electron.ipcRenderer.sendToHost("virtual_keyboard_close");
    },
    inputIsNeedBindVirtualKeyboard(node: HTMLInputElement) {
      return (
        node.tagName === "INPUT" &&
        (node.type === "email" ||
          node.type === "number" ||
          node.type === "password" ||
          node.type === "search" ||
          node.type === "tel" ||
          node.type === "text" ||
          node.type === "url")
      );
    },
    callback(mutationList: MutationRecord[], _observer: MutationObserver) {
      mutationList.forEach((mutationRecord) => {
        switch (mutationRecord.type) {
          case "childList":
            mutationRecord.addedNodes.forEach((node: Node) => {
              // 添加了节点可能只有直接的子节点在这里，嵌套的子节点不再这里哦
              if (node.nodeType !== Node.ELEMENT_NODE) return;
              const allEl = this.getSub(node as Element);
              allEl.forEach((el: Element) => {
                if (el.shadowRoot) {
                  // 添加 监听
                  this.createMutationObserver(
                    el.shadowRoot as unknown as Element, 
                    this.callback.bind(this)
                  );
                  return;
                }
                // 绑定 virtual-keyboard
                this.inputIsNeedBindVirtualKeyboard(el as HTMLInputElement)
                  ? this.inputBindVirtualKeyboard(el)
                  : "";
              });
            });
  
            mutationRecord.removedNodes.forEach((node) => {
              // 移除监听
              if ((node as unknown as {shadowRoot: ShadowRoot}).shadowRoot) {
                const el = ((node as unknown as {shadowRoot: ShadowRoot})
                            .shadowRoot) as unknown as $RemoveObserver
                el.removeObserver();
              }
            });
            break;
        }
      });
    },
    getSub(root: Element) {
      const sub = Array.from(root.children).reduce((pre: Element[], el: Element) => {
        this.getSub(el);
        return [...pre, ...this.getSub(el)];
      }, []) as Element[];
      if (root.shadowRoot) {
        return [...Array.from(root.shadowRoot.children), ...sub];
      }
      return [...Array.from(root.children), ...sub];
    },
    createMutationObserver(el: Element | ShadowRoot, callback: MutationCallback) {
      const removeObserver = Reflect.get(el, "removeObserver")
      if(removeObserver) return; /** 之前已经注册过监听了 */

      const observerOptions = {
        childList: true, // 观察目标子节点的变化，是否有添加或者删除
        subtree: true, // 观察后代节点，默认为 false
      };
      let observer: MutationObserver | null = new MutationObserver(callback);
      observer.observe(el, observerOptions);
      console.error("observer 在什么时候会被回收");
      // observer 会在 元素被移除的时候回收
      Reflect.set(el, "removeObserver", () => {
        console.log("回收了");
        observer = null;
      })
    },
    init(){
      this.createMutationObserver(document.body, this.callback.bind(this));
      const allEl = this.getSub(document.body);
      allEl.forEach((el: Element) => {
        if (el.shadowRoot) {
          this.createMutationObserver(el.shadowRoot, this.callback.bind(this));
          console.log("添加了 observe");
          return;
        }
        this.inputIsNeedBindVirtualKeyboard(el as HTMLInputElement) ? this.inputBindVirtualKeyboard(el) : "";
      });
    }
  }
  virtualkeyboard.init()
  

};
export default code;
export interface $RemoveObserver{
  removeObserver: { (): void}
}
export interface $Electron{
  ipcRenderer: {
    sendToHost(message: string, data?: unknown): void
  }
}
export interface $Virtualkeyboard{
  inputBindVirtualKeyboard(el: Element): void
  bindVirtualKeyboardFocusin(): void
  bindVirtualKeyboardFocusout(): void
  inputIsNeedBindVirtualKeyboard(input: Element ): boolean
  callback(mutationList: MutationRecord[], _observer: MutationObserver): void;
  createMutationObserver(el: Element | ShadowRoot, callback: MutationCallback): void,
  getSub(node: Node): Element[]
  init():void;
}

declare namespace globalThis{
  let electron: $Electron;
  let virtualkeyboard: $Virtualkeyboard;
}
