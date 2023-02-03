/// <reference path="../../sys/js-process.worker.d.ts"/>

console.log("ookkkkk, i'm in worker");

debugger;
(async () => {
  /// 申请端口监听，不同的端口会给出不同的域名和控制句柄，控制句柄不要泄露给任何人
  const { origin } = await process
    .fetch(`file://localhost.sys.dweb/listen?port=80`)
    .object<{
      origin: string;
    }>();

  console.log("origin", origin);
  {
    const view_id = await process
      .fetch(`file://mwebview.sys.dweb/open?url=${encodeURIComponent(origin)}`)
      .string();
  }
  {
    const view_id = await process
      .fetch(`file://mwebview.sys.dweb/open?url=${encodeURIComponent(origin)}`)
      .string();
  }

  //    addEventListener("fetch", (event) => {
  //       if (event.request.headers["view-id"] === view_id) {
  //       }
  //    });
})();
