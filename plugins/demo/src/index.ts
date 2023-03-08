function $<T extends HTMLElement>(params: string): T {
  return document.getElementById(params) as T
}

import { ToastPlugin, Duration } from "../../build/plugin/esm/src/index.js"

$("toast-show").addEventListener("click", () => {
  console.log("click toast-show")
  const duration = $("toast-duration").nodeValue as Duration ?? "long"
  const msg = $("toast-message").nodeValue ?? ""
  const toast = new ToastPlugin()
  toast.show(msg, duration)
})
