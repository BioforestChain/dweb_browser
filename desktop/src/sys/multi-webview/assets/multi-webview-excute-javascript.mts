export default `
  (() => {
    const watchers = Array.from(globalThis.__native_close_watcher_kit__._watchers.values())
    if(watchers.length === 0){
      if(history.state.back === null){
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
  })()
`