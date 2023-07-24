import { exportApis, mainApis } from "./apis.ts";
import { TaskbarElement } from "./taskbar.html.ts";

const taskbar = new TaskbarElement();
document.body.appendChild(taskbar);
exportApis(taskbar);
Object.assign(globalThis, { taskbar });

new ResizeObserver((entries) => {
  for (const entry of entries) {
    const { width, height } = entry.contentRect;
    console.log("resize", entry.contentRect);
    mainApis.resize(Math.ceil(width), Math.ceil(height));
  }
}).observe(taskbar);
