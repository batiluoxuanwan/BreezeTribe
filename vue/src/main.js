import ElementPlus from 'element-plus'
import "element-plus/dist/index.css"

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPersist from 'pinia-plugin-persistedstate'
import App from './App.vue'
import router from '/src/router'


const pinia = createPinia() // 创建 Pinia 实例
pinia.use(piniaPersist)
const app=createApp(App);
app.use(router);
app.use(ElementPlus);
app.use(pinia);
app.mount('#app');

export default pinia