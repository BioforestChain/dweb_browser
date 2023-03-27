export const nativeOpen = async (url: string) => {
  return await jsProcess
    .nativeFetch(
      `file://mwebview.sys.dweb/open?url=${encodeURIComponent(url)}`
    )
    .text();
}


export const nativeActivate = async (webview_id: string) => {
  return await jsProcess
    .nativeFetch(
      `file://mwebview.sys.dweb/activate?webview_id=${encodeURIComponent(webview_id)}`
    )
    .text();
}
