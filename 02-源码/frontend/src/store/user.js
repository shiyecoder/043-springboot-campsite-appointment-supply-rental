import { defineStore } from 'pinia'
import request from '../utils/request'

/**
 * 登录态、用户信息与角色菜单。
 * 登录响应结构 AuthDtos.LoginResp = { token, user, menus }：
 * user 与 menus 分两个 key 存，避免把整包响应揉成一个对象后取错层级。
 * 菜单由后端按 roleCode 下发，前端不硬编码可访问菜单，权限判定以后端为准。
 */
const ROLE_LABEL = {
  admin: '系统管理员',
  manager: '运营经理',
  front: '前台接待',
  warehouse: '仓库物资员',
  tourist: '游客/预订人'
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('at_token') || '',
    user: safeParse(localStorage.getItem('at_user')) || {},
    menus: safeParse(localStorage.getItem('at_menus')) || []
  }),
  getters: {
    isLogin: (s) => !!s.token,
    roleCode: (s) => s.user.roleCode || '',
    roleName: (s) => s.user.roleName || ROLE_LABEL[s.user.roleCode] || '用户',
    realName: (s) => s.user.realName || '',
    avatar: (s) => s.user.avatar || '',
    isAdmin: (s) => s.user.roleCode === 'admin'
  },
  actions: {
    async login(payload) {
      const data = await request.post('/api/auth/login', payload)
      this.token = data.token || ''
      this.user = data.user || {}
      this.menus = data.menus || []
      localStorage.setItem('at_token', this.token)
      localStorage.setItem('at_user', JSON.stringify(this.user))
      localStorage.setItem('at_menus', JSON.stringify(this.menus))
      return data
    },
    /** /api/auth/me 直接返回用户对象（含 menus），不是 { user, menus } */
    async fetchMe() {
      const data = await request.get('/api/auth/me')
      const menus = data.menus || []
      delete data.menus
      this.user = data
      this.menus = menus
      localStorage.setItem('at_user', JSON.stringify(this.user))
      localStorage.setItem('at_menus', JSON.stringify(this.menus))
      return data
    },
    /** 头像与资料更新后同步右上角 */
    patchUser(patch) {
      this.user = { ...(this.user || {}), ...patch }
      localStorage.setItem('at_user', JSON.stringify(this.user))
    },
    logout() {
      this.token = ''
      this.user = {}
      this.menus = []
      localStorage.removeItem('at_token')
      localStorage.removeItem('at_user')
      localStorage.removeItem('at_menus')
    }
  }
})

/** localStorage 里可能出现 "undefined" 字符串（历史坑），解析失败一律返回 null */
function safeParse(str) {
  if (!str || str === 'undefined' || str === 'null') return null
  try {
    return JSON.parse(str)
  } catch (e) {
    return null
  }
}
