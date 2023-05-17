export default `
  (() => {
    console.log("1")
    const watchers = Array.from(globalThis.__native_close_watcher_kit__._watchers.values())
    console.log("2", watchers, watchers.length, history.state)
    if(watchers.length === 0){
      if(history.state === null || history.state.back === null){
        window.electron.ipcRenderer.sendToHost(
          'webveiw_message',
          "back"
        )
        console.log('window.electron:',window.electron)
      }else{
        window.history.back();
      }
    }else{
      watchers[watchers.length - 1].close()
    }
    console.log("3")
  })()
`