<template>
  <div class="page">
    <div class="page-head">
      <h2>备货与发放<span class="sub">{{ pick.date }} · 待发放 {{ pendingQty }} 项</span></h2>
      <div class="ops">
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" @change="load" />
        <el-button :icon="Box" @click="$router.push('/goods')">物资档案</el-button>
      </div>
    </div>

    <div class="card">
      <div class="sec-t">今日备货清单</div>
      <el-table :data="rows" v-loading="loading" size="small">
        <el-table-column label="物资" min-width="190">
          <template #default="{ row }">
            <div class="flex aic gap8">
              <img class="thumb" :src="row.image" style="width: 46px; height: 34px" :alt="row.goods_name" />
              <div>
                <div>{{ row.goods_name }}</div>
                <div class="muted">{{ row.rent_unit }}租 · 已预留 {{ row.reserved_qty }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
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
        <el-table-column label="数量 / 天数" width="110" align="center">
          <template #default="{ row }">{{ row.qty }} 件 · {{ row.rent_days }} 天</template>
        </el-table-column>
        <el-table-column label="库存" width="80" align="center">
          <template #default="{ row }">{{ row.total_stock }}</template>
        </el-table-column>
        <el-table-column label="发放状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="tagType('item', row.item_status)">{{ label('itemStatus', row.item_status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.item_status === 'PENDING'"
              link
              type="primary"
              size="small"
              @click="openHandover(row, 'PICK')"
            >
              发放
            </el-button>
            <el-button
              v-else-if="row.item_status === 'PICKED'"
              link
              type="success"
              size="small"
              @click="openHandover(row, 'RETURN')"
            >
              归还验收
            </el-button>
            <span v-else class="muted">无操作</span>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!rows.length && !loading" class="muted" style="padding: 22px; text-align: center">
        今日没有需要备货的物资
      </div>
    </div>

    <el-dialog v-model="dlg" :title="mode === 'PICK' ? '物资发放' : '归还验收'" width="520px" destroy-on-close>
      <el-form :model="hf" label-width="92px">
        <el-form-item label="物资"><span>{{ cur && cur.goods_name }} × {{ cur && cur.qty }}</span></el-form-item>
        <el-form-item label="预约号"><span class="mono">{{ cur && cur.booking_no }}</span></el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="hf.qty" :min="1" :max="cur ? cur.qty : 1" />
        </el-form-item>
        <el-form-item v-if="mode === 'PICK'" label="物资状态">
          <el-radio-group v-model="hf.itemStatus">
            <el-radio value="PICKED">正常发放</el-radio>
            <el-radio value="DAMAGED">发放即损坏</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-else label="验收结果">
          <el-radio-group v-model="hf.itemStatus">
            <el-radio value="RETURNED">完好归还</el-radio>
            <el-radio value="DAMAGED">损坏</el-radio>
            <el-radio value="LOST">遗失</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="mode === 'PICK'" label="编号">
          <el-input v-model="hf.serialNo" placeholder="物资编号/序列号（选填）" />
        </el-form-item>
        <el-form-item label="现场照片">
          <UploadField v-model="hf.photo" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="hf.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Box } from '@element-plus/icons-vue'
import UploadField from '../components/UploadField.vue'
import { goodsApi } from '../api'
import { label, tagType } from '../utils/format'

const date = ref(new Date().toISOString().slice(0, 10))
const pick = ref({})
const rows = ref([])
const pendingQty = ref(0)
const loading = ref(false)
const dlg = ref(false)
const saving = ref(false)
const mode = ref('PICK')
const cur = ref(null)
const hf = ref({ rentItemId: null, handoverType: 'PICK', qty: 1, itemStatus: 'PICKED', serialNo: '', photo: '', remark: '' })

async function load() {
  loading.value = true
  try {
    const d = await goodsApi.picklist({ date: date.value })
    pick.value = d || {}
    rows.value = d.rows || []
    pendingQty.value = d.pendingQty || 0
  } finally {
    loading.value = false
  }
}

function openHandover(row, kind) {
  cur.value = row
  mode.value = kind
  hf.value = {
    rentItemId: row.item_id,
    handoverType: kind,
    qty: row.qty,
    itemStatus: kind === 'PICK' ? 'PICKED' : 'RETURNED',
    serialNo: '',
    photo: '',
    remark: ''
  }
  dlg.value = true
}

async function submit() {
  saving.value = true
  try {
    await goodsApi.handover({ ...hf.value })
    ElMessage.success('已记录')
    dlg.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.sec-t { font-size: 13px; font-weight: 600; color: var(--text-2); margin-bottom: 10px; padding-left: 8px; border-left: 3px solid var(--secondary); }
</style>
