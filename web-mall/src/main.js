import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import App from './App.vue';
import router from './router';
import './styles/theme.css';
import '@mall/shared-ui/index.css';
import SharedUI from '@mall/shared-ui/vue-components';

const app = createApp(App);
const pinia = createPinia();

// 注册 Element Plus
app.use(ElementPlus);
app.use(SharedUI);
app.use(pinia);
app.use(router);

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

app.mount('#app');
