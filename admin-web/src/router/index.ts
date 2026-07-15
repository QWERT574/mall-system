import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '仪表盘', icon: 'Odometer' }
      },
      {
        path: 'product',
        name: 'Product',
        component: () => import('@/views/product/index.vue'),
        meta: { title: '商品管理', icon: 'Goods' }
      },
      {
        path: 'order',
        name: 'Order',
        component: () => import('@/views/order/index.vue'),
        meta: { title: '订单管理', icon: 'ShoppingCart' }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'activity',
        name: 'Activity',
        component: () => import('@/views/activity/index.vue'),
        meta: { title: '活动管理', icon: 'Calendar' }
      },
      {
        path: 'aftersale',
        name: 'AfterSale',
        component: () => import('@/views/aftersale/index.vue'),
        meta: { title: '售后管理', icon: 'Service' }
      },
      {
        path: 'system',
        name: 'System',
        component: () => import('@/views/system/index.vue'),
        meta: { title: '系统设置', icon: 'Setting' }
      },
      {
        path: 'seller',
        name: 'Seller',
        component: () => import('@/views/seller/index.vue'),
        meta: { title: '商家审核', icon: 'Shop' }
      },
      {
        path: 'customer-service',
        name: 'CustomerService',
        component: () => import('@/views/CustomerService/Chat.vue'),
        meta: { title: '客服聊天', icon: 'ChatDotSquare' }
      },
      {
        path: 'intervention',
        name: 'Intervention',
        component: () => import('@/views/Intervention/index.vue'),
        meta: { title: '人工介入', icon: 'Warning' }
      },
      {
        path: 'agent-management',
        name: 'AgentManagement',
        component: () => import('@/views/CustomerService/AgentManagement.vue'),
        meta: { title: '客服管理', icon: 'Service' }
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('@/views/knowledge/index.vue'),
        meta: { title: '知识库管理', icon: 'Collection' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('admin_token')
  
  if (to.path !== '/login' && !token) {
    ElMessage.warning('请先登录')
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
