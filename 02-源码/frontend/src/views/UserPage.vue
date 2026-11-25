<template>
  <div class="page">
    <div class="page-head">
      <h2>用户管理<span class="sub">共 {{ total }} 个账号</span></h2>
      <div class="ops">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增用户</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="账号 / 姓名 / 手机号" clearable :prefix-icon="Search" @keyup.enter="search" />
      <el-select v-model="query.roleCode" placeholder="全部角色" clearable @change="search">
        <el-option v-for="r in roles" :key="r.role_code || r.roleCode" :label="r.role_name || r.roleName" :value="r.role_code || r.roleCode" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <div class="table-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column label="头像" width="76">
          <template #default="{ row }">
            <el-avatar :size="34" :src="row.avatar || undefined">{{ (row.realName || row.username).slice(0, 1) }}</el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="账号" min-width="120">
          <template #default="{ row }"><span class="mono">{{ row.username }}</span></template>
        </el-table-column>
        <el-table-column prop="realName" label="姓名" min-width="130" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="roleName" label="角色" width="130">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.roleName || row.role_code }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orgName" label="所属机构" min-width="140" />
        <el-table-column label="数据范围" width="110">
          <template #default="{ row }">{{ scopeName(row.dataScope) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上次登录" width="150">
          <template #default="{ row }">{{ fmtMinute(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="resetPwd(row)">重置密码</el-button>
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

    <el-dialog v-model="dlg" :title="form.id ? '编辑用户' : '新增用户'" width="580px" destroy-on-close>
      <el-form :model="form" label-width="92px">
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="账号" required>
              <el-input v-model="form.username" :disabled="!!form.id" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" required><el-input v-model="form.realName" /></el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="手机号"><el-input v-model="form.phone" maxlength="11" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="角色">
              <el-select v-model="form.roleCode" style="width: 100%">
                <el-option v-for="r in roles" :key="r.role_code || r.roleCode" :label="r.role_name || r.roleName" :value="r.role_code || r.roleCode" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属机构">
              <el-select v-model="form.orgId" style="width: 100%">
                <el-option v-for="o in orgs" :key="o.id" :label="o.org_name || o.orgName" :value="o.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据范围">
              <el-select v-model="form.dataScope" style="width: 100%">
                <el-option label="全部数据" value="ALL" />
                <el-option label="本机构" value="ORG" />
                <el-option label="仅本人" value="SELF" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="!form.id">
            <el-form-item label="初始密码"><el-input v-model="form.password" placeholder="默认 123456" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="头像"><UploadField v-model="form.avatar" /></el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import UploadField from '../components/UploadField.vue'
import { systemApi } from '../api'
import { useTable } from '../composables/useTable'
import { fmtMinute } from '../utils/format'

const roles = ref([])
const orgs = ref([])
const dlg = ref(false)
const saving = ref(false)
const form = ref({})

const { rows, total, loading, page, size, query, load, search, reset, onPage, onSize } = useTable(
  (p) => systemApi.users(p),
  { keyword: '', roleCode: '' }
)

const scopeName = (s) => ({ ALL: '全部数据', ORG: '本机构', SELF: '仅本人' }[s] || '仅本人')

function openCreate() {
  form.value = {
    id: null, username: '', realName: '', phone: '', email: '', roleCode: roles.value[0] && (roles.value[0].role_code || roles.value[0].roleCode),
    orgId: orgs.value[0] && orgs.value[0].id, dataScope: 'SELF', status: 1, password: '123456', avatar: ''
  }
  dlg.value = true
}

function openEdit(row) {
  form.value = {
    id: row.id, username: row.username, realName: row.realName, phone: row.phone, email: row.email,
    roleCode: row.roleCode, orgId: row.orgId, dataScope: row.dataScope, status: row.status, avatar: row.avatar
  }
  dlg.value = true
}

async function save() {
  if (!form.value.username || !form.value.realName) return ElMessage.warning('请填写账号与姓名')
  saving.value = true
  try {
    const { id, ...payload } = form.value
    if (id) await systemApi.updateUser(id, payload)
    else await systemApi.createUser(payload)
    ElMessage.success('已保存')
    dlg.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function resetPwd(row) {
  const r = await ElMessageBox.prompt(`为 ${row.realName} 设置新密码`, '重置密码', {
    inputValue: '123456',
    inputValidator: (v) => (v && v.length >= 6) || '密码至少 6 位'
  })
  await systemApi.resetPwd(row.id, r.value)
  ElMessage.success('密码已重置')
}

onMounted(async () => {
  const [r, o] = await Promise.all([systemApi.roles(), systemApi.orgs()])
  roles.value = r || []
  orgs.value = o || []
  load()
})
</script>
