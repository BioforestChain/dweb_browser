import { fetch_helpers } from "../../core/helper.cjs";

console.log("ookkkkk, i'm in worker");
const easyFetch = (url: string, init?: RequestInit) => {
  return Object.assign(fetch(url, init), fetch_helpers);
};
debugger;
(async () => {
  /// 申请端口监听，不同的端口会给出不同的域名和控制句柄，控制句柄不要泄露给任何人
  const domain = easyFetch(`file://http.sys.dweb/listen?port=80`).string();

  const view_id = await easyFetch(
    `file://mwebview.sys.dweb/open?url=desktop.html`
  ).then((res) => res.text());

  //    addEventListener("fetch", (event) => {
  //       if (event.request.headers["view-id"] === view_id) {
  //       }
  //    });
})();
