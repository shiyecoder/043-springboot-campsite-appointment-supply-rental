<template>
  <div class="page">
    <div class="page-head">
      <h2>消息中心<span class="sub">未读 {{ unread }} 条</span></h2>
      <div class="ops">
        <el-button @click="readAll">全部标为已读</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-radio-group v-model="filter" @change="() => {}">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="unread">未读</el-radio-button>
        <el-radio-button value="read">已读</el-radio-button>
      </el-radio-group>
      <span class="grow"></span>
      <span class="muted">状态变化与整改提醒由系统自动推送</span>
    </div>

    <div class="card msg-list">
      <div v-for="m in shown" :key="m.id" class="msg" :class="{ unread: m.isRead === 2 }" @click="open(m)">
        <div class="dot" v-if="m.isRead === 2"></div>
        <div class="body">
          <div class="row1">
            <span class="title">{{ m.title }}</span>
            <el-tag size="small" effect="plain" :type="m.isRead === 2 ? 'danger' : 'info'">
              {{ m.isRead === 2 ? '未读' : '已读' }}
            </el-tag>
            <span class="muted">{{ fromNow(m.createTime) }}</span>
          </div>
          <div class="content">{{ m.content }}</div>
          <div class="row3 muted">
            <span v-if="m.bizNo" class="mono">{{ m.bizNo }}</span>
            <span v-if="m.linkUrl">· 关联页 {{ m.linkUrl }}</span>
          </div>
        </div>
      </div>
      <div v-if="!shown.length" class="muted" style="padding: 40px; text-align: center">暂无消息</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { systemApi } from '../api'
import { fromNow } from '../utils/format'

const router = useRouter()
const list = ref([])
const unread = ref(0)
const filter = ref('all')

const shown = computed(() => {
  if (filter.value === 'unread') return list.value.filter((m) => m.isRead === 2)
  if (filter.value === 'read') return list.value.filter((m) => m.isRead !== 2)
  return list.value
})

async function load() {
  list.value = (await systemApi.messages()) || []
  const u = await systemApi.unread()
  unread.value = (u && u.count) || 0
}

async function open(m) {
  if (m.isRead === 2) {
    await systemApi.readOne(m.id)
    await load()
  }
  // 关联页存在且路由已注册时跳转
  if (m.linkUrl && m.linkUrl.startsWith('/')) router.push(m.linkUrl).catch(() => {})
}

async function readAll() {
  await systemApi.readAll()
  await load()
}

onMounted(load)
</script>

<style scoped>
.msg-list { padding: 4px 6px; }
.msg { display: flex; gap: 10px; padding: 12px 12px; border-bottom: 1px dashed var(--line); cursor: pointer; }
.msg:hover { background: #f8fafc; }
.msg.unread .title { font-weight: 600; }
.dot { width: 7px; height: 7px; border-radius: 50%; background: #b3443c; margin-top: 7px; flex: 0 0 7px; }
.body { flex: 1; min-width: 0; }
.row1 { display: flex; align-items: center; gap: 8px; }
.row1 .title { font-size: 14px; }
.row1 .muted { margin-left: auto; font-size: 12px; }
.content { font-size: 13px; color: var(--text-2); margin-top: 4px; line-height: 1.7; }
.row3 { font-size: 11px; margin-top: 3px; display: flex; gap: 6px; }
</style>
