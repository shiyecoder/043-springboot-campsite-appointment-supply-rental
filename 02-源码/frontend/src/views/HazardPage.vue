<template>
  <div class="page">
    <div class="page-head">
      <h2>隐患与事件<span class="sub">隐患 {{ hazardTotal }} 条 · 事件 {{ eventTotal }} 条</span></h2>
      <div class="ops">
        <el-button type="primary" :icon="Plus" @click="openEvent">登记现场事件</el-button>
      </div>
    </div>

    <el-tabs v-model="tab" class="tabs">
      <el-tab-pane label="隐患台账" name="hazard">
        <div class="filter-bar">
          <el-select v-model="hq.status" placeholder="全部状态" clearable @change="loadHazard">
            <el-option v-for="(v, k) in DICT.hazardStatus" :key="k" :label="v" :value="k" />
          </el-select>
          <el-button type="primary" @click="loadHazard">查询</el-button>
        </div>
        <div class="table-card">
          <el-table :data="hazards" v-loading="hLoading" stripe>
            <el-table-column label="照片" width="96">
              <template #default="{ row }"><img class="thumb" :src="row.image" :alt="row.hazard_no" /></template>
            </el-table-column>
            <el-table-column prop="hazard_no" label="隐患单号" min-width="150">
              <template #default="{ row }"><span class="mono">{{ row.hazard_no }}</span></template>
            </el-table-column>
            <el-table-column prop="area_name" label="营区" min-width="110" />
            <el-table-column prop="hazard_type" label="类型" width="110" />
            <el-table-column label="等级" width="86">
              <template #default="{ row }">
                <el-tag size="small" :type="tagType('hazard', row.level)">{{ label('hazardLevel', row.level) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="隐患描述" min-width="240" show-overflow-tooltip />
            <el-table-column prop="rectify_owner" label="整改责任人" width="110" />
            <el-table-column prop="deadline" label="期限" width="110" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="tagType('hazard', row.status)">{{ label('hazardStatus', row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status !== 'CLOSED'" link type="primary" size="small" @click="closeHazard(row)">关闭隐患</el-button>
                <span v-else class="muted">已关闭</span>
              </template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination
              v-model:current-page="hp"
              :total="hazardTotal"
              :page-size="10"
              layout="total, prev, pager, next"
              @current-change="(p) => { hp = p; loadHazard() }"
            />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="现场事件" name="event">
        <div class="filter-bar">
          <el-select v-model="eq.status" placeholder="全部状态" clearable @change="loadEvent">
            <el-option v-for="(v, k) in DICT.eventStatus" :key="k" :label="v" :value="k" />
          </el-select>
          <el-select v-model="eq.type" placeholder="全部类型" clearable @change="loadEvent">
            <el-option v-for="(v, k) in DICT.eventType" :key="k" :label="v" :value="k" />
          </el-select>
          <el-button type="primary" @click="loadEvent">查询</el-button>
        </div>
        <div class="table-card">
          <el-table :data="events" v-loading="eLoading" stripe>
            <el-table-column prop="event_no" label="事件编号" min-width="150">
              <template #default="{ row }"><span class="mono">{{ row.event_no }}</span></template>
            </el-table-column>
            <el-table-column label="类型" width="110">
              <template #default="{ row }">{{ label('eventType', row.event_type) }}</template>
            </el-table-column>
            <el-table-column label="等级" width="86">
              <template #default="{ row }">
                <el-tag size="small" :type="tagType('hazard', row.level)">{{ label('hazardLevel', row.level) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="营位" min-width="140">
              <template #default="{ row }">{{ row.site_no }} {{ row.site_name }}</template>
            </el-table-column>
            <el-table-column prop="content" label="事件内容" min-width="260" show-overflow-tooltip />
            <el-table-column prop="reporter_name" label="上报人" width="96" />
            <el-table-column label="上报时间" width="150">
              <template #default="{ row }">{{ fmtMinute(row.create_time) }}</template>
            </el-table-column>
            <el-table-column label="处理状态" width="106">
              <template #default="{ row }">
                <el-tag size="small" :type="tagType('event', row.handle_status)">{{ label('eventStatus', row.handle_status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="handle_result" label="处理结果" min-width="180">
              <template #default="{ row }"><span class="muted">{{ row.handle_result || '-' }}</span></template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination
              v-model:current-page="ep"
              :total="eventTotal"
              :page-size="10"
              layout="total, prev, pager, next"
              @current-change="(p) => { ep = p; loadEvent() }"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dlg" title="登记现场事件" width="560px" destroy-on-close>
      <el-form :model="ef" label-width="96px">
        <el-form-item label="事件类型">
          <el-select v-model="ef.eventType" style="width: 100%">
            <el-option v-for="(v, k) in DICT.eventType" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="等级">
          <el-radio-group v-model="ef.level">
            <el-radio value="LOW">轻微</el-radio>
            <el-radio value="MID">一般</el-radio>
            <el-radio value="HIGH">严重</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="关联营位">
          <el-select v-model="ef.siteId" filterable clearable style="width: 100%">
            <el-option v-for="s in sites" :key="s.id" :label="s.site_no + ' ' + s.site_name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="事件内容">
          <el-input v-model="ef.content" type="textarea" :rows="3" placeholder="时间、位置、现场情况与已采取措施" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEvent">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { eventApi, hazardApi, siteApi } from '../api'
import { DICT, fmtMinute, label, tagType } from '../utils/format'

const tab = ref('hazard')
const hazards = ref([])
const events = ref([])
const sites = ref([])
const hazardTotal = ref(0)
const eventTotal = ref(0)
const hp = ref(1)
const ep = ref(1)
const hLoading = ref(false)
const eLoading = ref(false)
const hq = ref({ status: '' })
const eq = ref({ status: '', type: '' })
const dlg = ref(false)
const saving = ref(false)
const ef = ref({ eventType: 'NOISE', level: 'LOW', siteId: null, content: '' })

async function loadHazard() {
  hLoading.value = true
  try {
    const r = await hazardApi.page({ page: hp.value, size: 10, status: hq.value.status || undefined })
    hazards.value = r.records || []
    hazardTotal.value = r.total || 0
  } finally {
    hLoading.value = false
  }
}

async function loadEvent() {
  eLoading.value = true
  try {
    const r = await eventApi.page({ page: ep.value, size: 10, status: eq.value.status || undefined, type: eq.value.type || undefined })
    events.value = r.records || []
    eventTotal.value = r.total || 0
  } finally {
    eLoading.value = false
  }
}

async function closeHazard(row) {
  const r = await ElMessageBox.prompt('请填写整改完成说明', '关闭隐患', {
    inputPlaceholder: '如：已更换灭火器并复检合格'
  })
  await hazardApi.close(row.id, r.value)
  ElMessage.success('隐患已关闭')
  loadHazard()
}

function openEvent() {
  ef.value = { eventType: 'NOISE', level: 'LOW', siteId: null, content: '' }
  dlg.value = true
}

async function saveEvent() {
  if (!ef.value.content) return ElMessage.warning('请填写事件内容')
  saving.value = true
  try {
    await eventApi.create({ ...ef.value })
    ElMessage.success('事件已登记')
    dlg.value = false
    tab.value = 'event'
    loadEvent()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const page = await siteApi.page({ page: 1, size: 200 })
  sites.value = page.records || []
  loadHazard()
  loadEvent()
})
</script>

<style scoped>
.tabs :deep(.el-tabs__header) { margin-bottom: 12px; }
</style>
