function $<T extends HTMLElement>(params: string): T {
  return document.getElementById(params) as T
}

import { ToastPlugin, Duration } from "../../src/index.ts"

$("toast-show").addEventListener("click", () => {
  console.log("click toast-show")
  const duration = $("toast-duration").nodeValue as Duration ?? "long"
  const msg = $("toast-message").nodeValue ?? ""
  const toast = new ToastPlugin()
  toast.show(msg, duration)
})
