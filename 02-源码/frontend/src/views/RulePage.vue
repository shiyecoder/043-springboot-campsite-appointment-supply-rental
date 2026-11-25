<template>
  <div class="page">
    <div class="page-head">
      <h2>规则配置<span class="sub">6 因子权重合计必须等于 100%</span></h2>
      <div class="ops">
        <el-button :icon="Opportunity" @click="$router.push('/avail')">去计算</el-button>
      </div>
    </div>

    <div class="detail-grid">
      <div class="col" style="gap: 12px">
        <div class="card">
          <div class="sec-t">因子权重</div>
          <div v-for="f in fields" :key="f.key" class="wrow">
            <div class="wlabel">
              <span>{{ f.name }}</span>
              <span class="muted">{{ f.desc }}</span>
            </div>
            <el-slider v-model="form[f.key]" :min="0" :max="60" :step="1" style="flex: 1" />
            <el-input-number v-model="form[f.key]" :min="0" :max="100" size="small" style="width: 96px" />
            <span class="pct">%</span>
          </div>
          <div class="sum" :class="sumOk ? 'ok' : 'bad'">
            合计：{{ sum }}% <span v-if="!sumOk">（必须等于 100%，当前差 {{ 100 - sum }}%）</span>
          </div>
        </div>

        <div class="card">
          <div class="sec-t">分档阈值</div>
          <el-form label-width="120px">
            <el-form-item label="推荐阈值（HIGH）">
              <el-input-number v-model="form.thresholdHigh" :min="60" :max="100" />
              <span class="muted" style="margin-left: 10px">综合得分 ≥ 该值判为推荐</span>
            </el-form-item>
            <el-form-item label="可接受阈值（MID）">
              <el-input-number v-model="form.thresholdMid" :min="40" :max="99" />
              <span class="muted" style="margin-left: 10px">低于该值判为不推荐</span>
            </el-form-item>
            <el-form-item label="规则名称">
              <el-input v-model="form.ruleName" placeholder="留空自动按版本命名" style="width: 320px" />
            </el-form-item>
          </el-form>
          <el-button type="primary" :disabled="!sumOk" :loading="saving" @click="save">
            保存为新版本
          </el-button>
          <div class="muted" style="margin-top: 8px">
            保存后旧版本自动归档（ARCHIVED），新版本立即生效，历史评分保留其当时的规则版本号。
          </div>
        </div>
      </div>

      <div class="col" style="gap: 12px">
        <div class="card">
          <div class="sec-t">当前生效规则</div>
          <dl class="kv">
            <dt>规则名</dt><dd>{{ active.rule_name }}</dd>
            <dt>版本</dt><dd>v{{ active.version }}</dd>
            <dt>生效时间</dt><dd>{{ fmtMinute(active.effective_time) }}</dd>
            <dt>创建人</dt><dd>{{ active.create_name }}</dd>
          </dl>
        </div>
        <div class="card">
          <div class="sec-t">版本记录</div>
          <div v-for="r in rules" :key="r.id" class="ver-item">
            <div class="flex aic" style="justify-content: space-between">
              <span class="strong">v{{ r.version }}</span>
              <el-tag size="small" :type="r.status === 'ENABLED' ? 'success' : 'info'">
                {{ r.status === 'ENABLED' ? '生效中' : '已归档' }}
              </el-tag>
            </div>
            <div class="muted" style="font-size: 12px">{{ r.rule_name }}</div>
            <div class="muted" style="font-size: 11px">
              权重 {{ r.w_site_time }}/{{ r.w_capacity }}/{{ r.w_facility }}/{{ r.w_stock }}/{{ r.w_turnover }}/{{ r.w_maintain }}
            </div>
            <div class="muted" style="font-size: 11px">生效 {{ fmtMinute(r.effective_time) }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Opportunity } from '@element-plus/icons-vue'
import { availApi } from '../api'
import { fmtMinute } from '../utils/format'

const fields = [
  { key: 'wSiteTime', name: '营位时段', desc: '营位状态与日期可用性，权重最高' },
  { key: 'wCapacity', name: '容纳人数', desc: '超出最大容量直接判 0 分' },
  { key: 'wFacility', name: '设施需求', desc: '电源、水桩、烧烤台等必需项' },
  { key: 'wStock', name: '物资库存', desc: '租赁物资可用库存是否充足' },
  { key: 'wTurnover', name: '物资周转', desc: '清洁消毒周转时长是否与时段冲突' },
  { key: 'wMaintain', name: '维护状态', desc: '营位与物资维护、维修情况' }
]

const form = ref({
  ruleName: '', wSiteTime: 20, wCapacity: 15, wFacility: 15,
  wStock: 25, wTurnover: 15, wMaintain: 10, thresholdHigh: 87, thresholdMid: 80
})
const active = ref({})
const rules = ref([])
const saving = ref(false)

const sum = computed(() => fields.reduce((s, f) => s + Number(form.value[f.key] || 0), 0))
const sumOk = computed(() => sum.value === 100)

async function loadAll() {
  active.value = (await availApi.activeRule()) || {}
  rules.value = (await availApi.rules()) || []
  form.value = {
    ruleName: '',
    wSiteTime: active.value.w_site_time ?? 20,
    wCapacity: active.value.w_capacity ?? 15,
    wFacility: active.value.w_facility ?? 15,
    wStock: active.value.w_stock ?? 25,
    wTurnover: active.value.w_turnover ?? 15,
    wMaintain: active.value.w_maintain ?? 10,
    thresholdHigh: active.value.threshold_high ?? 87,
    thresholdMid: active.value.threshold_mid ?? 80
  }
}

async function save() {
  if (!sumOk.value) return ElMessage.warning('六项权重合计必须等于 100%')
  saving.value = true
  try {
    await availApi.saveRule({ ...form.value })
    ElMessage.success('新版本已生效')
    loadAll()
  } finally {
    saving.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.sec-t { font-size: 13px; font-weight: 600; color: var(--text-2); margin-bottom: 10px; padding-left: 8px; border-left: 3px solid var(--secondary); }
.wrow { display: flex; align-items: center; gap: 14px; padding: 7px 0; border-bottom: 1px dashed var(--line); }
.wlabel { width: 210px; flex: 0 0 210px; font-size: 13px; }
.wlabel span { display: block; }
.wlabel .muted { font-size: 11px; margin-top: 2px; }
.pct { color: var(--text-3); font-size: 12px; }
.sum { margin-top: 12px; font-size: 13px; font-weight: 600; }
.sum.ok { color: #2e7d5b; }
.sum.bad { color: #b3443c; }
.ver-item { padding: 8px 0; border-bottom: 1px dashed var(--line); }
</style>
