import { dwebServiceWorker as sw } from "@bfex/plugin"

sw.addEventListener("updatefound", (event) => {
  console.log("Dweb Service Worker update found!", event);
})

