export default `
(() => {
  if (!globalThis.__native_close_watcher_kit__) {
    globalThis.__native_close_watcher_kit__ =  {
      allc: 0,
      _watchers: new Map(),
      _tasks: new Map(),
      registryToken: function(consumeToken){
        if (consumeToken === null || consumeToken === "") {
          throw new Error("CloseWatcher.registryToken invalid arguments");
        }
        const resolve = this._tasks.get(consumeToken)
        if(resolve === undefined) throw new Error('resolve === undefined');
        const id = this.allc++;
        resolve(id + "");
      },
      tryClose: function(id){
        const watcher = this._watchers.get(id);
        if(watcher === undefined) throw new Error('watcher === undefined');
        watcher.dispatchEvent(new Event("close"))
      }
    };

    // 这里会修改了 window.open 的方法 是否有问题了？？
    globalThis.open = function(arg){
      console.error('open 方法被修改了 需要调用 主渲染进程的 openWebview 方法，但是还没有处理', arg)
    }
  }
  
  // 拦截 fetch
  globalThis.nativeFetch = globalThis.fetch;
  globalThis.fetch = (request) => {
    let url = typeof request === 'string' ? request : request.url;
    if(url.endsWith('bfs-metadata.json')){
      // 把请求发送出去
      console.log('需要拦截的请求', request)
      console.log('window.navigator.userAgent', window.navigator.userAgent);
      // 把请求发送给 jsMM 模块
      url = 'http://api.browser.sys.dweb-443.localhost:22605/open_download?url=' + url
      // 只能够想办法 发送给 browser 让 browser 处理
      return globalThis.nativeFetch(url)
    }else{
      return globalThis.nativeFetch(request)
    }
  }

  // core 
  function inputBindVirtualKeyboard(el){
    el.removeEventListener('focusin',bindVirtualKeyboardFocusin)
    el.removeEventListener('focusout',bindVirtualKeyboardFocusout)
    el.addEventListener('focusin', bindVirtualKeyboardFocusin)
    el.addEventListener('focusout',bindVirtualKeyboardFocusout)
    console.log('bind virtual keyboard')
  }

  function bindVirtualKeyboardFocusin(){
    window.electron.ipcRenderer.sendToHost('virtual_keyboard_open')
  }

  function bindVirtualKeyboardFocusout(){
    window.electron.ipcRenderer.sendToHost('virtual_keyboard_close')
  }

  function inputIsNeedBindVirtualKeyboard(node){
    return node.tagName === "INPUT"
    && (
      node.type === "email"
      || node.type === "number"
      || node.type === "password"
      || node.type === "search"
      || node.type === "tel"
      || node.type === "text"
      || node.type === "url"
    ) 
  }

  function callback(mutationList, observe){
    mutationList.forEach(mutationRecord => {
      switch(mutationRecord.type){
        case "childList":
          mutationRecord.addedNodes.forEach(node => {
            // 添加了节点可能只有直接的子节点在这里，嵌套的子节点不再这里哦
            if(node.nodeType !== Node.ELEMENT_NODE) return;
            const allEl = getSub(node)
            allEl.forEach(el => {
              if(el.shadowRoot){
                // 添加 监听
                createMutationObserver(el.shadowRoot, callback)
                return;
              }
              // 绑定 virtual-keyboard
              inputIsNeedBindVirtualKeyboard(el)? inputBindVirtualKeyboard(el) : "";
            })
          })

          mutationRecord.removedNodes.forEach(node => {
            // 移除监听
            if(node.shadowRoot){
              node.shadowRoot.removeObserver();
            }
          })
        break;
      }
    })
  }

  function getSub(root){
    const sub = Array.from(root.children).reduce((pre, el) => {
      getSub(el)
      return [...pre, ...getSub(el)]
    },[])
    if(root.shadowRoot){
      return [...root.shadowRoot.children, ...sub]
    } 
    return [...root.children, ...sub]
  }

  function createMutationObserver(el, callback){
    const observerOptions = {
      childList: true,  // 观察目标子节点的变化，是否有添加或者删除
      subtree: true     // 观察后代节点，默认为 false
    }
    let observer = new MutationObserver(callback);
    observer.observe(el, observerOptions);
    console.error('observer 在什么时候会被回收')
    el.removeObserver = () => {
      console.log('回收了')
      observer = null
    }
  }

  createMutationObserver(document.body, callback)
  const allEl = getSub(document.body)
  allEl.forEach(el => {
    if(el.shadowRoot){
      createMutationObserver(el.shadowRoot, callback)
      console.log('添加了 observe')
      return;
    }
    inputIsNeedBindVirtualKeyboard(el)? inputBindVirtualKeyboard(el) : "";
  })
})()
`