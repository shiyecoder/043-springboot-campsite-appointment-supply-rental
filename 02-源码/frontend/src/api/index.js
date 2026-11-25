import request from '../utils/request'

/**
 * 全量接口封装：页面只调用这里的方法，不各自拼 URL。
 * 字段命名注意：业务列表（MyBatis/JdbcTemplate 直出）是 snake_case，
 * 系统管理（SysUser/SysMessage 实体）与登录态是 camelCase，页面按各自结构取。
 */
export const authApi = {
  login: (d) => request.post('/api/auth/login', d),
  register: (d) => request.post('/api/auth/register', d),
  me: () => request.get('/api/auth/me'),
  updateProfile: (d) => request.put('/api/auth/profile', d),
  changePassword: (d) => request.put('/api/auth/password', d)
}

export const siteApi = {
  page: (p) => request.get('/api/site/page', { params: p }),
  detail: (id) => request.get(`/api/site/${id}`),
  options: () => request.get('/api/site/options'),
  create: (d) => request.post('/api/site', d),
  update: (id, d) => request.put(`/api/site/${id}`, d),
  remove: (id) => request.delete(`/api/site/${id}`)
}

export const bookingApi = {
  page: (p) => request.get('/api/booking/page', { params: p }),
  detail: (id) => request.get(`/api/booking/${id}`),
  calendar: (p) => request.get('/api/booking/calendar', { params: p }),
  create: (d) => request.post('/api/booking', d),
  transition: (id, d) => request.post(`/api/booking/${id}/transition`, d)
}

export const goodsApi = {
  page: (p) => request.get('/api/goods/page', { params: p }),
  create: (d) => request.post('/api/goods', d),
  update: (id, d) => request.put(`/api/goods/${id}`, d),
  picklist: (p) => request.get('/api/goods/picklist', { params: p }),
  handover: (d) => request.post('/api/goods/handover', d)
}

export const stayApi = {
  board: (p) => request.get('/api/stay/board', { params: p }),
  checkin: (d) => request.post('/api/stay/checkin', d),
  checkoutPreview: (bookingId) => request.get(`/api/checkout/${bookingId}/preview`),
  settle: (d) => request.post('/api/checkout/settle', d)
}

export const patrolApi = {
  page: (p) => request.get('/api/patrol/page', { params: p }),
  finish: (d) => request.post('/api/patrol/finish', d)
}

export const hazardApi = {
  page: (p) => request.get('/api/hazard/page', { params: p }),
  close: (id, note) => request.post(`/api/hazard/${id}/close`, { note })
}

export const eventApi = {
  page: (p) => request.get('/api/event/page', { params: p }),
  create: (d) => request.post('/api/event', d)
}

export const reviewApi = {
  page: (p) => request.get('/api/review/page', { params: p }),
  reply: (id, reply) => request.post(`/api/review/${id}/reply`, { reply })
}

export const blacklistApi = {
  page: (p) => request.get('/api/blacklist/page', { params: p }),
  apply: (d) => request.post('/api/blacklist', d),
  audit: (id, result, note) => request.post(`/api/blacklist/${id}/audit`, { result, note }),
  release: (id, note) => request.post(`/api/blacklist/${id}/release`, { note })
}

/** 创新点：营位与租赁物资联合可用性 */
export const availApi = {
  page: (p) => request.get('/api/avail/page', { params: p }),
  evaluate: (d) => request.post('/api/avail/evaluate', d),
  manual: (id, level, reason) => request.post(`/api/avail/${id}/manual`, { level, reason }),
  rules: () => request.get('/api/rule/list'),
  activeRule: () => request.get('/api/rule/active'),
  saveRule: (d) => request.post('/api/rule/config', d)
}

export const dashboardApi = {
  get: (p) => request.get('/api/dashboard', { params: p })
}

export const systemApi = {
  users: (p) => request.get('/api/system/user/page', { params: p }),
  createUser: (d) => request.post('/api/system/user', d),
  updateUser: (id, d) => request.put(`/api/system/user/${id}`, d),
  resetPwd: (id, password) => request.post(`/api/system/user/${id}/reset-password`, null, { params: { password } }),
  roles: () => request.get('/api/system/role/list'),
  menuTree: () => request.get('/api/system/menu/tree'),
  roleMenus: (code) => request.get(`/api/system/role/${code}/menus`),
  assignMenus: (code, ids) => request.post(`/api/system/role/${code}/menus`, ids),
  dicts: (type) => request.get('/api/system/dict/list', { params: { type } }),
  saveDict: (d) => request.post('/api/system/dict', d),
  delDict: (id) => request.delete(`/api/system/dict/${id}`),
  orgs: () => request.get('/api/system/org/list'),
  messages: () => request.get('/api/system/message/list'),
  unread: () => request.get('/api/system/message/unread'),
  readOne: (id) => request.post(`/api/system/message/${id}/read`),
  readAll: () => request.post('/api/system/message/read-all'),
  logs: (p) => request.get('/api/system/log/page', { params: p })
}

export const fileApi = {
  /** 传 File 对象，返回 { url, name, size } */
  upload: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return request.post('/api/file/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  }
}
