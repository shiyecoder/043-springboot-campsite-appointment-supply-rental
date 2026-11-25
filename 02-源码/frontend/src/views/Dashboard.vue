<template>
  <div class="page dash">
    <div class="page-head">
      <h2>
        经营看板
        <span class="sub">{{ d.today }} · 近 {{ d.days || 30 }} 天</span>
      </h2>
      <div class="ops">
        <el-radio-group v-model="period" size="default" @change="load">
          <el-radio-button value="7">近 7 天</el-radio-button>
          <el-radio-button value="30">近 30 天</el-radio-button>
          <el-radio-button value="90">近 90 天</el-radio-button>
        </el-radio-group>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <!-- 6 个 KPI：入住率、在营、今日到离、待处理、当月收入、物资出租率 -->
    <div class="stat-row">
      <div class="stat">
        <div class="k">营位入住率</div>
        <div class="v">{{ fmtPct(d.occupancyRate) }}</div>
      </div>
      <div class="stat">
        <div class="k">在营 / 营位总数</div>
        <div class="v">{{ d.siteOccupied || 0 }}<span class="u">/ {{ d.siteTotal || 0 }}</span></div>
      </div>
      <div class="stat">
        <div class="k">今日到营 / 离营</div>
        <div class="v">{{ d.todayIn || 0 }}<span class="u">/ {{ d.todayOut || 0 }}</span></div>
      </div>
      <div class="stat">
        <div class="k">待备货 / 待整改</div>
        <div class="v">{{ d.pendingPick || 0 }}<span class="u">/ {{ d.pendingRectify || 0 }}</span></div>
      </div>
      <div class="stat">
        <div class="k">当月收入</div>
        <div class="v">{{ fmtNum(d.monthIncome, 0) }}<span class="u">元</span></div>
      </div>
      <div class="stat">
        <div class="k">物资出租率</div>
        <div class="v">{{ fmtPct(d.goodsRentRate) }}</div>
      </div>
    </div>

    <!-- 7 个图表 + 待办，整页控制在 1440x900 一屏。
         必须等数据到位再挂载图表：空数据时雷达的 indicator 为空数组，
         echarts 在 data 初始化阶段会抛 "reading 'push'"，且首屏会闪一次空图。 -->
    <div class="grid" v-if="loaded">
      <div class="chart-card span2">
        <h4>预约与入住趋势<span>近 {{ d.days || 30 }} 天</span></h4>
        <ChartBox :option="trendOpt" :height="262" />
      </div>
      <div class="chart-card">
        <h4>营位类型使用率<span>按类型</span></h4>
        <ChartBox :option="typeOpt" :height="262" />
      </div>
      <div class="chart-card">
        <h4>联合可用性分布<span>创新点评分</span></h4>
        <ChartBox :option="availOpt" :height="262" />
      </div>

      <div class="chart-card">
        <h4>物资租用 TOP6<span>租赁明细</span></h4>
        <ChartBox :option="goodsOpt" :height="262" />
      </div>
      <div class="chart-card">
        <h4>收入构成<span>营位费 / 物资 / 其他</span></h4>
        <ChartBox :option="incomeOpt" :height="262" />
      </div>
      <div class="chart-card">
        <h4>营区热度<span>预约量</span></h4>
        <ChartBox :option="areaOpt" :height="262" />
      </div>

      <div class="chart-card todo-card">
        <h4>待确认预约<span>需前台跟进</span></h4>
        <div class="todo-list">
          <div v-if="!d.todos || !d.todos.length" class="muted" style="padding: 20px 0; text-align: center">
            暂无待确认预约
          </div>
          <div v-for="t in d.todos" :key="t.id" class="todo-item" @click="$router.push('/booking')">
            <div class="line1">
              <span class="strong">{{ t.customer_name }}</span>
              <el-tag size="small" type="warning" effect="plain">待确认</el-tag>
            </div>
            <div class="line2">{{ t.site_name }} · {{ t.guest_count }} 人</div>
            <div class="line3">{{ t.check_in_date }} → {{ t.check_out_date }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import ChartBox from '../components/ChartBox.vue'
import { dashboardApi } from '../api'
import { fmtNum, fmtPct } from '../utils/format'

const d = ref({})
const period = ref('30')
const loaded = ref(false)

async function load() {
  d.value = (await dashboardApi.get({ period: period.value })) || {}
  loaded.value = true
}

const axis = {
  grid: { left: 8, right: 12, top: 22, bottom: 4, containLabel: true },
  tooltip: { trigger: 'axis' }
}

const trendOpt = computed(() => {
  const rows = d.value.trend || []
  return {
    ...axis,
    xAxis: { type: 'category', data: rows.map((r) => r.label), axisLine: { lineStyle: { color: '#dfe6ed' } }, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0f4f8' } }, axisLabel: { fontSize: 11 } },
    series: [
      {
        name: '预约数', type: 'line', smooth: true, symbolSize: 5,
        data: rows.map((r) => r.value),
        areaStyle: { color: 'rgba(47,107,138,.14)' },
        lineStyle: { width: 2 }
      }
    ]
  }
})

const typeOpt = computed(() => {
  const rows = d.value.typeUsage || []
  return {
    ...axis,
    xAxis: { type: 'category', data: rows.map((r) => r.label), axisLabel: { fontSize: 10, interval: 0, rotate: 16 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0f4f8' } }, axisLabel: { fontSize: 11 } },
    series: [
      { name: '营位数', type: 'bar', barWidth: 16, data: rows.map((r) => r.total), itemStyle: { color: '#9db4c4', borderRadius: [3, 3, 0, 0] } },
      { name: '占用率', type: 'line', smooth: true, data: rows.map((r) => r.value), yAxisIndex: 0, itemStyle: { color: '#b07b2b' } }
    ]
  }
})

const availOpt = computed(() => {
  const rows = d.value.availDist || []
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 11 } },
    series: [
      {
        type: 'pie', radius: ['46%', '66%'], center: ['50%', '44%'],
        label: { formatter: '{b}\n{c}', fontSize: 11 },
        data: rows.map((r) => ({ name: r.label, value: r.value }))
      }
    ]
  }
})

const goodsOpt = computed(() => {
  const rows = [...(d.value.goodsRank || [])].reverse()
  return {
    grid: { left: 8, right: 26, top: 10, bottom: 4, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0f4f8' } }, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'category', data: rows.map((r) => r.label), axisLabel: { fontSize: 11 } },
    series: [{ type: 'bar', barWidth: 12, data: rows.map((r) => r.value), itemStyle: { color: '#2f6b8a', borderRadius: [0, 3, 3, 0] } }]
  }
})

const incomeOpt = computed(() => {
  const rows = d.value.incomeMix || []
  return {
    tooltip: { trigger: 'item', valueFormatter: (v) => '¥' + Number(v).toLocaleString() },
    legend: { bottom: 0, itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 11 } },
    series: [
      {
        type: 'pie', radius: '60%', center: ['50%', '44%'],
        label: { formatter: '{b} {d}%', fontSize: 11 },
        data: rows.map((r) => ({ name: r.label, value: r.value }))
      }
    ]
  }
})

const areaOpt = computed(() => {
  const rows = d.value.areaHeat || []
  return {
    radar: {
      indicator: rows.map((r) => ({ name: r.label, max: Math.max(...rows.map((x) => x.value), 4) + 2 })),
      radius: '62%', center: ['50%', '52%'],
      axisName: { fontSize: 10, color: '#5a6b7b' },
      splitLine: { lineStyle: { color: '#e9eff4' } },
      splitArea: { areaStyle: { color: ['#fff', '#fafcfd'] } }
    },
    tooltip: {},
    series: [{ type: 'radar', data: [{ value: rows.map((r) => r.value), name: '预约量' }], areaStyle: { opacity: .18 }, symbolSize: 4 }]
  }
})

onMounted(load)
</script>

<style scoped>
.dash { height: calc(100vh - 56px); display: flex; flex-direction: column; overflow: hidden; }
.grid {
  flex: 1; min-height: 0;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: 1fr 1fr;
  gap: 12px;
}
.grid > .chart-card { min-height: 0; }
.span2 { grid-column: span 2; }

.todo-card { padding-bottom: 6px; }
.todo-list { flex: 1; min-height: 0; overflow-y: auto; }
.todo-item { padding: 7px 8px; border-radius: 6px; cursor: pointer; border-bottom: 1px dashed var(--line); }
.todo-item:hover { background: #f6f9fb; }
.todo-item .line1 { display: flex; align-items: center; justify-content: space-between; font-size: 13px; }
.todo-item .line2 { font-size: 12px; color: var(--text-2); margin-top: 2px; }
.todo-item .line3 { font-size: 11px; color: var(--text-3); margin-top: 1px; }
</style>
