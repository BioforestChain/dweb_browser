/**
 * main.ts
 *
 * Bootstraps Vuetify and other plugins then mounts the App`
 */

// Components
import App from './App.vue'

// Composables
import { createApp } from 'vue'

// Plugins
import { registerPlugins } from '@/pages/new-tab/plugins'

const app = createApp(App)

registerPlugins(app)

app.mount('#app')

window.oncontextmenu = (event)=>{
  event.preventDefault();
}

  // 暗色背景设置
  const blackTheme = () => {
    const bodyDom = document.querySelector("body")
    console.log("blackTheme=>","暗色主题")
    if (bodyDom) {
      bodyDom.style.backgroundColor = "#000"
    }
  }
  // 暴露给 javascriptInterface 调用
  Object.assign(globalThis,{blackTheme})
