import { dwebServiceWorker as sw } from "@bfex/plugin"
import { onMounted } from "vue"


(async () => {

  onMounted(async () => {
    sw.addEventListener("updatefound", (event) => {
      console.log("Dweb Service Worker update found!", event);
    })

    sw.addEventListener("fetch", (event) => {
      console.log("Dweb Service Worker fetch!", event);
    })

    sw.addEventListener("onFetch", (event) => {
      console.log("Dweb Service Worker onFetch!", event);
    })

    for await (let progess of sw.updateContoller.progress()) {
      console.log("Dweb Service Worker demo#process =>", progess)
    }

    const updateContoller = sw.update

    updateContoller.addEventListener("start", (event) => {
      console.log("Dweb Service Worker updateContoller start =>", event);
    })
    updateContoller.addEventListener("end", (event) => {
      console.log("Dweb Service Worker updateContoller end =>", event);
    })
    updateContoller.addEventListener("progress", (event) => {
      console.log("Dweb Service Worker updateContoller progress =>", event);
    })
    updateContoller.addEventListener("cancel", (event) => {
      console.log("Dweb Service Worker updateContoller cancel =>", event);
    })

  })


})()
