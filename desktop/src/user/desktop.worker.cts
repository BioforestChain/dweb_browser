console.log("ookkkkk, i'm in worker");
debugger;
(async () => {
   const view_id = await fetch(
      `file://mwebview.sys.dweb/open?url=desktop.html`
   ).then((res) => res.text());

   //    addEventListener("fetch", (event) => {
   //       if (event.request.headers["view-id"] === view_id) {
   //       }
   //    });
})();
