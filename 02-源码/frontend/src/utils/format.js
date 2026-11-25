/**
 * 全局时间、数值与枚举文案，全系统统一调用，禁止各页面各写一套。
 * 枚举英文码 → 中文标签集中在这里，页面上永远只显示中文，颜色用 tag 类型辅助。
 */
export function fmtDateTime(v) {
  if (!v) return '-'
  const d = typeof v === 'string' ? new Date(v.replace(/-/g, '/').replace('T', ' ')) : new Date(v)
  if (isNaN(d.getTime())) return String(v).replace('T', ' ').slice(0, 19)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

export function fmtDate(v) {
  if (!v) return '-'
  return fmtDateTime(v).slice(0, 10)
}

/** 只到分钟，表格里省空间 */
export function fmtMinute(v) {
  if (!v) return '-'
  return fmtDateTime(v).slice(0, 16)
}

export function fromNow(v) {
  if (!v) return '-'
  const d = new Date(String(v).replace(/-/g, '/').replace('T', ' '))
  const diff = Date.now() - d.getTime()
  if (isNaN(diff)) return '-'
  const m = Math.floor(diff / 60000)
  if (m < 1) return '刚刚'
  if (m < 60) return m + ' 分钟前'
  const h = Math.floor(m / 60)
  if (h < 24) return h + ' 小时前'
  const day = Math.floor(h / 24)
  if (day < 30) return day + ' 天前'
  return fmtDate(v)
}

export function fmtNum(v, digits = 2) {
  if (v === null || v === undefined || v === '') return '-'
  const n = Number(v)
  if (isNaN(n)) return '-'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: digits })
}

/** 金额统一两位小数 + 千分位 */
export function fmtMoney(v) {
  if (v === null || v === undefined || v === '') return '-'
  const n = Number(v)
  if (isNaN(n)) return '-'
  return '¥' + n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function fmtPct(v, digits = 1) {
  if (v === null || v === undefined || v === '') return '-'
  const n = Number(v)
  return isNaN(n) ? '-' : n.toFixed(digits) + '%'
}

/** 枚举字典：码值 → 中文 */
export const DICT = {
  bookingStatus: {
    DRAFT: '草稿', SUBMITTED: '待确认', CONFIRMED: '已确认', RESERVED: '已预留',
    OCCUPIED: '已入住', CHECKED: '退营待结算', COMPLETED: '已完成',
    CANCELLED: '已取消', REJECTED: '已拒绝', NOSHOW: '爽约'
  },
  siteStatus: { FREE: '可预订', RESERVED: '已预留', OCCUPIED: '已入住', CLEANING: '待清洁', REPAIR: '维修中', OFF: '停用' },
  itemStatus: { PENDING: '待发放', PICKED: '已发放', RETURNED: '已归还', DAMAGED: '损坏', LOST: '遗失' },
  availLevel: { HIGH: '推荐', MID: '可接受', LOW: '不推荐' },
  patrolStatus: { PENDING: '待巡检', DONE: '已完成', OVERDUE: '已逾期' },
  patrolResult: { NORMAL: '无异常', PROBLEM: '发现问题' },
  hazardStatus: { OPEN: '待整改', RECTIFY: '整改中', CLOSED: '已关闭' },
  hazardLevel: { LOW: '轻微', MID: '一般', HIGH: '严重' },
  eventStatus: { OPEN: '待处理', HANDLING: '处理中', CLOSED: '已关闭' },
  eventType: {
    NOISE: '噪音扰邻', SAFETY: '安全隐患', FACILITY: '设施损坏', FIRE: '明火违规',
    ENV: '环境卫生', OTHER: '其他'
  },
  blackStatus: { PENDING: '待审核', ACTIVE: '已生效', REJECTED: '已驳回', RELEASED: '已解除' },
  reasonType: { DAMAGE: '损毁物资', NOISE: '噪音扰邻', NOSHOW: '多次爽约', FIRE: '违规用火', OTHER: '其他' },
  source: { WEB: '线上预订', PHONE: '电话预订', WALKIN: '到店散客' },
  goodsStatus: { ON: '在库可租', MAINTAIN: '维护中', OFF: '停用' },
  patrolType: { POWER: '用电安全', FIRE: '消防设施', ENV: '环境卫生', FACILITY: '设施完好', NIGHT: '夜间巡查' },
  payType: { CASH: '现金', WECHAT: '微信', ALIPAY: '支付宝', CARD: '刷卡', TRANSFER: '对公转账' }
}

/** 取中文标签，找不到就原样返回 */
export function label(map, code) {
  const m = DICT[map] || {}
  return m[code] || code || '-'
}

/** 状态 → Element Plus tag 类型（颜色只做辅助，文字同步显示） */
export function tagType(map, code) {
  const key = code
  if (['COMPLETED', 'CONFIRMED', 'FREE', 'RETURNED', 'NORMAL', 'CLOSED', 'HIGH', 'ON', 'ACTIVE'].includes(key)) return 'success'
  if (['SUBMITTED', 'RESERVED', 'PENDING', 'CLEANING', 'RECTIFY', 'HANDLING', 'MAINTAIN', 'MID'].includes(key)) return 'warning'
  if (['OCCUPIED', 'CHECKED', 'PICKED', 'DONE', 'OPEN'].includes(key)) return 'primary'
  if (['CANCELLED', 'REJECTED', 'NOSHOW', 'REPAIR', 'OFF', 'DAMAGED', 'LOST', 'LOW', 'OVERDUE', 'DRAFT'].includes(key)) return 'danger'
  return 'info'
}

/** 保留兼容：旧代码用 statusType(中文) */
export function statusType(cn) {
  if (['已完成', '已确认', '可预订', '已归还', '无异常', '已关闭', '推荐', '在库可租', '已生效'].includes(cn)) return 'success'
  if (['待确认', '已预留', '待发放', '待清洁', '整改中', '处理中', '维护中', '可接受', '待审核', '待整改'].includes(cn)) return 'warning'
  if (['已入住', '退营待结算', '已发放', '已完成检查', '待处理'].includes(cn)) return 'primary'
  if (['已取消', '已拒绝', '爽约', '维修中', '停用', '损坏', '遗失', '不推荐', '已逾期', '草稿'].includes(cn)) return 'danger'
  return 'info'
}
