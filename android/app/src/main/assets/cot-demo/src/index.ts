function $<T extends HTMLElement>(params: string): T {
  return document.getElementById(params) as T
}

import { ToastPlugin } from "../../build/plugin/esm/src/index.js"

$("toast-show").addEventListener("click", () => {
  console.log("click toast-show")
  const duration = $<HTMLSelectElement>("toast-duration").value ?? "long"
  const msg = $<HTMLInputElement>("toast-message").value ?? "我是toast🍓"
  const toast = new ToastPlugin()
  toast.show(msg, duration)
})
