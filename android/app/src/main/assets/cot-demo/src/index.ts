function $<T extends HTMLElement>(params: string): T {
  return document.getElementById(params) as T
}

import { Duration } from '../../build/plugin/types/src/components/toast/toast.type.d.ts';
import "../../build/plugin/esm/src/index.js"
import { StatusbarPlugin, StatusbarStyle, EStatusBarAnimation, ToastPlugin, SplashScreenPlugin } from "../../src/index.ts";
import { SharePlugin } from "../../src/components/share/index.ts";
document.addEventListener("DOMContentLoaded", () => {
  /**toast */
  $("toast-show").addEventListener("click", async () => {
    const toast = document.querySelector<ToastPlugin>("dweb-toast")
    console.log("click toast-show")
    const duration = ($<HTMLSelectElement>("toast-duration").value ?? "long") as Duration
    const text = $<HTMLInputElement>("toast-message").value ?? "我是toast🍓"
    if (toast) {
      const result = await toast.show({ text, duration }).then(res => res.text())
      $("statusbar-observer-log").innerHTML = result
    }
  })
  /**statusbar */
  const statusBar = document.querySelector<StatusbarPlugin>("dweb-statusbar")!
  $("statusbar-setBackgroundColor").addEventListener("click", async () => {
    const color = $<HTMLInputElement>("statusbar-background-color").value
    console.log("statusbar=>", color)
    const result = await statusBar.setBackgroundColor({ color: color }).then(res => res.text())
    console.log("statusbar-setBackgroundColor=>", result)
    $("statusbar-observer-log").innerHTML = result
  })
  $("statusbar-getBackgroundColor").addEventListener("click", async () => {
    const result = await statusBar.getBackgroundColor().then(res => res.text())
    console.log("statusbar-getBackgroundColor=>", result)
    $("statusbar-observer-log").innerHTML = result
  })
  // style
  $("statusbar-setStyle").addEventListener("click", async () => {
    const styleOptions = ($<HTMLSelectElement>("statusbar-style").value) as StatusbarStyle
    await statusBar.setStyle({ style: styleOptions })
  })
  $("statusbar-getStyle").addEventListener("click", async () => {
    const result = await statusBar.getStyle()
    console.log("statusbar-getBackgroundColor=>", result)
    $("statusbar-observer-log").innerHTML = result
  })
  // visible
  $("statusbar-show").addEventListener("click", async () => {
    const animation = ($<HTMLSelectElement>("statusbar-animation").value) as EStatusBarAnimation
    await statusBar.show({ animation: animation })
  })
  $("statusbar-hide").addEventListener("click", async () => {
    const animation = ($<HTMLSelectElement>("statusbar-animation").value) as EStatusBarAnimation
    await statusBar.hide({ animation: animation })
  })
  // Overlays
  $("statusbar-setOverlaysWebView").addEventListener("click", async () => {
    const overlay = $<HTMLInputElement>("statusbar-overlay").checked
    await statusBar.setOverlaysWebView({ overlay: overlay })
  })
  $("statusbar-getOverlaysWebView").addEventListener("click", async () => {
    const result = await statusBar.getInfo()
    $("statusbar-observer-log").innerHTML = JSON.stringify(result)
  })

  /**Splash */
  const splash = document.querySelector<SplashScreenPlugin>("dweb-splash")!
  $("splashscreen-show").addEventListener("click", async () => {
    const result = await splash.show({ autoHide: true, fadeInDuration: 300, fadeOuDuration: 200, showDuration: 3000 }).then(res => res.text())
    $("statusbar-observer-log").innerHTML = result
  })
  $("splashscreen-hide").addEventListener("click", async () => {
    const result = await splash.hide({ fadeOuDuration: 200 }).then(res => res.text())
    $("statusbar-observer-log").innerHTML = result
  })

  /**Share */
  const share = document.querySelector<SharePlugin>("dweb-share")!
  $("share-share").addEventListener("click", async () => {
    const text = $<HTMLTextAreaElement>("share-options").value
    const result = await share.share(
      { title: "分享标题", text, url: "file:///xxx", files: ["file://xxx"], dialogTitle: "dialogTitle" })
      .then(res => res.text())
    $("statusbar-observer-log").innerHTML = result
  })

  /**Safe Area */
  /**Navigation Bar */
  /**Keyboard */
  /**Haptics */

});

