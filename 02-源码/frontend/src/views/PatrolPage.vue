<template>
  <div class="page">
    <div class="page-head">
      <h2>巡检任务<span class="sub">共 {{ total }} 条</span></h2>
      <div class="ops">
        <el-button :icon="Cellphone" @click="$router.push('/mobile/patrol')">移动端巡检</el-button>
        <el-button :icon="Warning" @click="$router.push('/hazard')">隐患台账</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-select v-model="query.status" placeholder="全部状态" clearable @change="search">
        <el-option v-for="(v, k) in DICT.patrolStatus" :key="k" :label="v" :value="k" />
      </el-select>
      <el-select v-model="query.type" placeholder="全部巡检类型" clearable @change="search">
        <el-option v-for="(v, k) in DICT.patrolType" :key="k" :label="v" :value="k" />
      </el-select>
      <el-select v-model="query.areaId" placeholder="全部营区" clearable @change="search">
        <el-option v-for="a in areas" :key="a.id" :label="a.area_name" :value="a.id" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <div class="table-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column label="营区" width="96">
          <template #default="{ row }"><img class="thumb" :src="row.cover_image" :alt="row.area_name" /></template>
        </el-table-column>
        <el-table-column prop="task_no" label="任务编号" min-width="150">
          <template #default="{ row }"><span class="mono">{{ row.task_no }}</span></template>
        </el-table-column>
        <el-table-column prop="area_name" label="营区" min-width="110" />
        <el-table-column label="巡检类型" width="110">
          <template #default="{ row }">{{ label('patrolType', row.task_type) }}</template>
        </el-table-column>
        <el-table-column prop="inspector_name" label="巡检人" width="96" />
        <el-table-column prop="plan_date" label="计划日期" width="112" />
        <el-table-column label="结果" width="106">
          <template #default="{ row }">
            <el-tag v-if="row.result" size="small" :type="row.result === 'PROBLEM' ? 'warning' : 'success'">
              {{ label('patrolResult', row.result) }}
            </el-tag>
            <span v-else class="muted">待巡检</span>
          </template>
        </el-table-column>
        <el-table-column label="问题描述" min-width="220">
          <template #default="{ row }"><span class="muted">{{ row.problem_desc || '-' }}</span></template>
        </el-table-column>
        <el-table-column label="整改期限" width="112">
          <template #default="{ row }">{{ row.deadline || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="tagType('patrol', row.status)">{{ label('patrolStatus', row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="106" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'DONE'" link type="primary" size="small" @click="openFinish(row)">提交结果</el-button>
            <span v-else class="muted">已完成</span>
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

    <el-dialog v-model="dlg" title="提交巡检结果" width="560px" destroy-on-close>
      <el-form :model="ff" label-width="96px">
        <el-form-item label="任务">
          <span class="mono">{{ cur && cur.task_no }}</span>
          <span class="muted" style="margin-left: 8px">{{ cur && cur.area_name }} · {{ label('patrolType', cur && cur.task_type) }}</span>
        </el-form-item>
        <el-form-item label="巡检结果">
          <el-radio-group v-model="ff.result">
            <el-radio value="NORMAL">无异常</el-radio>
            <el-radio value="PROBLEM">发现问题</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="ff.result === 'PROBLEM'">
          <el-form-item label="问题等级">
            <el-select v-model="ff.problemLevel" style="width: 200px">
              <el-option v-for="(v, k) in DICT.hazardLevel" :key="k" :label="v" :value="k" />
            </el-select>
          </el-form-item>
          <el-form-item label="问题描述">
            <el-input v-model="ff.problemDesc" type="textarea" :rows="3" placeholder="具体位置、现象与风险" />
          </el-form-item>
          <el-form-item label="整改期限">
            <el-date-picker v-model="ff.deadline" type="date" value-format="YYYY-MM-DD" />
          </el-form-item>
        </template>
        <el-form-item label="现场照片">
          <UploadField v-model="ff.image" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Cellphone, Warning } from '@element-plus/icons-vue'
import UploadField from '../components/UploadField.vue'
import { patrolApi, siteApi } from '../api'
import { useTable } from '../composables/useTable'
import { DICT, label, tagType } from '../utils/format'

const areas = ref([])
const dlg = ref(false)
const saving = ref(false)
const cur = ref(null)
const ff = ref({ id: null, result: 'NORMAL', problemLevel: 'LOW', problemDesc: '', deadline: '', image: '' })

const { rows, total, loading, page, size, query, load, search, reset, onPage, onSize } = useTable(
  (p) => patrolApi.page(p),
  { status: '', type: '', areaId: '' }
)

function openFinish(row) {
  cur.value = row
  ff.value = { id: row.id, result: 'NORMAL', problemLevel: 'LOW', problemDesc: '', deadline: '', image: '' }
  dlg.value = true
}

async function submit() {
  if (ff.value.result === 'PROBLEM' && !ff.value.problemDesc) return ElMessage.warning('请填写问题描述')
  saving.value = true
  try {
    await patrolApi.finish({ ...ff.value })
    ElMessage.success('巡检结果已提交')
    dlg.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const opt = await siteApi.options()
  areas.value = opt.areas || []
  load()
})
</script>
