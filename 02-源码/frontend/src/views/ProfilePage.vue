<template>
  <div class="page">
    <div class="page-head">
      <h2>个人中心<span class="sub">资料、头像与密码</span></h2>
    </div>

    <div class="detail-grid">
      <div class="col" style="gap: 12px">
        <div class="card">
          <div class="sec-t">基本资料</div>
          <el-form :model="form" label-width="92px">
            <el-form-item label="登录账号">
              <el-input :model-value="user.username" disabled />
            </el-form-item>
            <el-form-item label="真实姓名">
              <el-input v-model="form.realName" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="form.phone" maxlength="11" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="form.email" />
            </el-form-item>
            <el-form-item label="所属机构">
              <el-input :model-value="user.orgName" disabled />
            </el-form-item>
            <el-form-item label="头像">
              <UploadField v-model="form.avatar" />
            </el-form-item>
            <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
          </el-form>
        </div>

        <div class="card">
          <div class="sec-t">修改密码</div>
          <el-form :model="pwd" label-width="92px">
            <el-form-item label="原密码">
              <el-input v-model="pwd.oldPassword" type="password" show-password style="width: 320px" />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="pwd.newPassword" type="password" show-password style="width: 320px" />
            </el-form-item>
            <el-form-item label="确认新密码">
              <el-input v-model="pwd.confirm" type="password" show-password style="width: 320px" />
            </el-form-item>
            <el-button type="primary" :loading="changing" @click="changePwd">修改密码</el-button>
          </el-form>
        </div>
      </div>

      <div class="col" style="gap: 12px">
        <div class="card profile-card">
          <el-avatar :size="78" :src="form.avatar || undefined">
            {{ (form.realName || '游').slice(0, 1) }}
          </el-avatar>
          <div class="pn">{{ form.realName || user.username }}</div>
          <el-tag size="small" effect="plain">{{ roleName }}</el-tag>
          <dl class="kv" style="margin-top: 16px; width: 100%">
            <dt>数据范围</dt><dd>{{ scopeName(user.dataScope) }}</dd>
            <dt>所属机构</dt><dd>{{ user.orgName || '-' }}</dd>
            <dt>上次登录</dt><dd>{{ fmtMinute(user.lastLoginTime) }}</dd>
          </dl>
        </div>
        <div class="card">
          <div class="sec-t">我的权限范围</div>
          <div class="muted" style="line-height: 1.9">
            菜单由「角色—菜单」关系动态下发，当前角色为
            <b>{{ roleName }}</b>，可见菜单 {{ menus.length }} 项。
            如需调整权限，请联系系统管理员在「角色菜单」中分配。
          </div>
          <div class="perm">
            <el-tag v-for="m in flatMenus" :key="m.id" size="small" effect="plain" style="margin: 3px 4px 0 0">
              {{ m.name }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import UploadField from '../components/UploadField.vue'
import { authApi, fileApi } from '../api'
import { useUserStore } from '../store/user'
import { fmtMinute } from '../utils/format'

const userStore = useUserStore()
const saving = ref(false)
const changing = ref(false)
const form = ref({ realName: '', phone: '', email: '', avatar: '' })
const pwd = ref({ oldPassword: '', newPassword: '', confirm: '' })

const user = computed(() => userStore.user || {})
const roleName = computed(() => userStore.roleName)
const menus = computed(() => userStore.menus || [])
const flatMenus = computed(() => {
  const out = []
  const walk = (ms) => ms.forEach((m) => { if (m.path) out.push(m); walk(m.children || []) })
  walk(menus.value)
  return out
})

const scopeName = (s) => ({ ALL: '全部数据', ORG: '本机构数据', SELF: '仅本人数据' }[s] || '仅本人数据')

async function saveProfile() {
  saving.value = true
  try {
    const r = await authApi.updateProfile({ ...form.value })
    // 头像与姓名同步到右上角
    userStore.patchUser({ ...form.value, ...(r && r.user ? r.user : {}) })
    ElMessage.success('资料已更新')
  } finally {
    saving.value = false
  }
}

async function changePwd() {
  if (!pwd.value.oldPassword || !pwd.value.newPassword) return ElMessage.warning('请填写原密码与新密码')
  if (pwd.value.newPassword.length < 6) return ElMessage.warning('新密码至少 6 位')
  if (pwd.value.newPassword !== pwd.value.confirm) return ElMessage.warning('两次输入的新密码不一致')
  changing.value = true
  try {
    await authApi.changePassword({ oldPassword: pwd.value.oldPassword, newPassword: pwd.value.newPassword })
    ElMessage.success('密码已修改')
    pwd.value = { oldPassword: '', newPassword: '', confirm: '' }
  } finally {
    changing.value = false
  }
}

onMounted(async () => {
  try {
    const me = await userStore.fetchMe()
    form.value = { realName: me.realName || '', phone: me.phone || '', email: me.email || '', avatar: me.avatar || '' }
  } catch (e) {
    form.value = { realName: user.value.realName || '', phone: user.value.phone || '', email: user.value.email || '', avatar: user.value.avatar || '' }
  }
})
</script>

<style scoped>
.sec-t { font-size: 13px; font-weight: 600; color: var(--text-2); margin-bottom: 12px; padding-left: 8px; border-left: 3px solid var(--secondary); }
.profile-card { display: flex; flex-direction: column; align-items: center; text-align: center; }
.pn { font-size: 16px; font-weight: 600; margin: 10px 0 6px; }
.perm { display: flex; flex-wrap: wrap; margin-top: 10px; }
</style>
