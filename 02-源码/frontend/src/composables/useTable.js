import { reactive, ref } from 'vue'

/**
 * 列表页通用逻辑：服务端分页 + 筛选 + 加载态 + 空数据。
 * 需求要求所有列表统一「默认每页 10 条、可切 20/50、切换筛选回到第 1 页」，
 * 这段逻辑集中在这里，页面不再各写一套。
 */
export function useTable(fetcher, defaults = {}) {
  const rows = ref([])
  const total = ref(0)
  const loading = ref(false)
  const page = ref(1)
  const size = ref(10)
  const query = reactive({ ...defaults })

  async function load() {
    loading.value = true
    try {
      const data = await fetcher({ page: page.value, size: size.value, ...clean(query) })
      rows.value = data.records || []
      total.value = data.total || 0
    } finally {
      loading.value = false
    }
  }

  /** 切换筛选条件后页码回到第 1 页 */
  function search() {
    page.value = 1
    load()
  }

  function reset() {
    Object.keys(query).forEach((k) => (query[k] = ''))
    page.value = 1
    load()
  }

  function onPage(p) {
    page.value = p
    load()
  }

  function onSize(s) {
    size.value = s
    page.value = 1
    load()
  }

  return { rows, total, loading, page, size, query, load, search, reset, onPage, onSize }
}

/** 去掉空串与 null，避免后端收到空筛选参数 */
function clean(obj) {
  const out = {}
  Object.entries(obj).forEach(([k, v]) => {
    if (v !== '' && v !== null && v !== undefined) out[k] = v
  })
  return out
}
