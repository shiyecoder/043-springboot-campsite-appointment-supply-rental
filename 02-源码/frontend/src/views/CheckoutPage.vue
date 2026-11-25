<template>
  <div class="page">
    <div class="page-head">
      <h2>退营验收与结算<span class="sub">按预约结算，费用与押金自动核算</span></h2>
      <div class="ops">
        <el-button :icon="Switch" @click="$router.push('/stay')">回入住与退营</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-select v-model="bookingId" filterable placeholder="选择待退营预约" style="width: 320px" @change="preview">
        <el-option
          v-for="b in checkable"
          :key="b.id"
          :label="`${b.booking_no} · ${b.customer_name} · ${b.site_no}`"
          :value="b.id"
        />
      </el-select>
      <el-button type="primary" :disabled="!bookingId" @click="preview">载入结算单</el-button>
      <span class="grow"></span>
      <span class="muted">仅「已入住」状态的预约可结算</span>
    </div>

    <div v-if="pv" class="detail-grid">
      <div class="col" style="gap: 12px">
        <div class="card">
          <div class="sec-t">退营验收</div>
          <el-form :model="form" label-width="104px">
            <el-row :gutter="14">
              <el-col :span="12">
                <el-form-item label="清洁程度">
                  <el-rate v-model="form.cleanliness" :max="5" show-score score-template="{value} 分" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="支付方式">
                  <el-select v-model="form.payType" style="width: 100%">
                    <el-option v-for="(v, k) in DICT.payType" :key="k" :label="v" :value="k" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="减免金额">
                  <el-input-number v-model="form.discount" :min="0" :step="10" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="损坏说明">
                  <el-input v-model="form.damageDesc" placeholder="无损坏可留空" />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="支付备注">
                  <el-input v-model="form.payRemark" placeholder="如：微信收款，差额现场退回" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </div>

        <div class="card">
          <div class="sec-t">物资归还核对</div>
          <el-table :data="pv.rentItems || []" size="small">
            <el-table-column prop="goods_name" label="物资" min-width="160" />
            <el-table-column prop="qty" label="数量" width="70" align="center" />
            <el-table-column prop="rent_days" label="天数" width="70" align="center" />
            <el-table-column label="小计" width="100" align="right">
              <template #default="{ row }">{{ fmtMoney(row.amount) }}</template>
            </el-table-column>
            <el-table-column label="归还状态" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="tagType('item', row.status)">{{ label('itemStatus', row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <div class="col" style="gap: 12px">
        <div class="card">
          <div class="sec-t">结算单</div>
          <dl class="kv">
            <dt>预约号</dt><dd class="mono">{{ pv.booking && pv.booking.booking_no }}</dd>
            <dt>客户</dt><dd>{{ pv.booking && pv.booking.customer_name }}</dd>
            <dt>营位</dt><dd>{{ pv.booking && pv.booking.site_no }} {{ pv.booking && pv.booking.site_name }}</dd>
            <dt>营位费</dt><dd class="money">{{ fmtMoney(pv.siteFee) }}</dd>
            <dt>物资费</dt><dd class="money">{{ fmtMoney(pv.goodsFee) }}</dd>
            <dt>超时</dt><dd>{{ pv.overtimeHours }} 小时 · {{ fmtMoney(pv.overtimeFee) }}</dd>
            <dt>损坏赔偿</dt><dd>{{ fmtMoney(pv.damageFee) }}</dd>
            <dt>减免</dt><dd>-{{ fmtMoney(manualDiscount) }}</dd>
            <dt>应收合计</dt>
            <dd class="money" style="font-size: 17px">{{ fmtMoney(payable) }}</dd>
            <dt>押金</dt><dd>{{ fmtMoney(pv.deposit) }}</dd>
            <dt>押金退回</dt><dd class="ok-text">{{ fmtMoney(pv.depositRefund) }}</dd>
          </dl>
          <el-alert type="warning" :closable="false" show-icon style="margin: 12px 0">
            押金退回 = 押金 − 损坏赔偿；应收合计 = 营位费 + 物资费 + 超时费 + 损坏赔偿 − 减免
          </el-alert>
          <el-button type="primary" style="width: 100%" :loading="saving" @click="settle">确认结算并退营</el-button>
        </div>

        <div class="card">
          <div class="sec-t">结算留痕</div>
          <div class="muted" style="line-height: 1.8">
            结算生成 checkout_record 记录，营位状态转为「待清洁」，
            预约状态转「退营待结算」；随后在预约列表点「完成」归档。
          </div>
        </div>
      </div>
    </div>

    <div v-else class="card" style="text-align: center; padding: 46px 0">
      <el-empty description="请选择待退营的预约以生成结算单" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Switch } from '@element-plus/icons-vue'
import { bookingApi, stayApi } from '../api'
import { DICT, fmtMoney, label, tagType } from '../utils/format'

const bookingId = ref(null)
const checkable = ref([])
const pv = ref(null)
const saving = ref(false)
const form = ref({ bookingId: null, discount: 0, cleanliness: 5, damageDesc: '', payType: 'WECHAT', payRemark: '' })

const manualDiscount = computed(() => Number(form.value.discount) || 0)
const payable = computed(() => {
  if (!pv.value) return 0
  return (
    Number(pv.value.siteFee || 0) +
    Number(pv.value.goodsFee || 0) +
    Number(pv.value.overtimeFee || 0) +
    Number(pv.value.damageFee || 0) -
    manualDiscount.value
  )
})

async function preview() {
  if (!bookingId.value) return
  pv.value = await stayApi.checkoutPreview(bookingId.value)
  form.value.bookingId = bookingId.value
}

async function settle() {
  saving.value = true
  try {
    await stayApi.settle({ ...form.value })
    ElMessage.success('结算完成')
    pv.value = null
    bookingId.value = null
    await loadList()
  } finally {
    saving.value = false
  }
}

async function loadList() {
  const r = await bookingApi.page({ page: 1, size: 100, status: 'OCCUPIED' })
  checkable.value = r.records || []
}

onMounted(loadList)
</script>

<style scoped>
.sec-t { font-size: 13px; font-weight: 600; color: var(--text-2); margin-bottom: 10px; padding-left: 8px; border-left: 3px solid var(--secondary); }
</style>
