<template>
  <div class="page">
    <div class="page-head">
      <h2>预约列表<span class="sub">共 {{ total }} 条</span></h2>
      <div class="ops">
        <el-button :icon="Calendar" @click="$router.push('/booking/calendar')">日历视图</el-button>
        <el-button type="primary" :icon="Plus" @click="$router.push('/booking/new')">新建预约</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="预约号 / 客户 / 手机号" clearable :prefix-icon="Search" @keyup.enter="search" />
      <el-select v-model="query.status" placeholder="全部状态" clearable @change="search">
        <el-option v-for="(v, k) in DICT.bookingStatus" :key="k" :label="v" :value="k" />
      </el-select>
      <el-select v-model="query.siteId" placeholder="全部营位" clearable filterable @change="search">
        <el-option v-for="s in sites" :key="s.id" :label="s.site_no + ' ' + s.site_name" :value="s.id" />
      </el-select>
      <el-date-picker
        v-model="range"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="入住起始"
        end-placeholder="入住截止"
        @change="onRange"
      />
      <el-select v-model="query.quick" placeholder="快捷筛选" clearable @change="search">
        <el-option label="今日到营" value="todayIn" />
        <el-option label="今日离营" value="todayOut" />
        <el-option label="待确认" value="pending" />
        <el-option label="未归还物资" value="overdue" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="onReset">重置</el-button>
    </div>

    <div class="table-card">
      <el-table :data="rows" v-loading="loading" stripe @row-dblclick="openDetail">
        <el-table-column label="营位" width="96">
          <template #default="{ row }">
            <img class="thumb" :src="row.cover_image" :alt="row.site_no" />
          </template>
        </el-table-column>
        <el-table-column prop="booking_no" label="预约号" min-width="150">
          <template #default="{ row }"><span class="mono">{{ row.booking_no }}</span></template>
        </el-table-column>
        <el-table-column label="客户" min-width="120">
          <template #default="{ row }">
            <div>{{ row.customer_name }}</div>
            <div class="muted">{{ row.customer_phone }}</div>
          </template>
        </el-table-column>
        <el-table-column label="营位" min-width="140">
          <template #default="{ row }">
            {{ row.site_no }} {{ row.site_name }}
            <div class="muted">{{ row.area_name }}</div>
          </template>
        </el-table-column>
        <el-table-column label="入住 / 离营" min-width="180">
          <template #default="{ row }">
            {{ row.check_in_date }} → {{ row.check_out_date }}
            <div class="muted">{{ row.nights }} 晚</div>
          </template>
        </el-table-column>
        <el-table-column label="人数" width="86" align="center">
          <template #default="{ row }">
            {{ row.guest_count }}
            <div class="muted" v-if="row.guest_count_real && row.guest_count_real !== row.guest_count">
              实到 {{ row.guest_count_real }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="物资" width="72" align="center">
          <template #default="{ row }">{{ row.rent_kinds || 0 }} 类</template>
        </el-table-column>
        <el-table-column label="金额" width="110" align="right">
          <template #default="{ row }"><span class="money">{{ fmtMoney(row.total_fee) }}</span></template>
        </el-table-column>
        <el-table-column label="来源" width="96">
          <template #default="{ row }"><span class="muted">{{ label('source', row.source) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="106">
          <template #default="{ row }">
            <el-tag size="small" :type="tagType('booking', row.status)">{{ label('bookingStatus', row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="196" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button
              v-for="t in nextStates(row.status).slice(0, 2)"
              :key="t"
              link
              size="small"
              :type="t === 'REJECTED' || t === 'CANCELLED' || t === 'NOSHOW' ? 'danger' : 'success'"
              @click="doTransition(row, t)"
            >
              {{ actName(t) }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="onPage"
          @size-change="onSize"
        />
      </div>
    </div>

    <!-- 预约详情：状态流转留痕 + 物资明细 + 金额构成 -->
    <el-drawer v-model="drawer" title="预约详情" size="680px" destroy-on-close>
      <div v-if="detail" class="col" style="gap: 14px">
        <div class="card">
          <div class="flex aic" style="justify-content: space-between; margin-bottom: 10px">
            <div>
              <div class="strong" style="font-size: 15px">{{ detail.booking && detail.booking.booking_no }}</div>
              <div class="muted">{{ detail.booking && detail.booking.site_name }} · {{ detail.booking && detail.booking.area_name }}</div>
            </div>
            <el-tag :type="tagType('booking', detail.booking && detail.booking.status)">
              {{ label('bookingStatus', detail.booking && detail.booking.status) }}
            </el-tag>
          </div>
          <dl class="kv">
            <dt>客户</dt><dd>{{ detail.booking && detail.booking.customer_name }} · {{ detail.booking && detail.booking.customer_phone }}</dd>
            <dt>入住</dt><dd>{{ detail.booking && detail.booking.check_in_date }} → {{ detail.booking && detail.booking.check_out_date }}（{{ detail.booking && detail.booking.nights }} 晚）</dd>
            <dt>人数</dt><dd>{{ detail.booking && detail.booking.guest_count }} 人</dd>
            <dt>来源</dt><dd>{{ label('source', detail.booking && detail.booking.source) }}</dd>
            <dt>特殊需求</dt><dd>{{ (detail.booking && detail.booking.special_need) || '无' }}</dd>
            <dt>营位费</dt><dd class="money">{{ fmtMoney(detail.booking && detail.booking.site_fee) }}</dd>
            <dt>物资费</dt><dd class="money">{{ fmtMoney(detail.booking && detail.booking.goods_fee) }}</dd>
            <dt>押金</dt><dd>{{ fmtMoney(detail.booking && detail.booking.deposit) }}</dd>
            <dt>合计</dt><dd class="money">{{ fmtMoney(detail.booking && detail.booking.total_fee) }}</dd>
          </dl>
        </div>

        <div class="card" v-if="detail.rentItems && detail.rentItems.length">
          <div class="strong" style="margin-bottom: 8px">租赁物资</div>
          <el-table :data="detail.rentItems" size="small">
            <el-table-column prop="goods_name" label="物资" min-width="140" />
            <el-table-column prop="qty" label="数量" width="70" align="center" />
            <el-table-column prop="rent_days" label="天数" width="70" align="center" />
            <el-table-column label="小计" width="100" align="right">
              <template #default="{ row }">{{ fmtMoney(row.amount) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="94">
              <template #default="{ row }">
                <el-tag size="small" :type="tagType('item', row.status)">{{ label('itemStatus', row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="card">
          <div class="strong" style="margin-bottom: 8px">状态流转留痕</div>
          <ul class="timeline">
            <li v-for="c in detail.changes || []" :key="c.id">
              <div>{{ label('bookingStatus', c.old_value) }} → <b>{{ label('bookingStatus', c.new_value) }}</b></div>
              <div class="muted">{{ c.reason }}</div>
              <div class="t">{{ c.operator_name }} · {{ fmtDateTime(c.create_time) }}</div>
            </li>
          </ul>
          <div v-if="!detail.changes || !detail.changes.length" class="muted">暂无流转记录</div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Plus, Search } from '@element-plus/icons-vue'
import { bookingApi, siteApi } from '../api'
import { useTable } from '../composables/useTable'
import { DICT, fmtDateTime, fmtMoney, label, tagType } from '../utils/format'

/** 与后端 FLOW 状态机保持一致，只展示合法目标状态 */
const FLOW = {
  DRAFT: ['SUBMITTED'],
  SUBMITTED: ['RESERVED', 'REJECTED', 'CANCELLED'],
  CONFIRMED: ['RESERVED', 'OCCUPIED', 'CANCELLED'],
  RESERVED: ['OCCUPIED', 'NOSHOW', 'CANCELLED'],
  OCCUPIED: ['CHECKED'],
  CHECKED: ['COMPLETED'],
  COMPLETED: [], CANCELLED: [], REJECTED: [], NOSHOW: []
}
const ACT = {
  SUBMITTED: '提交', RESERVED: '预留', REJECTED: '拒绝', CANCELLED: '取消',
  OCCUPIED: '办理入住', CHECKED: '退营登记', COMPLETED: '完成'
}

const sites = ref([])
const range = ref([])
const drawer = ref(false)
const detail = ref(null)

const { rows, total, loading, page, size, query, load, search, reset, onPage, onSize } = useTable(
  (p) => bookingApi.page(p),
  { keyword: '', status: '', siteId: '', quick: '', from: '', to: '' }
)

const nextStates = (s) => FLOW[s] || []
const actName = (t) => ACT[t] || t

function onRange(v) {
  query.from = v && v[0] ? v[0] : ''
  query.to = v && v[1] ? v[1] : ''
  search()
}

function onReset() {
  range.value = []
  query.from = ''
  query.to = ''
  reset()
}

async function openDetail(row) {
  const r = await bookingApi.detail(row.id)
  detail.value = r
  drawer.value = true
}

async function doTransition(row, target) {
  const needReason = ['CANCELLED', 'REJECTED', 'NOSHOW'].includes(target)
  let reason = ''
  if (needReason) {
    const r = await ElMessageBox.prompt(`请输入${actName(target)}原因（不少于 5 个字）`, '填写原因', {
      inputPlaceholder: '如：客户行程变更，主动取消',
      inputValidator: (v) => (v && v.trim().length >= 5) || '原因至少 5 个字'
    })
    reason = r.value
  } else {
    await ElMessageBox.confirm(`确认将 ${row.booking_no} 变更为「${label('bookingStatus', target)}」吗？`, '状态流转', { type: 'warning' })
  }
  await bookingApi.transition(row.id, { target, reason })
  ElMessage.success('已更新为' + label('bookingStatus', target))
  load()
}

onMounted(async () => {
  // 营位下拉：options 只给营区/类型，营位清单走列表接口一次取全
  const page = await siteApi.page({ page: 1, size: 200 })
  sites.value = page.records || []
  load()
})
</script>
