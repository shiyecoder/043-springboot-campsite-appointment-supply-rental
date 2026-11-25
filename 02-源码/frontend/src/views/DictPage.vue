<template>
  <div class="page">
    <div class="page-head">
      <h2>字典与公告<span class="sub">共 {{ rows.length }} 条</span></h2>
      <div class="ops">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增字典项</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-select v-model="type" placeholder="全部字典类型" clearable @change="load" style="width: 220px">
        <el-option v-for="t in types" :key="t" :label="t" :value="t" />
      </el-select>
      <el-input v-model="kw" placeholder="按名称 / 值搜索" clearable :prefix-icon="Search" style="width: 220px" />
      <span class="grow"></span>
      <span class="muted">字典用于前台下拉与状态文案，业务枚举以代码内映射为准</span>
    </div>

    <div class="table-card">
      <el-table :data="shown" v-loading="loading" stripe>
        <el-table-column prop="dict_type" label="字典类型" min-width="150">
          <template #default="{ row }"><span class="mono">{{ row.dict_type }}</span></template>
        </el-table-column>
        <el-table-column prop="dict_label" label="显示名称" min-width="150" />
        <el-table-column prop="dict_value" label="字典值" min-width="130">
          <template #default="{ row }"><span class="mono">{{ row.dict_value }}</span></template>
        </el-table-column>
        <el-table-column prop="sort_no" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180">
          <template #default="{ row }"><span class="muted">{{ row.remark || '-' }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dlg" title="新增字典项" width="500px" destroy-on-close>
      <el-form :model="form" label-width="92px">
        <el-form-item label="字典类型" required>
          <el-input v-model="form.dictType" placeholder="如 camp_notice_type" />
        </el-form-item>
        <el-form-item label="显示名称" required><el-input v-model="form.dictLabel" /></el-form-item>
        <el-form-item label="字典值" required><el-input v-model="form.dictValue" placeholder="英文码或数字" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortNo" :min="0" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { systemApi } from '../api'

const rows = ref([])
const loading = ref(false)
const type = ref('')
const kw = ref('')
const dlg = ref(false)
const form = ref({ dictType: '', dictLabel: '', dictValue: '', sortNo: 0, remark: '' })

const types = computed(() => [...new Set(rows.value.map((r) => r.dict_type))])
const shown = computed(() =>
  rows.value.filter(
    (r) => (!type.value || r.dict_type === type.value) && (!kw.value || (r.dict_label || '').includes(kw.value) || (r.dict_value || '').includes(kw.value))
  )
)

async function load() {
  loading.value = true
  try {
    rows.value = (await systemApi.dicts()) || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = { dictType: type.value || '', dictLabel: '', dictValue: '', sortNo: 0, remark: '' }
  dlg.value = true
}

async function save() {
  if (!form.value.dictType || !form.value.dictLabel || !form.value.dictValue) return ElMessage.warning('请填写完整')
  await systemApi.saveDict({ ...form.value, status: 1 })
  ElMessage.success('已保存')
  dlg.value = false
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除字典项「${row.dict_label}」吗？`, '删除确认', { type: 'warning' })
  await systemApi.delDict(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
