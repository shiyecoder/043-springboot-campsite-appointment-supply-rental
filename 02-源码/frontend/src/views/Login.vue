<template>
  <div class="login-page">
    <!-- 左侧品牌区：营地实景 + 品牌语，不使用默认表单风格 -->
    <section class="brand-side">
      <div class="brand-bg"></div>
      <div class="veil"></div>
      <div class="brand-inner">
        <div class="mark">
          <span class="logo">营</span>
          <span class="mark-text">桃源谷露营基地</span>
        </div>
        <h1>营位与物资，<br />一次预约全部就绪</h1>
        <p class="lead">
          营位时段、容纳人数、设施配套与租赁物资库存联合校验，
          前台、仓库与运营经理在同一条链路上协同。
        </p>
        <ul class="points">
          <li><el-icon><Check /></el-icon>实时营位状态与可订日历</li>
          <li><el-icon><Check /></el-icon>物资库存与周转联合可用性评分</li>
          <li><el-icon><Check /></el-icon>入住核验、退营结算全留痕</li>
        </ul>
        <div class="foot">桃源谷露营基地 · 智慧营地管理系统 v1.0</div>
      </div>
    </section>

    <!-- 右侧登录区 -->
    <section class="form-side">
      <div class="form-wrap">
        <div class="wap-head">
          <h2>{{ mode === 'login' ? '登录' : '注册游客账号' }}</h2>
          <p class="muted">{{ mode === 'login' ? '使用营地分配的账号进入系统' : '注册后可直接在线预订营位' }}</p>
        </div>

        <el-form v-if="mode === 'login'" :model="form" :rules="rules" ref="formRef" @submit.prevent>
          <el-form-item prop="username">
            <el-input v-model="form.username" size="large" placeholder="账号" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              show-password
              placeholder="密码"
              :prefix-icon="Lock"
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-button type="primary" size="large" class="submit" :loading="loading" @click="submit">
            登录系统
          </el-button>
        </el-form>

        <el-form v-else :model="reg" :rules="regRules" ref="regRef" @submit.prevent>
          <el-form-item prop="username">
            <el-input v-model="reg.username" size="large" placeholder="设置账号" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="realName">
            <el-input v-model="reg.realName" size="large" placeholder="真实姓名" :prefix-icon="Postcard" />
          </el-form-item>
          <el-form-item prop="phone">
            <el-input v-model="reg.phone" size="large" placeholder="手机号" :prefix-icon="Iphone" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="reg.password" size="large" type="password" show-password placeholder="设置密码" :prefix-icon="Lock" />
          </el-form-item>
          <el-button type="primary" size="large" class="submit" :loading="loading" @click="submitReg">
            注册并登录
          </el-button>
        </el-form>

        <div class="switch">
          <template v-if="mode === 'login'">
            还没有账号？<el-button link type="primary" @click="mode = 'register'">注册游客账号</el-button>
          </template>
          <template v-else>
            已有账号？<el-button link type="primary" @click="mode = 'login'">返回登录</el-button>
          </template>
        </div>

        <div class="hint" v-if="mode === 'login'">
          <div class="hint-t">演示账号（密码 admin123 / 123456）</div>
          <div class="hint-list">
            <span @click="fill('admin', 'admin123')">管理员 admin</span>
            <span @click="fill('manager01', '123456')">运营经理 manager01</span>
            <span @click="fill('front01', '123456')">前台 front01</span>
            <span @click="fill('wh01', '123456')">仓库 wh01</span>
            <span @click="fill('tourist01', '123456')">游客 tourist01</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, Iphone, Lock, Postcard, User } from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'
import { authApi } from '../api'

const router = useRouter()
const userStore = useUserStore()

const mode = ref('login')
const loading = ref(false)
const formRef = ref(null)
const regRef = ref(null)

const form = reactive({ username: '', password: '' })
const reg = reactive({ username: '', realName: '', phone: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const regRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }, { min: 4, message: '账号至少 4 位', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少 6 位', trigger: 'blur' }]
}

function fill(u, p) {
  form.username = u
  form.password = p
}

async function submit() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return
  loading.value = true
  try {
    const data = await userStore.login({ username: form.username, password: form.password })
    ElMessage.success('欢迎回来，' + ((data.user && data.user.realName) || form.username))
    // 游客角色默认进门户，其余角色进看板
    const role = (data.user && data.user.roleCode) || ''
    router.push(role === 'tourist' ? '/portal' : '/dashboard')
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

async function submitReg() {
  const ok = await regRef.value.validate().catch(() => false)
  if (!ok) return
  loading.value = true
  try {
    await authApi.register({ ...reg, roleCode: 'tourist' })
    ElMessage.success('注册成功，正在登录')
    await userStore.login({ username: reg.username, password: reg.password })
    router.push('/portal')
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { display: flex; height: 100vh; background: #eef2f6; }

/* 左侧品牌区 */
.brand-side { position: relative; flex: 1 1 56%; min-width: 480px; overflow: hidden; }
.brand-bg {
  position: absolute; inset: 0;
  background: url('/login-bg.jpg') center / cover no-repeat, #1f2937;
  transform: scale(1.02);
}
.veil {
  position: absolute; inset: 0;
  background: linear-gradient(115deg, rgba(17, 25, 35, .88) 0%, rgba(24, 45, 62, .72) 48%, rgba(47, 107, 138, .34) 100%);
}
.brand-inner {
  position: relative; height: 100%; padding: 54px 58px; color: #fff;
  display: flex; flex-direction: column; justify-content: center; gap: 18px;
}
.mark { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.logo {
  width: 36px; height: 36px; border-radius: 8px; background: #2f6b8a; display: grid; place-items: center;
  font-weight: 700; font-size: 17px; box-shadow: 0 4px 14px rgba(0, 0, 0, .3);
}
.mark-text { font-size: 14px; letter-spacing: 1px; opacity: .92; }
.brand-side h1 { font-size: 34px; line-height: 1.32; margin: 4px 0 6px; font-weight: 600; letter-spacing: .5px; }
.lead { font-size: 14px; line-height: 1.9; color: rgba(255, 255, 255, .78); max-width: 430px; margin: 0; }
.points { list-style: none; padding: 0; margin: 14px 0 0; display: flex; flex-direction: column; gap: 10px; }
.points li { display: flex; align-items: center; gap: 8px; font-size: 13.5px; color: rgba(255, 255, 255, .9); }
.points .el-icon { color: #7fc4a3; font-size: 15px; }
.foot { position: absolute; bottom: 26px; font-size: 12px; color: rgba(255, 255, 255, .5); letter-spacing: .5px; }

/* 右侧表单区 */
.form-side { flex: 0 0 44%; max-width: 540px; background: #fff; display: flex; align-items: center; justify-content: center; }
.form-wrap { width: 100%; max-width: 348px; padding: 24px; }
.wap-head h2 { font-size: 23px; margin: 0 0 6px; font-weight: 600; letter-spacing: .5px; }
.wap-head .muted { margin: 0 0 22px; }
.submit { width: 100%; margin-top: 4px; height: 44px; font-size: 15px; letter-spacing: 2px; }
.switch { margin-top: 16px; font-size: 13px; color: var(--text-3); text-align: center; }

.hint { margin-top: 26px; border-top: 1px dashed var(--line); padding-top: 14px; }
.hint-t { font-size: 12px; color: var(--text-3); margin-bottom: 8px; }
.hint-list { display: flex; flex-wrap: wrap; gap: 6px; }
.hint-list span {
  font-size: 12px; color: var(--secondary); background: #f2f6f9; border: 1px solid #e2ebf1;
  padding: 3px 8px; border-radius: 12px; cursor: pointer; transition: .15s;
}
.hint-list span:hover { background: #e6eff5; }

@media (max-width: 1024px) {
  .brand-side { display: none; }
  .form-side { flex: 1; max-width: none; }
}
</style>
