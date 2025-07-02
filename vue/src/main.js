import ElementPlus from 'element-plus'
import "element-plus/dist/index.css"
import './assets/main.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPersist from 'pinia-plugin-persistedstate'
import App from './App.vue'
import router from '/src/router'

window.global = window;
const pinia = createPinia() // 创建 Pinia 实例
pinia.use(piniaPersist)
const app=createApp(App);
app.use(router);
app.use(ElementPlus);
app.use(pinia);
app.mount('#app');

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

export default pinia