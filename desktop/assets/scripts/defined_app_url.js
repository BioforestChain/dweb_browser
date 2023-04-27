function definedAppUrl(){
  let appUrl = undefined;
  Object.defineProperty(window, 'appUrl',{
      get(){
          if(window.parentElement === undefined) throw new Error(`window.parentElement === undefined`)
          if(appUrl !== undefined) return appUrl;
          const url = window.parentElement.dataset.appUrl
          appUrl = new URL(window.parentElement.dataset.appUrl).origin;
          return appUrl
      }
  })
}

definedAppUrl()