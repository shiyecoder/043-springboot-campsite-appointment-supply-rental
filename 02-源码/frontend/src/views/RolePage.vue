<template>
  <div class="page">
    <div class="page-head">
      <h2>角色与菜单<span class="sub">为角色分配可见菜单，登录后按角色动态下发</span></h2>
      <div class="ops">
        <el-button :icon="Refresh" @click="loadTree">刷新菜单树</el-button>
      </div>
    </div>

    <div class="detail-grid">
      <div class="card">
        <div class="sec-t">角色列表</div>
        <div
          v-for="r in roles"
          :key="r.role_code"
          class="role-item"
          :class="{ active: curRole === r.role_code }"
          @click="selectRole(r)"
        >
          <div class="ri-top">
            <span class="ri-name">{{ r.role_name }}</span>
            <el-tag size="small" effect="plain">{{ r.role_code }}</el-tag>
          </div>
          <div class="muted" style="font-size: 12px">{{ r.description || '—' }}</div>
        </div>
      </div>

      <div class="card">
        <div class="sec-t">
          菜单分配
          <span class="muted" style="font-weight: 400; margin-left: 8px">
            当前角色：{{ curRoleName }}
          </span>
        </div>
        <div class="muted" style="margin-bottom: 10px; font-size: 12px">
          「菜单管理」本身不下发给任何角色，避免越权修改权限结构。
        </div>
        <el-tree
          ref="treeRef"
          :data="tree"
          show-checkbox
          node-key="id"
          :props="{ label: 'label', children: 'children' }"
          default-expand-all
          style="max-height: 460px; overflow: auto"
        />
        <div class="flex gap8" style="margin-top: 14px">
          <el-button type="primary" :disabled="!curRole" :loading="saving" @click="assign">保存分配</el-button>
          <el-button :disabled="!curRole" @click="loadRoleMenus">恢复原配置</el-button>
          <span class="muted" style="margin-left: auto; align-self: center">
            已选 {{ checkedCount }} 项
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { systemApi } from '../api'

const roles = ref([])
const tree = ref([])
const curRole = ref('')
const curRoleName = ref('')
const treeRef = ref(null)
const saving = ref(false)
const checkedCount = ref(0)

/** 过滤掉菜单管理节点：不在任何角色的分配树中出现 */
function strip(ms) {
  return (ms || [])
    .filter((m) => !/菜单管理|分配菜单/.test(m.label || ''))
    .map((m) => ({ ...m, children: strip(m.children) }))
}

async function loadTree() {
  const t = await systemApi.menuTree()
  tree.value = strip(t)
}

async function loadRoleMenus() {
  if (!curRole.value) return
  const ids = (await systemApi.roleMenus(curRole.value)) || []
  treeRef.value && treeRef.value.setCheckedKeys(ids, false)
  countChecked()
}

function countChecked() {
  checkedCount.value = treeRef.value ? treeRef.value.getCheckedKeys().length : 0
}

function selectRole(r) {
  curRole.value = r.role_code
  curRoleName.value = r.role_name
  loadRoleMenus()
}

async function assign() {
  const ids = treeRef.value.getCheckedKeys().concat(treeRef.value.getHalfCheckedKeys())
  saving.value = true
  try {
    await systemApi.assignMenus(curRole.value, ids)
    ElMessage.success('已保存，相关角色下次登录生效')
    countChecked()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadTree()
  roles.value = (await systemApi.roles()) || []
  if (roles.value.length) selectRole(roles.value[0])
})
</script>

<style scoped>
.sec-t { font-size: 13px; font-weight: 600; color: var(--text-2); margin-bottom: 12px; padding-left: 8px; border-left: 3px solid var(--secondary); }
.role-item { padding: 9px 10px; border-radius: 7px; cursor: pointer; border: 1px solid transparent; margin-bottom: 6px; }
.role-item:hover { background: #f6f9fb; }
.role-item.active { background: #eef4f8; border-color: #cfe0ea; }
.ri-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 2px; }
.ri-name { font-size: 13.5px; font-weight: 600; }
</style>
