<template>
  <div class="page">
    <div class="page-head">
      <h2>入住与退营<span class="sub">{{ board.date || today }} 到营 {{ arrivals.length }} 组 · 离营 {{ departures.length }} 组</span></h2>
      <div class="ops">
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" @change="load" />
        <el-button type="primary" :icon="Switch" @click="$router.push('/checkout')">去退营结算</el-button>
      </div>
    </div>

    <!-- 今日到营：核验人数与安全告知后办理入住 -->
    <div class="card" style="margin-bottom: 12px">
      <div class="block-t">今日到营（{{ arrivals.length }}）</div>
      <el-table :data="arrivals" size="small" v-loading="loading">
        <el-table-column prop="booking_no" label="预约号" min-width="150">
          <template #default="{ row }"><span class="mono">{{ row.booking_no }}</span></template>
        </el-table-column>
        <el-table-column prop="customer_name" label="客户" width="100" />
        <el-table-column prop="customer_phone" label="手机号" width="130" />
        <el-table-column label="营位" min-width="140">
          <template #default="{ row }">{{ row.site_no }} {{ row.site_name }}</template>
        </el-table-column>
        <el-table-column label="入住 / 离营" min-width="180">
          <template #default="{ row }">{{ row.check_in_date }} → {{ row.check_out_date }}</template>
        </el-table-column>
        <el-table-column label="预订 / 实到" width="110" align="center">
          <template #default="{ row }">{{ row.guest_count }} 人</template>
        </el-table-column>
        <el-table-column label="物资" width="90" align="center">
          <template #default="{ row }">{{ row.rent_kinds || 0 }} 类</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="tagType('booking', row.status)">{{ label('bookingStatus', row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openCheckin(row)">办理入住</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!arrivals.length && !loading" class="muted" style="padding: 18px; text-align: center">今日暂无到营预约</div>
    </div>

    <!-- 今日离营 -->
    <div class="card">
      <div class="block-t">今日离营（{{ departures.length }}）</div>
      <el-table :data="departures" size="small">
        <el-table-column prop="booking_no" label="预约号" min-width="150">
          <template #default="{ row }"><span class="mono">{{ row.booking_no }}</span></template>
        </el-table-column>
        <el-table-column prop="customer_name" label="客户" width="100" />
        <el-table-column label="营位" min-width="140">
          <template #default="{ row }">{{ row.site_no }} {{ row.site_name }}</template>
        </el-table-column>
        <el-table-column label="入住 / 离营" min-width="180">
          <template #default="{ row }">{{ row.check_in_date }} → {{ row.check_out_date }}</template>
        </el-table-column>
        <el-table-column label="金额" width="110" align="right">
          <template #default="{ row }">{{ fmtMoney(row.total_fee) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="tagType('booking', row.status)">{{ label('bookingStatus', row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push('/checkout')">退营验收</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!departures.length" class="muted" style="padding: 18px; text-align: center">今日暂无离营预约</div>
    </div>

    <!-- 入住核验单 -->
    <el-dialog v-model="dlg" title="入住核验" width="560px" destroy-on-close>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 14px">
        需核对实到人数、安全告知与物资发放凭证；提交后营位状态变为「已入住」。
      </el-alert>
      <el-form :model="cf" label-width="104px">
        <el-form-item label="预约号"><span class="mono">{{ cur && cur.booking_no }}</span></el-form-item>
        <el-form-item label="实到人数">
          <el-input-number v-model="cf.actualGuestCount" :min="1" :max="20" />
          <span class="muted" style="margin-left: 8px">预订 {{ cur && cur.guest_count }} 人</span>
        </el-form-item>
        <el-form-item label="安全告知">
          <el-switch v-model="cf.safetyConfirmed" active-text="已告知用火/用电安全" />
        </el-form-item>
        <el-form-item label="物资照片">
          <UploadField v-model="cf.goodsPhoto" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="cf.remark" type="textarea" :rows="2" placeholder="迟到、加人、换营位等说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doCheckin">确认入住</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Switch } from '@element-plus/icons-vue'
import UploadField from '../components/UploadField.vue'
import { stayApi } from '../api'
import { fmtMoney, label, tagType } from '../utils/format'

const today = new Date().toISOString().slice(0, 10)
const date = ref(today)
const board = ref({})
const arrivals = ref([])
const departures = ref([])
const loading = ref(false)
const dlg = ref(false)
const saving = ref(false)
const cur = ref(null)
const cf = ref({ bookingId: null, actualGuestCount: 2, safetyConfirmed: true, goodsPhoto: '', remark: '' })

async function load() {
  loading.value = true
  try {
    const d = await stayApi.board({ date: date.value })
    board.value = d || {}
    arrivals.value = d.arrivals || []
    departures.value = d.departures || []
  } finally {
    loading.value = false
  }
}

function openCheckin(row) {
  cur.value = row
  cf.value = { bookingId: row.id, actualGuestCount: row.guest_count, safetyConfirmed: true, goodsPhoto: '', remark: '' }
  dlg.value = true
}

async function doCheckin() {
  if (!cf.value.safetyConfirmed) return ElMessage.warning('请先完成安全告知')
  saving.value = true
  try {
    await stayApi.checkin(cf.value)
    ElMessage.success('入住办理成功')
    dlg.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.block-t { font-size: 13px; font-weight: 600; color: var(--text-2); margin-bottom: 10px; padding-left: 8px; border-left: 3px solid var(--secondary); }
</style>
