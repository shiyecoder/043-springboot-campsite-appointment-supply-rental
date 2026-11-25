<template>
  <div class="mobile-wrap">
    <!-- 移动端巡检原型（390 宽），前台/巡检员现场使用 -->
    <div class="phone">
      <div class="status-bar">
        <span>9:41</span>
        <span class="right">巡检端</span>
      </div>

      <div class="m-head">
        <div class="m-title">今日巡检</div>
        <div class="m-sub">{{ today }} · {{ areaName }}</div>
      </div>

      <el-scrollbar class="m-body">
        <div v-for="t in tasks" :key="t.id" class="m-card" @click="open(t)">
          <div class="mc-top">
            <span class="mono">{{ t.task_no }}</span>
            <el-tag size="small" :type="tagType('patrol', t.status)">{{ label('patrolStatus', t.status) }}</el-tag>
          </div>
          <div class="mc-type">{{ label('patrolType', t.task_type) }} · {{ t.area_name }}</div>
          <div class="muted">计划 {{ t.plan_date }} · 巡检人 {{ t.inspector_name }}</div>
          <div v-if="t.problem_desc" class="mc-problem">{{ t.problem_desc }}</div>
        </div>
        <div v-if="!tasks.length" class="muted" style="padding: 30px; text-align: center">今日暂无巡检任务</div>
      </el-scrollbar>

      <div class="m-tabbar">
        <div class="tab active"><el-icon><Checked /></el-icon><span>巡检</span></div>
        <div class="tab"><el-icon><Warning /></el-icon><span>隐患</span></div>
        <div class="tab"><el-icon><User /></el-icon><span>我的</span></div>
      </div>
    </div>

    <div class="side-note">
      <h3>移动巡检端说明</h3>
      <p>现场巡检员在手机上按任务逐项核对，发现问题直接拍照登记并生成隐患单，后台台账实时同步。</p>
      <ul>
        <li>窄屏 390px 布局，卡片化展示，无横向滚动</li>
        <li>拍照走上传组件，落库为本地 /uploads 路径</li>
        <li>提交后任务状态转「已完成」，问题进入隐患台账</li>
      </ul>
    </div>

    <el-dialog v-model="dlg" title="提交巡检" width="440px" destroy-on-close>
      <el-form :model="ff" label-width="80px">
        <el-form-item label="结果">
          <el-radio-group v-model="ff.result">
            <el-radio value="NORMAL">正常</el-radio>
            <el-radio value="PROBLEM">有问题</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="ff.result === 'PROBLEM'" label="等级">
          <el-select v-model="ff.problemLevel" style="width: 140px">
            <el-option label="轻微" value="LOW" />
            <el-option label="一般" value="MID" />
            <el-option label="严重" value="HIGH" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="ff.result === 'PROBLEM'" label="描述">
          <el-input v-model="ff.problemDesc" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="照片"><UploadField v-model="ff.image" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="submit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Checked, User, Warning } from '@element-plus/icons-vue'
import UploadField from '../components/UploadField.vue'
import { patrolApi } from '../api'
import { label, tagType } from '../utils/format'

const today = new Date().toISOString().slice(0, 10)
const tasks = ref([])
const areaName = ref('今日待办')
const dlg = ref(false)
const ff = ref({ id: null, result: 'NORMAL', problemLevel: 'LOW', problemDesc: '', image: '' })

async function load() {
  const r = await patrolApi.page({ page: 1, size: 20 })
  tasks.value = (r.records || []).filter((t) => t.status !== 'DONE').slice(0, 8)
}

function open(t) {
  ff.value = { id: t.id, result: 'NORMAL', problemLevel: 'LOW', problemDesc: '', image: '' }
  dlg.value = true
}

async function submit() {
  await patrolApi.finish({ ...ff.value })
  ElMessage.success('已提交')
  dlg.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.mobile-wrap { display: flex; gap: 40px; padding: 24px 28px; align-items: flex-start; }
.phone {
  width: 390px; height: 780px; background: #fff; border-radius: 26px; border: 1px solid var(--line);
  box-shadow: 0 18px 44px rgba(31, 41, 55, .16); display: flex; flex-direction: column; overflow: hidden;
}
.status-bar { height: 34px; display: flex; align-items: center; justify-content: space-between; padding: 0 18px; font-size: 12px; color: var(--text-2); background: #1f2937; color: #fff; }
.status-bar .right { font-size: 11px; opacity: .8; }
.m-head { padding: 16px 18px 10px; background: #1f2937; color: #fff; }
.m-title { font-size: 19px; font-weight: 600; }
.m-sub { font-size: 12px; color: rgba(255,255,255,.7); margin-top: 3px; }
.m-body { flex: 1; background: #f5f7fa; }
.m-card { background: #fff; margin: 10px 12px; border-radius: 10px; padding: 11px 12px; border: 1px solid var(--line); }
.mc-top { display: flex; align-items: center; justify-content: space-between; }
.mc-type { font-size: 13.5px; font-weight: 600; margin: 6px 0 3px; }
.mc-problem { font-size: 12px; color: #b3443c; background: #fdf3f2; border-radius: 6px; padding: 6px 8px; margin-top: 7px; line-height: 1.6; }
.m-tabbar { height: 56px; display: flex; border-top: 1px solid var(--line); background: #fff; }
.tab { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2px; font-size: 11px; color: var(--text-3); }
.tab.active { color: var(--secondary); }
.side-note { max-width: 420px; }
.side-note h3 { font-size: 16px; margin: 0 0 8px; }
.side-note p { font-size: 13px; color: var(--text-2); line-height: 1.9; margin: 0 0 10px; }
.side-note ul { padding-left: 18px; font-size: 13px; color: var(--text-2); line-height: 2; }
@media (max-width: 900px) { .mobile-wrap { flex-direction: column; } }
</style>
