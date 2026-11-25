<template>
  <div class="page">
    <div class="page-head">
      <h2>预约日历<span class="sub">按营位查看占用区间</span></h2>
      <div class="ops">
        <el-date-picker v-model="month" type="month" value-format="YYYY-MM" placeholder="选择月份" @change="load" />
        <el-button :icon="List" @click="$router.push('/booking')">列表视图</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-select v-model="areaId" placeholder="全部营区" clearable @change="load">
        <el-option v-for="a in areas" :key="a.id" :label="a.area_name" :value="a.id" />
      </el-select>
      <span class="grow"></span>
      <span class="muted">
        <i class="dot free"></i>可订 <i class="dot busy"></i>已占用 <i class="dot mine"></i>本页选中预约
      </span>
    </div>

    <div class="table-card cal-card">
      <div class="cal-head">
        <div class="cell site-col">营位</div>
        <div class="days">
          <div v-for="d in days" :key="d.key" class="day" :class="{ weekend: d.weekend }">
            <span>{{ d.d }}</span>
            <i>{{ d.w }}</i>
          </div>
        </div>
      </div>

      <el-scrollbar height="calc(100vh - 320px)">
        <div v-for="s in filteredSites" :key="s.id" class="cal-row">
          <div class="cell site-col">
            <div class="sn mono">{{ s.site_no }}</div>
            <div class="sname">{{ s.site_name }}</div>
          </div>
          <div class="days">
            <div
              v-for="d in days"
              :key="d.key"
              class="day-cell"
              :class="cellClass(s, d.key)"
              :title="cellTitle(s, d.key)"
            ></div>
          </div>
        </div>
      </el-scrollbar>
    </div>

    <div class="card" style="margin-top: 12px">
      <div class="strong" style="margin-bottom: 8px">本月预约明细（{{ items.length }} 条）</div>
      <el-table :data="items" size="small" max-height="220">
        <el-table-column prop="booking_no" label="预约号" min-width="150">
          <template #default="{ row }"><span class="mono">{{ row.booking_no }}</span></template>
        </el-table-column>
        <el-table-column prop="customer_name" label="客户" width="100" />
        <el-table-column prop="site_name" label="营位" min-width="130" />
        <el-table-column label="入住" width="200">
          <template #default="{ row }">{{ row.check_in_date }} → {{ row.check_out_date }}</template>
        </el-table-column>
        <el-table-column prop="guest_count" label="人数" width="70" align="center" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="tagType('booking', row.status)">{{ label('bookingStatus', row.status) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { List } from '@element-plus/icons-vue'
import { bookingApi, siteApi } from '../api'
import { label, tagType } from '../utils/format'

const month = ref(new Date().toISOString().slice(0, 7))
const areaId = ref('')
const sites = ref([])
const areas = ref([])
const items = ref([])

const days = computed(() => {
  const [y, m] = month.value.split('-').map(Number)
  const last = new Date(y, m, 0).getDate()
  const w = ['日', '一', '二', '三', '四', '五', '六']
  return Array.from({ length: last }, (_, i) => {
    const d = new Date(y, m - 1, i + 1)
    return {
      key: `${month.value}-${String(i + 1).padStart(2, '0')}`,
      d: i + 1,
      w: w[d.getDay()],
      weekend: d.getDay() === 0 || d.getDay() === 6
    }
  })
})

const filteredSites = computed(() =>
  areaId.value ? sites.value.filter((s) => s.area_id === areaId.value) : sites.value
)

/** 某营位某日是否被占用：入住日含、离营日不含 */
function bookingAt(site, dayKey) {
  return items.value.find(
    (b) => b.site_id === site.id && dayKey >= b.check_in_date && dayKey < b.check_out_date
  )
}

function cellClass(site, key) {
  const b = bookingAt(site, key)
  if (!b) return 'free'
  if (['CANCELLED', 'REJECTED', 'NOSHOW'].includes(b.status)) return 'free'
  return 'busy'
}

function cellTitle(site, key) {
  const b = bookingAt(site, key)
  if (!b) return `${site.site_no} ${key} 可预订`
  return `${site.site_no} ${key} ${b.customer_name}（${label('bookingStatus', b.status)}）${b.check_in_date}→${b.check_out_date}`
}

async function load() {
  const [y, m] = month.value.split('-').map(Number)
  const from = `${month.value}-01`
  const to = `${month.value}-${String(new Date(y, m, 0).getDate()).padStart(2, '0')}`
  const all = await bookingApi.calendar({ from, to })
  items.value = all || []
}

onMounted(async () => {
  const opt = await siteApi.options()
  areas.value = opt.areas || []
  const page = await siteApi.page({ page: 1, size: 200 })
  sites.value = page.records || []
  load()
})
</script>

<style scoped>
.cal-card { padding: 0; }
.cal-head { display: flex; border-bottom: 1px solid var(--line); background: #f7f9fb; }
.cal-row { display: flex; border-bottom: 1px solid #f2f5f8; }
.cal-row:hover { background: #fbfdfe; }
.cell.site-col { width: 168px; flex: 0 0 168px; padding: 6px 10px; border-right: 1px solid var(--line); }
.cal-head .site-col { font-size: 12px; color: var(--text-2); font-weight: 600; display: flex; align-items: center; }
.sn { font-size: 12px; color: var(--text-2); }
.sname { font-size: 12px; color: var(--text); }
.days { flex: 1; display: grid; grid-template-columns: repeat(31, 1fr); }
.day {
  text-align: center; font-size: 11px; color: var(--text-2); padding: 3px 0; border-right: 1px solid #f2f5f8;
  display: flex; flex-direction: column; align-items: center; line-height: 1.15;
}
.day.weekend { background: #f4f8fb; }
.day i { font-style: normal; font-size: 9px; color: var(--text-3); }
.day-cell { height: 34px; border-right: 1px solid #f2f5f8; }
.day-cell.free { background: #fff; }
.day-cell.busy { background: #cfe0ea; }
.dot { display: inline-block; width: 9px; height: 9px; border-radius: 2px; margin: 0 4px 0 10px; }
.dot.free { background: #fff; border: 1px solid #cfd8e0; }
.dot.busy { background: #cfe0ea; }
.dot.mine { background: #2f6b8a; }
</style>
