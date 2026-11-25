import { createRouter, createWebHashHistory } from 'vue-router'

/**
 * 路由与后端菜单 sys_menu.path 一一对应，菜单按角色权限动态下发，
 * 前端只保证「有路径就有页面」；无权限的菜单后端不下发，用户看不到入口。
 * 菜单管理与分配菜单不在菜单表里下发，因此不注册可点入口。
 */
const routes = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { public: true, title: '登录' } },
  {
    path: '/',
    component: () => import('../layout/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '经营看板' } },
      { path: 'portal', component: () => import('../views/Portal.vue'), meta: { title: '营地门户' } },
      { path: 'site', component: () => import('../views/SitePage.vue'), meta: { title: '营区与营位' } },
      { path: 'site/price', component: () => import('../views/PricePage.vue'), meta: { title: '类型与价格' } },
      { path: 'booking', component: () => import('../views/BookingPage.vue'), meta: { title: '预约列表' } },
      { path: 'booking/calendar', component: () => import('../views/CalendarPage.vue'), meta: { title: '预约日历' } },
      { path: 'booking/new', component: () => import('../views/BookingNew.vue'), meta: { title: '新建预约' } },
      { path: 'stay', component: () => import('../views/StayPage.vue'), meta: { title: '入住与退营' } },
      { path: 'goods', component: () => import('../views/GoodsPage.vue'), meta: { title: '物资档案' } },
      { path: 'goods/pick', component: () => import('../views/PickPage.vue'), meta: { title: '备货与发放' } },
      { path: 'avail', component: () => import('../views/AvailPage.vue'), meta: { title: '联合可用性' } },
      { path: 'patrol', component: () => import('../views/PatrolPage.vue'), meta: { title: '巡检任务' } },
      { path: 'mobile/patrol', component: () => import('../views/MobilePatrol.vue'), meta: { title: '移动巡检' } },
      { path: 'hazard', component: () => import('../views/HazardPage.vue'), meta: { title: '隐患与事件' } },
      { path: 'checkout', component: () => import('../views/CheckoutPage.vue'), meta: { title: '退营结算' } },
      { path: 'review', component: () => import('../views/ReviewPage.vue'), meta: { title: '评价与黑名单' } },
      { path: 'rule', component: () => import('../views/RulePage.vue'), meta: { title: '规则配置' } },
      { path: 'system/user', component: () => import('../views/UserPage.vue'), meta: { title: '用户管理' } },
      { path: 'system/role', component: () => import('../views/RolePage.vue'), meta: { title: '角色菜单' } },
      { path: 'system/dict', component: () => import('../views/DictPage.vue'), meta: { title: '字典公告' } },
      { path: 'system/log', component: () => import('../views/LogPage.vue'), meta: { title: '操作日志' } },
      { path: 'message', component: () => import('../views/MessagePage.vue'), meta: { title: '消息中心' } },
      { path: 'profile', component: () => import('../views/ProfilePage.vue'), meta: { title: '个人中心' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({ history: createWebHashHistory(), routes })

router.beforeEach((to) => {
  const token = localStorage.getItem('at_token')
  if (!to.meta.public && !token) return '/login'
  if (to.path === '/login' && token) return '/dashboard'
  return true
})

export default router
