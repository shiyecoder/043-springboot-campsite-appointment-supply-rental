<template>
  <div class="layout" :class="[collapsed ? 'collapsed' : '', 'role-' + (userStore.roleCode || 'admin')]">
    <!-- 左侧菜单 224px（折叠 64px），菜单项来自后端按角色下发 -->
    <aside class="sidebar">
      <div class="brand">
        <div class="logo">营</div>
        <div class="brand-text" v-show="!collapsed">
          <div class="name">桃源谷露营</div>
          <div class="sub">TAOYUAN CAMP</div>
        </div>
      </div>

      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="route.path"
          :collapse="collapsed"
          :collapse-transition="false"
          background-color="transparent"
          text-color="#c3d1dc"
          active-text-color="#ffffff"
          router
        >
          <template v-for="m in menus" :key="m.id">
            <el-sub-menu v-if="m.children && m.children.length" :index="'g' + m.id">
              <template #title>
                <el-icon><component :is="iconOf(m.icon)" /></el-icon>
                <span>{{ m.name }}</span>
              </template>
              <el-menu-item v-for="c in m.children" :key="c.id" :index="c.path">
                <el-icon><component :is="iconOf(c.icon)" /></el-icon>
                <template #title>{{ c.name }}</template>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="m.path">
              <el-icon><component :is="iconOf(m.icon)" /></el-icon>
              <template #title>{{ m.name }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>

      <div class="role-badge" v-show="!collapsed">{{ userStore.roleName }}</div>
    </aside>

    <section class="main">
      <!-- 顶部栏 56px：面包屑 + 消息 + 头像 -->
      <header class="topbar">
        <div class="left">
          <el-icon class="fold" @click="collapsed = !collapsed">
            <component :is="collapsed ? Expand : Fold" />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ groupName }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="right">
          <el-popover placement="bottom-end" :width="350" trigger="click" popper-class="msg-pop">
            <template #reference>
              <div class="bell">
                <!-- unread 接口返回 {count}，不是裸数字，这里必须取 .count 否则角标显示 [object Object] -->
                <el-badge :value="unread" :hidden="!unread" :max="99">
                  <el-icon :size="18"><Bell /></el-icon>
                </el-badge>
              </div>
            </template>
            <div class="msg-head">
              <span>站内消息</span>
              <el-button link type="primary" size="small" @click="readAll">全部已读</el-button>
            </div>
            <div v-if="!messages.length" class="muted" style="padding: 12px 0">暂无消息</div>
            <div v-for="m in messages.slice(0, 5)" :key="m.id" class="msg-item" @click="openMsg(m)">
              <div class="t">
                <el-tag v-if="m.isRead === 2" type="danger" size="small" effect="plain">未读</el-tag>
                <span>{{ m.title }}</span>
              </div>
              <div class="c">{{ m.content }}</div>
              <div class="d">{{ fromNow(m.createTime || m.create_time) }}</div>
            </div>
            <div class="more">
              <el-button link type="primary" size="small" @click="$router.push('/message')">查看全部消息</el-button>
            </div>
          </el-popover>

          <el-dropdown trigger="click" @command="onCommand">
            <div class="user">
              <el-avatar :size="30" :src="userStore.avatar || undefined">
                {{ (userStore.realName || '游').slice(0, 1) }}
              </el-avatar>
              <div class="uinfo">
                <div class="uname">{{ userStore.realName || '未登录' }}</div>
                <div class="urole">{{ userStore.roleName }}</div>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="portal">营地门户</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <div class="content">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Expand, Fold } from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'
import { systemApi } from '../api'
import { fromNow } from '../utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const collapsed = ref(false)
const unread = ref(0)
const messages = ref([])

const menus = computed(() => userStore.menus || [])

/** 面包屑：找到当前路径所属的一级菜单 */
const groupName = computed(() => {
  for (const m of menus.value) {
    if (m.path === route.path) return '桃源谷露营'
    for (const c of m.children || []) {
      if (c.path === route.path) return m.name
    }
  }
  return '桃源谷露营'
})
const pageTitle = computed(() => route.meta.title || '经营看板')

/** 后端下发的是 Element Plus 图标组件名，找不到就退回 Menu 图标 */
const FALLBACK = { Place: 'Location', Opportunity: 'TrendCharts', Money: 'Coin', Goods: 'Box' }
function iconOf(name) {
  return FALLBACK[name] || name || 'Menu'
}

async function loadMsg() {
  try {
    const [u, list] = await Promise.all([systemApi.unread(), systemApi.messages()])
    unread.value = u && typeof u === 'object' ? u.count || 0 : 0
    messages.value = list || []
  } catch (e) {
    // 游客角色可能无消息权限，静默降级
  }
}

async function readAll() {
  await systemApi.readAll()
  loadMsg()
}

async function openMsg(m) {
  if (m.isRead === 2) await systemApi.readOne(m.id)
  loadMsg()
  if (m.linkUrl) router.push(m.linkUrl)
}

function onCommand(cmd) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确认退出登录吗？', '提示', { type: 'warning' })
      .then(() => {
        userStore.logout()
        router.push('/login')
      })
      .catch(() => {})
    return
  }
  if (cmd === 'profile') router.push('/profile')
  if (cmd === 'portal') router.push('/portal')
}

onMounted(async () => {
  if (!userStore.user || !userStore.user.id) {
    try {
      await userStore.fetchMe()
    } catch (e) {
      router.push('/login')
      return
    }
  }
  loadMsg()
})
</script>

<style scoped>
.layout { display: flex; height: 100vh; overflow: hidden; background: var(--bg); }

/* 侧栏底色按角色区分：管理员最深、经理海军蓝、前台偏蓝、仓库偏绿 */
.sidebar {
  width: 224px; flex: 0 0 224px; background: #1f2937; display: flex; flex-direction: column;
  transition: width .18s ease, flex-basis .18s ease; overflow: hidden;
}
.role-manager .sidebar { background: #1f2937; }
.role-front .sidebar { background: #24455c; }
.role-warehouse .sidebar { background: #2a504a; }
.role-admin .sidebar { background: #1b2430; }
.role-tourist .sidebar { background: #2f6b8a; }
.collapsed .sidebar { width: 64px; flex-basis: 64px; }

.brand { height: 56px; display: flex; align-items: center; gap: 10px; padding: 0 14px; border-bottom: 1px solid rgba(255,255,255,.08); }
.logo {
  width: 30px; height: 30px; border-radius: 6px; background: #2f6b8a; color: #fff;
  display: grid; place-items: center; font-weight: 700; font-size: 15px; flex: 0 0 30px;
}
.brand-text .name { color: #fff; font-size: 14px; font-weight: 600; letter-spacing: .3px; white-space: nowrap; }
.brand-text .sub { color: #7f93a5; font-size: 9px; letter-spacing: 1.2px; white-space: nowrap; }

.menu-scroll { flex: 1; }
:deep(.el-menu) { border-right: none; }
:deep(.el-menu-item), :deep(.el-sub-menu__title) { height: 42px; line-height: 42px; font-size: 13.5px; }
:deep(.el-menu-item.is-active) { background: #2f6b8a !important; color: #fff !important; border-radius: 6px; margin: 0 8px; }
:deep(.el-menu--collapse .el-menu-item) { margin: 0 4px; }
:deep(.el-sub-menu .el-menu-item) { padding-left: 42px !important; }
:deep(.el-menu-item:hover), :deep(.el-sub-menu__title:hover) { background: rgba(255,255,255,.06) !important; }

.role-badge {
  margin: 8px 12px 12px; padding: 6px 10px; border-radius: 6px; font-size: 12px;
  color: #c3d1dc; background: rgba(255,255,255,.06); text-align: center; white-space: nowrap;
}

.main { flex: 1; min-width: 0; display: flex; flex-direction: column; }

.topbar {
  height: 56px; flex: 0 0 56px; background: #fff; border-bottom: 1px solid var(--line);
  display: flex; align-items: center; justify-content: space-between; padding: 0 20px;
}
.topbar .left { display: flex; align-items: center; gap: 14px; }
.fold { cursor: pointer; color: var(--text-2); font-size: 17px; }
.topbar .right { display: flex; align-items: center; gap: 20px; }
.bell { cursor: pointer; color: var(--text-2); display: flex; align-items: center; }

.user { display: flex; align-items: center; gap: 8px; cursor: pointer; outline: none; }
.uinfo { line-height: 1.2; }
.uname { font-size: 13px; color: var(--text); font-weight: 500; }
.urole { font-size: 11px; color: var(--text-2); }

.content { flex: 1; overflow-y: auto; background: var(--bg); }

.msg-head { display: flex; justify-content: space-between; align-items: center; font-weight: 600; margin-bottom: 6px; }
.msg-item { padding: 8px 0; border-bottom: 1px dashed var(--line); cursor: pointer; }
.msg-item .t { font-size: 13px; display: flex; gap: 6px; align-items: center; }
.msg-item .c { font-size: 12px; color: var(--text-2); margin-top: 3px; line-height: 1.5; }
.msg-item .d { font-size: 11px; color: var(--text-3); margin-top: 2px; }
.more { margin-top: 8px; text-align: right; }

@media (max-width: 768px) {
  .sidebar { width: 64px; flex-basis: 64px; }
  .brand-text, .role-badge { display: none; }
  .topbar { padding: 0 12px; }
  .uinfo { display: none; }
}
</style>
