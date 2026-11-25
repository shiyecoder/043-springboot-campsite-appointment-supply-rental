<template>
  <div class="portal">
    <!-- 门户头：营地实景大图 + 检索 -->
    <header class="hero">
      <div class="hero-bg"></div>
      <div class="hero-veil"></div>
      <div class="hero-inner">
        <div class="mark">
          <span class="logo">营</span>
          <span>桃源谷露营基地</span>
        </div>
        <h1>选一处营位，把周末交给山野</h1>
        <p class="sub">湖畔、林间、亲子、山脊四类营区，营位与租赁物资可一起预约</p>
        <div class="search">
          <el-select v-model="pick.areaId" placeholder="选择营区" clearable style="width: 180px">
            <el-option v-for="a in areas" :key="a.id" :label="a.area_name" :value="a.id" />
          </el-select>
          <el-select v-model="pick.typeId" placeholder="选择类型" clearable style="width: 180px">
            <el-option v-for="t in types" :key="t.id" :label="t.type_name" :value="t.id" />
          </el-select>
          <el-date-picker v-model="pick.range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="入住" end-placeholder="退营" style="width: 260px" />
          <el-button type="primary" @click="goBook">立即预订</el-button>
        </div>
      </div>
    </header>

    <main class="wrap">
      <section class="sec">
        <div class="sec-head">
          <h3>营区一览</h3>
          <span class="muted">四个营区，四种过夜方式</span>
        </div>
        <div class="area-grid">
          <div v-for="a in areas" :key="a.id" class="area-card" @click="pick.areaId = a.id">
            <img :src="a.cover_image" :alt="a.area_name" />
            <div class="ac-body">
              <div class="ac-name">{{ a.area_name }}</div>
              <div class="muted">{{ a.terrain }} · {{ a.site_count || 6 }} 个营位</div>
            </div>
          </div>
        </div>
      </section>

      <section class="sec">
        <div class="sec-head">
          <h3>可选营位</h3>
          <span class="muted">共 {{ filtered.length }} 个营位 · 显示可预订优先</span>
        </div>
        <div class="site-grid">
          <div v-for="s in paged" :key="s.id" class="site-card">
            <div class="cover">
              <img :src="s.cover_image" :alt="s.site_name" />
              <span class="badge" :class="s.status === 'FREE' ? 'free' : 'busy'">{{ label('siteStatus', s.status) }}</span>
            </div>
            <div class="sc-body">
              <div class="sc-top">
                <span class="sc-name">{{ s.site_name }}</span>
                <span class="sc-price">{{ fmtMoney(s.base_price) }}<i>/晚</i></span>
              </div>
              <div class="muted">{{ s.area_name }} · {{ s.type_name }}</div>
              <div class="sc-tags">
                <el-tag size="small" effect="plain">{{ s.std_capacity }}~{{ s.max_capacity }} 人</el-tag>
                <el-tag size="small" effect="plain">{{ s.area_size }} ㎡</el-tag>
                <el-tag v-if="s.power_supply" size="small" effect="plain" type="success">独立电源</el-tag>
              </div>
              <div class="sc-fac muted">{{ s.facilities }}</div>
              <el-button type="primary" size="small" style="width: 100%; margin-top: 8px" @click="book(s)">预订该营位</el-button>
            </div>
          </div>
        </div>
        <div class="pager">
          <el-pagination v-model:current-page="p" :page-size="8" :total="filtered.length" layout="prev, pager, next, total" />
        </div>
      </section>

      <section class="sec">
        <div class="sec-head">
          <h3>服务与须知</h3>
        </div>
        <div class="notice-grid">
          <div class="notice">
            <h4>入住与退营</h4>
            <p>入住 14:00 后，退营 12:00 前。入住时需核验人数并完成用火用电安全告知。</p>
          </div>
          <div class="notice">
            <h4>物资租赁</h4>
            <p>帐篷、睡袋、天幕、烧烤炉等可在预订时一并选择，归还前请清理干净，损坏按押金标准赔付。</p>
          </div>
          <div class="notice">
            <h4>营地规范</h4>
            <p>22:30 后请降低音量；明火仅限指定烧烤台；垃圾请投放至营区分类点。</p>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { siteApi } from '../api'
import { fmtMoney, label } from '../utils/format'

const router = useRouter()
const areas = ref([])
const types = ref([])
const sites = ref([])
const p = ref(1)
const pick = ref({ areaId: '', typeId: '', range: [] })

const filtered = computed(() => {
  let list = sites.value
  if (pick.value.areaId) list = list.filter((s) => s.area_id === pick.value.areaId)
  if (pick.value.typeId) list = list.filter((s) => s.type_id === pick.value.typeId)
  // 可预订优先，其次按价格升序
  return [...list].sort((a, b) => (a.status === 'FREE' ? -1 : 1) - (b.status === 'FREE' ? -1 : 1) || a.base_price - b.base_price)
})

const paged = computed(() => filtered.value.slice((p.value - 1) * 8, p.value * 8))

function goBook() {
  router.push('/booking/new')
}

function book(s) {
  router.push('/booking/new')
}

onMounted(async () => {
  const opt = await siteApi.options()
  areas.value = opt.areas || []
  types.value = opt.types || []
  const page = await siteApi.page({ page: 1, size: 60 })
  sites.value = page.records || []
})
</script>

<style scoped>
.portal { background: #f5f7fa; min-height: 100%; }

.hero { position: relative; height: 300px; overflow: hidden; }
.hero-bg {
  position: absolute; inset: 0;
  background: url('/uploads/seed/scene/06.jpg') center 40% / cover no-repeat, #1f2937;
}
.hero-veil { position: absolute; inset: 0; background: linear-gradient(100deg, rgba(17,25,35,.9), rgba(31,41,55,.62) 55%, rgba(47,107,138,.28)); }
.hero-inner { position: relative; padding: 34px 40px; color: #fff; max-width: 1280px; margin: 0 auto; }
.mark { display: flex; align-items: center; gap: 9px; font-size: 13px; letter-spacing: 1px; opacity: .9; }
.logo { width: 28px; height: 28px; border-radius: 6px; background: #2f6b8a; display: grid; place-items: center; font-weight: 700; }
.hero h1 { font-size: 29px; margin: 16px 0 6px; font-weight: 600; letter-spacing: .5px; }
.hero .sub { font-size: 13.5px; color: rgba(255,255,255,.78); margin: 0; }
.search {
  display: flex; gap: 10px; margin-top: 22px; background: rgba(255,255,255,.94);
  padding: 10px; border-radius: 10px; width: fit-content; box-shadow: 0 10px 30px rgba(0,0,0,.18);
}

.wrap { max-width: 1280px; margin: 0 auto; padding: 22px 40px 40px; }
.sec { margin-bottom: 26px; }
.sec-head { display: flex; align-items: baseline; gap: 12px; margin-bottom: 12px; }
.sec-head h3 { font-size: 17px; margin: 0; font-weight: 600; }

.area-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.area-card { background: #fff; border-radius: 10px; overflow: hidden; border: 1px solid var(--line); cursor: pointer; transition: .18s; }
.area-card:hover { box-shadow: 0 8px 22px rgba(31,41,55,.12); transform: translateY(-2px); }
.area-card img { width: 100%; height: 132px; object-fit: cover; display: block; }
.ac-body { padding: 10px 12px; }
.ac-name { font-size: 14px; font-weight: 600; margin-bottom: 2px; }

.site-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.site-card { background: #fff; border: 1px solid var(--line); border-radius: 10px; overflow: hidden; }
.cover { position: relative; }
.cover img { width: 100%; height: 150px; object-fit: cover; display: block; }
.badge {
  position: absolute; top: 8px; left: 8px; font-size: 11px; padding: 2px 8px; border-radius: 10px; color: #fff;
}
.badge.free { background: rgba(46,125,91,.9); }
.badge.busy { background: rgba(90,107,123,.9); }
.sc-body { padding: 10px 12px 12px; }
.sc-top { display: flex; justify-content: space-between; align-items: baseline; }
.sc-name { font-size: 14px; font-weight: 600; }
.sc-price { font-size: 15px; font-weight: 600; color: var(--primary); }
.sc-price i { font-style: normal; font-size: 11px; color: var(--text-3); font-weight: 400; }
.sc-tags { display: flex; gap: 5px; margin: 7px 0 5px; flex-wrap: wrap; }
.sc-fac { font-size: 11.5px; line-height: 1.6; min-height: 34px; }

.notice-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.notice { background: #fff; border: 1px solid var(--line); border-radius: 10px; padding: 14px 16px; }
.notice h4 { margin: 0 0 6px; font-size: 14px; }
.notice p { margin: 0; font-size: 12.5px; color: var(--text-2); line-height: 1.8; }

.pager { display: flex; justify-content: center; margin-top: 16px; }

@media (max-width: 1100px) {
  .area-grid, .site-grid { grid-template-columns: repeat(2, 1fr); }
  .notice-grid { grid-template-columns: 1fr; }
  .search { flex-wrap: wrap; width: 100%; }
}
</style>
