import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

// 样式引入顺序:Element Plus 主题覆盖 → Element Plus 样式 → 全局样式 → 响应式样式
import './styles/element-overrides.scss'
import 'element-plus/dist/index.css'
import './styles/global.scss'
import './styles/responsive.scss'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
