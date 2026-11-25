<template>
  <div class="page">
    <div class="page-head">
      <h2>评价与黑名单<span class="sub">评价 {{ total }} 条</span></h2>
      <div class="ops">
        <el-button type="primary" :icon="Plus" @click="openBlack">申请加入黑名单</el-button>
      </div>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane label="客户评价" name="review">
        <div class="filter-bar">
          <el-select v-model="query.minScore" placeholder="全部评分" clearable style="width: 200px" @change="search">
            <el-option label="5 分" :value="5" />
            <el-option label="4 分及以上" :value="4" />
            <el-option label="3 分及以下（待改进）" :value="0" />
          </el-select>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <span class="grow"></span>
          <span class="muted">评价来自退营结算后的客户回访</span>
        </div>

        <div class="table-card">
          <el-table :data="rows" v-loading="loading" stripe>
            <el-table-column prop="booking_no" label="预约号" min-width="150">
              <template #default="{ row }"><span class="mono">{{ row.booking_no }}</span></template>
            </el-table-column>
            <el-table-column prop="customer_name" label="客户" width="110" />
            <el-table-column label="营位" min-width="140">
              <template #default="{ row }">{{ row.site_no }} {{ row.site_name }}</template>
            </el-table-column>
            <el-table-column label="评分" width="150" align="center">
              <template #default="{ row }">
                <el-rate :model-value="Number(row.avg_score)" disabled size="small" />
                <div class="muted">{{ row.avg_score }} 分</div>
              </template>
            </el-table-column>
            <el-table-column label="分项" width="180">
              <template #default="{ row }">
                <span class="muted">营位 {{ row.site_score }} · 环境 {{ row.env_score }} · 设施 {{ row.facility_score }} · 服务 {{ row.service_score }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="评价内容" min-width="240" show-overflow-tooltip />
            <el-table-column label="回复" min-width="180">
              <template #default="{ row }">
                <span v-if="row.reply">{{ row.reply }}</span>
                <el-button v-else link type="primary" size="small" @click="reply(row)">回复</el-button>
              </template>
            </el-table-column>
            <el-table-column label="评价时间" width="150">
              <template #default="{ row }">{{ fmtMinute(row.create_time) }}</template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination
              v-model:current-page="page"
              v-model:page-size="size"
              :total="total"
              layout="total, sizes, prev, pager, next"
              :page-sizes="[10, 20, 50]"
              @current-change="onPage"
              @size-change="onSize"
            />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="黑名单" name="black">
        <div class="table-card" style="margin-top: 0">
          <el-table :data="blacks" v-loading="bLoading" stripe>
            <el-table-column prop="customer_name" label="客户" width="120" />
            <el-table-column prop="phone" label="手机号" width="130" />
            <el-table-column label="原因类型" width="120">
              <template #default="{ row }">{{ label('reasonType', row.reason_type) }}</template>
            </el-table-column>
            <el-table-column prop="reason" label="详细原因" min-width="260" show-overflow-tooltip />
            <el-table-column prop="apply_name" label="申请人" width="100" />
            <el-table-column label="申请时间" width="150">
              <template #default="{ row }">{{ fmtMinute(row.create_time) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="tagType('black', row.status)">{{ label('blackStatus', row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 'PENDING'">
                  <el-button link type="success" size="small" @click="audit(row, 'APPROVED')">通过</el-button>
                  <el-button link type="danger" size="small" @click="audit(row, 'REJECTED')">驳回</el-button>
                </template>
                <el-button v-else-if="row.status === 'ACTIVE'" link type="primary" size="small" @click="release(row)">解除</el-button>
                <span v-else class="muted">{{ row.audit_name || '-' }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dlg" title="申请加入黑名单" width="520px" destroy-on-close>
      <el-form :model="bf" label-width="96px">
        <el-form-item label="客户姓名"><el-input v-model="bf.customerName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="bf.phone" maxlength="11" /></el-form-item>
        <el-form-item label="原因类型">
          <el-select v-model="bf.reasonType" style="width: 100%">
            <el-option v-for="(v, k) in DICT.reasonType" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="详细原因">
          <el-input v-model="bf.reason" type="textarea" :rows="3" placeholder="具体行为与损失情况" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="submitBlack">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { blacklistApi, reviewApi } from '../api'
import { useTable } from '../composables/useTable'
import { DICT, fmtMinute, label, tagType } from '../utils/format'

const tab = ref('review')
const blacks = ref([])
const bLoading = ref(false)
const dlg = ref(false)
const bf = ref({ customerName: '', phone: '', reasonType: 'DAMAGE', reason: '' })

const { rows, total, loading, page, size, query, load, search, reset, onPage, onSize } = useTable(
  (p) => reviewApi.page(p),
  { minScore: '' }
)

async function reply(row) {
  const r = await ElMessageBox.prompt('回复客户评价', '评价回复', { inputPlaceholder: '感谢您的反馈…' })
  await reviewApi.reply(row.id, r.value)
  ElMessage.success('已回复')
  load()
}

async function loadBlack() {
  bLoading.value = true
  try {
    const r = await blacklistApi.page({ page: 1, size: 50 })
    blacks.value = r.records || []
  } finally {
    bLoading.value = false
  }
}

function openBlack() {
  bf.value = { customerName: '', phone: '', reasonType: 'DAMAGE', reason: '' }
  dlg.value = true
}

async function submitBlack() {
  if (!bf.value.customerName || !bf.value.reason) return ElMessage.warning('请填写客户与原因')
  await blacklistApi.apply({ ...bf.value })
  ElMessage.success('已提交，等待管理员审核')
  dlg.value = false
  tab.value = 'black'
  loadBlack()
}

async function audit(row, result) {
  const r = await ElMessageBox.prompt(`请填写${result === 'APPROVED' ? '通过' : '驳回'}意见`, '审核', { inputPlaceholder: '审核意见' })
  await blacklistApi.audit(row.id, result, r.value)
  ElMessage.success('已处理')
  loadBlack()
}

async function release(row) {
  const r = await ElMessageBox.prompt('请填写解除原因', '解除黑名单', { inputPlaceholder: '如：客户已完成赔偿并致歉' })
  await blacklistApi.release(row.id, r.value)
  ElMessage.success('已解除')
  loadBlack()
}

watch(tab, (v) => {
  if (v === 'black') loadBlack()
})

onMounted(() => {
  load()
  loadBlack()
})
</script>
