function $<T extends HTMLElement>(params: string): T {
  return document.getElementById(params) as T
}

import { Tduration } from '../../build/plugin/types/src/components/toast/toast.type.d.ts';
import { ToastPlugin } from "../../src/components/toast/toast.plugin.ts";
import "../../build/plugin/esm/src/index.js"

$("toast-show").addEventListener("click", async () => {
  console.log("click toast-show")
  const duration = ($<HTMLSelectElement>("toast-duration").value ?? "long") as Tduration
  const text = $<HTMLInputElement>("toast-message").value ?? "我是toast🍓"
  const toast = document.querySelector<ToastPlugin>("dweb-toast")
  if (toast) {
    const result = await toast.show({ text, duration })
    console.log("show result=>", await result)
  }
})
