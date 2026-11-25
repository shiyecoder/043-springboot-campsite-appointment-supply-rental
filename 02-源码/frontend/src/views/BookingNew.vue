<template>
  <div class="page">
    <div class="page-head">
      <h2>新建预约<span class="sub">营位 + 物资一次下单</span></h2>
      <div class="ops">
        <el-button :icon="Back" @click="$router.push('/booking')">返回列表</el-button>
      </div>
    </div>

    <div class="detail-grid">
      <div class="col" style="gap: 12px">
        <div class="card">
          <div class="sec-t">客户信息</div>
          <el-form :model="form" label-width="92px">
            <el-row :gutter="14">
              <el-col :span="8"><el-form-item label="客户姓名" required><el-input v-model="form.customerName" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="手机号" required><el-input v-model="form.customerPhone" maxlength="11" /></el-form-item></el-col>
              <el-col :span="8">
                <el-form-item label="来源">
                  <el-select v-model="form.source" style="width: 100%">
                    <el-option v-for="(v, k) in DICT.source" :key="k" :label="v" :value="k" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </div>

        <div class="card">
          <div class="sec-t">营位与日期</div>
          <el-form :model="form" label-width="92px">
            <el-row :gutter="14">
              <el-col :span="8">
                <el-form-item label="营位" required>
                  <el-select v-model="form.siteId" filterable style="width: 100%" @change="onSite">
                    <el-option
                      v-for="s in sites"
                      :key="s.id"
                      :label="`${s.site_no} ${s.site_name}（${s.max_capacity}人）`"
                      :value="s.id"
                      :disabled="s.status === 'OFF' || s.status === 'REPAIR'"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="入住日期" required>
                  <el-date-picker v-model="form.checkInDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" @change="calc" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="退营日期" required>
                  <el-date-picker v-model="form.checkOutDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" @change="calc" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="入住人数" required>
                  <el-input-number v-model="form.guestCount" :min="1" :max="maxGuests" style="width: 100%" @change="calc" />
                </el-form-item>
              </el-col>
              <el-col :span="16">
                <el-form-item label="特殊需求">
                  <el-input v-model="form.specialNeed" placeholder="如：需靠近洗手间、带宠物" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </div>

        <div class="card">
          <div class="sec-t">租赁物资</div>
          <el-table :data="cart" size="small">
            <el-table-column label="物资" min-width="160">
              <template #default="{ row }">
                <div class="flex aic gap8">
                  <img class="thumb-sm" :src="row.image" style="width: 30px; height: 30px; border-radius: 4px" />
                  <span>{{ row.goods_name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="单价" width="110" align="right">
              <template #default="{ row }">{{ fmtMoney(row.rent_price) }}/{{ row.rent_unit }}</template>
            </el-table-column>
            <el-table-column label="数量" width="130" align="center">
              <template #default="{ row }">
                <el-input-number v-model="row.qty" :min="1" :max="row.total_stock" size="small" style="width: 100px" @change="calc" />
              </template>
            </el-table-column>
            <el-table-column label="小计" width="100" align="right">
              <template #default="{ row }">{{ fmtMoney(row.rent_price * row.qty * nights) }}</template>
            </el-table-column>
            <el-table-column label="" width="70">
              <template #default="{ $index }">
                <el-button link type="danger" size="small" @click="cart.splice($index, 1)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="flex gap8 aic" style="margin-top: 10px">
            <el-select v-model="pickGoodsId" filterable placeholder="选择要租赁的物资" style="width: 260px">
              <el-option v-for="g in goods" :key="g.id" :label="`${g.goods_name}（库存 ${g.total_stock}）`" :value="g.id" />
            </el-select>
            <el-button :icon="Plus" @click="addGoods">加入</el-button>
            <span class="muted">库存不足时后端会拒绝下单</span>
          </div>
        </div>

        <div class="card">
          <div class="sec-t">备注</div>
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="到店时间、随行安排等" />
        </div>
      </div>

      <!-- 右侧费用与提交 -->
      <div class="col" style="gap: 12px">
        <div class="card">
          <div class="sec-t">费用估算</div>
          <dl class="kv">
            <dt>营位</dt><dd>{{ curSite ? curSite.site_no + ' ' + curSite.site_name : '未选择' }}</dd>
            <dt>单价</dt><dd>{{ curSite ? fmtMoney(curSite.base_price) + ' / 晚' : '-' }}</dd>
            <dt>晚数</dt><dd>{{ nights }} 晚</dd>
            <dt>营位费</dt><dd class="money">{{ fmtMoney(siteFee) }}</dd>
            <dt>物资费</dt><dd class="money">{{ fmtMoney(goodsFee) }}</dd>
            <dt>押金</dt><dd>{{ fmtMoney(deposit) }}</dd>
            <dt>合计</dt><dd class="money" style="font-size: 16px">{{ fmtMoney(siteFee + goodsFee) }}</dd>
          </dl>
          <el-button type="primary" style="width: 100%; margin-top: 14px" :loading="saving" @click="submit">
            提交预约
          </el-button>
          <div class="muted" style="margin-top: 8px; line-height: 1.6">
            提交后状态为「待确认」，需前台确认后预留营位；入住前须完成物资备货与发放。
          </div>
        </div>
        <div class="card" v-if="curSite">
          <div class="sec-t">营位信息</div>
          <img class="thumb-lg" :src="curSite.detail_image || curSite.cover_image" :alt="curSite.site_name" />
          <dl class="kv" style="margin-top: 10px">
            <dt>营区</dt><dd>{{ curSite.area_name }}</dd>
            <dt>类型</dt><dd>{{ curSite.type_name }}</dd>
            <dt>人数</dt><dd>{{ curSite.std_capacity }} 标准 / {{ curSite.max_capacity }} 最多</dd>
            <dt>设施</dt><dd>{{ curSite.facilities }}</dd>
            <dt>状态</dt><dd>{{ label('siteStatus', curSite.status) }}</dd>
          </dl>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Plus } from '@element-plus/icons-vue'
import { bookingApi, siteApi } from '../api'
import { DICT, fmtMoney, label } from '../utils/format'

const router = useRouter()
const sites = ref([])
const goods = ref([])
const cart = ref([])
const pickGoodsId = ref(null)
const saving = ref(false)

const form = ref({
  customerName: '', customerPhone: '', source: 'WEB', siteId: null,
  checkInDate: '', checkOutDate: '', guestCount: 2, specialNeed: '', remark: ''
})

const curSite = computed(() => sites.value.find((s) => s.id === form.value.siteId) || null)
const maxGuests = computed(() => (curSite.value ? curSite.value.max_capacity : 12))

const nights = computed(() => {
  const { checkInDate: a, checkOutDate: b } = form.value
  if (!a || !b) return 1
  const n = (new Date(b.replace(/-/g, '/')) - new Date(a.replace(/-/g, '/'))) / 86400000
  return n > 0 ? n : 1
})

const siteFee = computed(() => (curSite.value ? curSite.value.base_price * nights.value : 0))
const goodsFee = computed(() => cart.value.reduce((s, r) => s + r.rent_price * r.qty * nights.value, 0))
const deposit = computed(() => cart.value.reduce((s, r) => s + r.deposit * r.qty, 0) + 200)

function onSite() {
  if (form.value.guestCount > maxGuests.value) form.value.guestCount = maxGuests.value
  calc()
}

function calc() {}

function addGoods() {
  if (!pickGoodsId.value) return
  const g = goods.value.find((x) => x.id === pickGoodsId.value)
  if (!g) return
  if (cart.value.some((c) => c.id === g.id)) return ElMessage.info('该物资已在清单中')
  cart.value.push({ ...g, qty: 1 })
  pickGoodsId.value = null
}

async function submit() {
  const f = form.value
  if (!f.customerName || !f.customerPhone) return ElMessage.warning('请填写客户姓名与手机号')
  if (!f.siteId || !f.checkInDate || !f.checkOutDate) return ElMessage.warning('请选择营位与日期')
  if (!/^1\d{10}$/.test(f.customerPhone)) return ElMessage.warning('手机号格式不正确')
  saving.value = true
  try {
    const id = await bookingApi.create({
      ...f,
      rentItems: cart.value.map((c) => ({ goodsId: c.id, qty: c.qty }))
    })
    ElMessage.success('预约已提交，编号 #' + id)
    router.push('/booking')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const opt = await siteApi.options()
  goods.value = opt.goods || []
  const page = await siteApi.page({ page: 1, size: 200 })
  sites.value = page.records || []
  const today = new Date()
  const fmt = (d) => d.toISOString().slice(0, 10)
  form.value.checkInDate = fmt(today)
  form.value.checkOutDate = fmt(new Date(today.getTime() + 86400000 * 2))
})
</script>

<style scoped>
.sec-t { font-size: 13px; font-weight: 600; color: var(--text-2); margin-bottom: 10px; padding-left: 8px; border-left: 3px solid var(--secondary); }
</style>
