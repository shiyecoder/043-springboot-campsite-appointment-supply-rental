<template>
  <div class="page">
    <div class="page-head">
      <h2>
        营位与物资联合可用性
        <span class="sub">6 因子加权 · 规则 v{{ rule.version || '-' }}</span>
      </h2>
      <div class="ops">
        <el-button :icon="Setting" @click="$router.push('/rule')">规则配置</el-button>
      </div>
    </div>

    <div class="detail-grid">
      <div class="col" style="gap: 12px">
        <!-- 计算输入 -->
        <div class="card">
          <div class="sec-t">计算输入</div>
          <el-form :model="q" label-width="92px">
            <el-row :gutter="14">
              <el-col :span="12">
                <el-form-item label="营位" required>
                  <el-select v-model="q.siteId" filterable style="width: 100%">
                    <el-option
                      v-for="s in sites"
                      :key="s.id"
                      :label="`${s.site_no} ${s.site_name}（最多 ${s.max_capacity} 人）`"
                      :value="s.id"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="入住人数">
                  <el-input-number v-model="q.guestCount" :min="1" :max="20" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="入住日期">
                  <el-date-picker v-model="q.checkInDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="退营日期">
                  <el-date-picker v-model="q.checkOutDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="租赁物资">
                  <el-select v-model="q.goodsIds" multiple filterable collapse-tags placeholder="选择要一起租赁的物资" style="width: 100%">
                    <el-option v-for="g in goods" :key="g.id" :label="g.goods_name" :value="g.id" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-button type="primary" :loading="calculating" @click="evaluate">执行联合可用性计算</el-button>
            <span class="muted" style="margin-left: 10px">结果会留痕保存，可人工修正</span>
          </el-form>
        </div>

        <!-- 结果：总分 + 六因子明细 -->
        <div class="card" v-if="result">
          <div class="res-head">
            <div>
              <div class="muted">综合得分</div>
              <div class="score" :class="'lv-' + result.level">{{ result.total_score }}<span class="muted"> / 100</span></div>
            </div>
            <el-tag :type="tagType('avail', result.level)" effect="dark" size="large">
              {{ label('availLevel', result.level) }}
            </el-tag>
          </div>

          <div class="factor-list">
            <div v-for="f in factors" :key="f.key" class="factor">
              <div class="f-top">
                <span>{{ f.name }}<i class="muted"> 权重 {{ f.weight }}%</i></span>
                <b>{{ f.score }} 分</b>
              </div>
              <div class="bar"><i :style="{ width: f.score + '%', background: barColor(f.score) }"></i></div>
              <div class="muted" style="font-size: 12px">{{ f.note }}</div>
            </div>
          </div>

          <el-alert
            v-if="result.level === 'LOW'"
            type="error"
            :closable="false"
            show-icon
            style="margin-top: 12px"
            :title="'硬性条件不满足：' + (result.unmet || '见因子明细')"
          />
          <el-alert
            v-else
            :type="result.level === 'HIGH' ? 'success' : 'warning'"
            :closable="false"
            show-icon
            style="margin-top: 12px"
            :title="result.main_factor"
            :description="'改进建议：' + (result.improve_tip || '无')"
          />

          <div class="suggest" v-if="suggestions.length">
            <div class="sec-t" style="margin-top: 14px">可替代营位推荐</div>
            <div v-for="s in suggestions" :key="s.siteNo" class="sg-item">
              <span class="mono">{{ s.siteNo }}</span>
              <span>{{ s.siteName }}</span>
              <span class="muted">价差 {{ fmtMoney(s.priceDiff) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 历史评分与人工修正 -->
      <div class="col" style="gap: 12px">
        <div class="card" v-if="rule.id">
          <div class="sec-t">当前规则</div>
          <div class="strong" style="font-size: 13px">{{ rule.rule_name }}</div>
          <dl class="kv" style="margin-top: 8px">
            <dt>营位时段</dt><dd>{{ rule.w_site_time }}%</dd>
            <dt>容纳人数</dt><dd>{{ rule.w_capacity }}%</dd>
            <dt>设施需求</dt><dd>{{ rule.w_facility }}%</dd>
            <dt>物资库存</dt><dd>{{ rule.w_stock }}%</dd>
            <dt>物资周转</dt><dd>{{ rule.w_turnover }}%</dd>
            <dt>维护状态</dt><dd>{{ rule.w_maintain }}%</dd>
            <dt>推荐阈值</dt><dd>≥ {{ rule.threshold_high }} 分</dd>
            <dt>可接受阈值</dt><dd>≥ {{ rule.threshold_mid }} 分</dd>
          </dl>
        </div>
        <div class="card">
          <div class="sec-t">评分留痕（最近）</div>
          <div class="hist">
            <div v-for="h in history" :key="h.id" class="hist-item" @click="openManual(h)">
              <div class="flex aic" style="justify-content: space-between">
                <span class="mono">{{ h.booking_id ? 'BK#' + h.booking_id : '手动' }}</span>
                <el-tag size="small" :type="tagType('avail', h.level)">{{ label('availLevel', h.level) }}</el-tag>
              </div>
              <div class="h-score">{{ h.total_score }} 分<span class="muted"> · 规则 v{{ h.rule_version }}</span></div>
              <div class="muted" style="font-size: 11px">{{ h.main_factor }}</div>
            </div>
            <div v-if="!history.length" class="muted" style="padding: 12px 0">暂无评分记录</div>
          </div>
          <div class="pager" style="padding: 8px 0 0">
            <el-pagination
              small
              layout="prev, pager, next"
              :total="histTotal"
              :page-size="6"
              :current-page="histPage"
              @current-change="(p) => { histPage = p; loadHistory() }"
            />
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dlg" title="人工修正评分" width="480px" destroy-on-close>
      <el-form :model="mf" label-width="92px">
        <el-form-item label="原结果">
          <span>{{ cur && cur.total_score }} 分 · {{ label('availLevel', cur && cur.level) }}</span>
        </el-form-item>
        <el-form-item label="修正为">
          <el-radio-group v-model="mf.level">
            <el-radio value="HIGH">推荐</el-radio>
            <el-radio value="MID">可接受</el-radio>
            <el-radio value="LOW">不推荐</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="修正原因">
          <el-input v-model="mf.reason" type="textarea" :rows="3" placeholder="如：客户为常客，现场确认有额外电源可满足需求" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="saveManual">保存修正</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Setting } from '@element-plus/icons-vue'
import { availApi, siteApi } from '../api'
import { fmtMoney, label, tagType } from '../utils/format'

const sites = ref([])
const goods = ref([])
const rule = ref({})
const result = ref(null)
const calculating = ref(false)
const history = ref([])
const histTotal = ref(0)
const histPage = ref(1)
const dlg = ref(false)
const cur = ref(null)
const mf = ref({ level: 'MID', reason: '' })

const today = new Date().toISOString().slice(0, 10)
const q = ref({ siteId: null, guestCount: 2, checkInDate: today, checkOutDate: today, goodsIds: [] })

const FACTOR_META = [
  { key: 's_site_time', wk: 'w_site_time', name: '营位时段可用性' },
  { key: 's_capacity', wk: 'w_capacity', name: '容纳人数匹配' },
  { key: 's_facility', wk: 'w_facility', name: '设施需求满足' },
  { key: 's_stock', wk: 'w_stock', name: '物资库存充足' },
  { key: 's_turnover', wk: 'w_turnover', name: '物资周转可行' },
  { key: 's_maintain', wk: 'w_maintain', name: '维护状态' }
]

const factors = computed(() => {
  if (!result.value) return []
  return FACTOR_META.map((f) => ({
    key: f.key,
    name: f.name,
    weight: Number(rule.value[f.wk] || 0),
    score: Number(result.value[f.key] || 0),
    note: noteOf(f.key, result.value)
  }))
})

function noteOf(key, r) {
  const s = Number(r[key] || 0)
  if (s >= 95) return '完全满足，无制约'
  if (s >= 80) return '基本满足，影响很小'
  if (s >= 60) return '略有制约，仍在可接受范围'
  return '明显制约，建议调整营位或日期'
}

const barColor = (s) => (s >= 87 ? '#2e7d5b' : s >= 60 ? '#b07b2b' : '#b3443c')

const suggestions = computed(() => {
  if (!result.value || !result.value.suggest_json) return []
  try {
    return JSON.parse(result.value.suggest_json) || []
  } catch (e) {
    return []
  }
})

async function evaluate() {
  if (!q.value.siteId) return ElMessage.warning('请选择营位')
  calculating.value = true
  try {
    const r = await availApi.evaluate({ ...q.value })
    result.value = r
    ElMessage.success('计算完成，已保存评分留痕')
    loadHistory()
  } finally {
    calculating.value = false
  }
}

async function loadHistory() {
  const r = await availApi.page({ page: histPage.value, size: 6 })
  history.value = r.records || []
  histTotal.value = r.total || 0
}

function openManual(h) {
  cur.value = h
  mf.value = { level: h.level, reason: '' }
  dlg.value = true
}

async function saveManual() {
  if (!mf.value.reason || mf.value.reason.trim().length < 5) return ElMessage.warning('请填写不少于 5 个字的修正原因')
  await availApi.manual(cur.value.id, mf.value.level, mf.value.reason)
  ElMessage.success('已保存人工修正')
  dlg.value = false
  loadHistory()
}

onMounted(async () => {
  const opt = await siteApi.options()
  goods.value = opt.goods || []
  const page = await siteApi.page({ page: 1, size: 200 })
  sites.value = page.records || []
  rule.value = (await availApi.activeRule()) || {}
  loadHistory()
})
</script>

<style scoped>
.sec-t { font-size: 13px; font-weight: 600; color: var(--text-2); margin-bottom: 10px; padding-left: 8px; border-left: 3px solid var(--secondary); }
.res-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.score { font-size: 34px; font-weight: 700; letter-spacing: -1px; line-height: 1.1; }
.lv-HIGH { color: #2e7d5b; }
.lv-MID { color: #b07b2b; }
.lv-LOW { color: #b3443c; }
.factor-list { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px 22px; }
.factor { font-size: 13px; }
.f-top { display: flex; justify-content: space-between; margin-bottom: 5px; }
.f-top i { font-style: normal; font-size: 11px; }
.factor .bar { margin-bottom: 4px; }
.sg-item { display: flex; gap: 10px; align-items: center; font-size: 13px; padding: 6px 0; border-bottom: 1px dashed var(--line); }
.hist { max-height: 330px; overflow-y: auto; }
.hist-item { padding: 7px 8px; border-radius: 6px; cursor: pointer; border-bottom: 1px dashed var(--line); }
.hist-item:hover { background: #f6f9fb; }
.h-score { font-size: 13px; font-weight: 600; margin: 2px 0; }
</style>
