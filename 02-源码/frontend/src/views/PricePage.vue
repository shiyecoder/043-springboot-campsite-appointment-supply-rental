<template>
  <div class="page">
    <div class="page-head">
      <h2>类型与价格<span class="sub">营位类型基础价与特殊时段价</span></h2>
      <div class="ops">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增物资</el-button>
      </div>
    </div>

    <!-- 营位类型卡：基础价、容纳人数、视觉色 -->
    <div class="type-row">
      <div v-for="t in types" :key="t.id" class="type-card">
        <div class="bar-top" :style="{ background: t.color || '#2f6b8a' }"></div>
        <div class="tc-body">
          <div class="tc-name">{{ t.type_name }}</div>
          <div class="tc-code mono">{{ t.type_code }}</div>
          <div class="tc-price">{{ fmtMoney(t.base_price) }}<span class="muted"> / 晚</span></div>
          <div class="tc-cap muted">标准 {{ t.std_capacity }} 人 · 最多 {{ t.max_capacity }} 人</div>
        </div>
      </div>
      <div class="type-card ghost">
        <div class="tc-body">
          <div class="tc-name muted">价格调整</div>
          <div class="muted" style="font-size: 12px; line-height: 1.7; margin-top: 6px">
            类型基础价在需求文档中固定，<br />如需调整请在营位档案中改单营位价。
          </div>
        </div>
      </div>
    </div>

    <!-- 营位单价与启停 -->
    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="营位编号 / 名称" clearable :prefix-icon="Search" @keyup.enter="search" />
      <el-select v-model="query.areaId" placeholder="全部营区" clearable @change="search">
        <el-option v-for="a in areas" :key="a.id" :label="a.area_name" :value="a.id" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <div class="table-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="site_no" label="营位编号" min-width="110">
          <template #default="{ row }"><span class="mono">{{ row.site_no }}</span></template>
        </el-table-column>
        <el-table-column prop="site_name" label="营位名称" min-width="130" />
        <el-table-column prop="area_name" label="营区" min-width="110" />
        <el-table-column prop="type_name" label="类型" min-width="120" />
        <el-table-column label="标准/最大人数" width="120" align="center">
          <template #default="{ row }">{{ row.std_capacity }} / {{ row.max_capacity }}</template>
        </el-table-column>
        <el-table-column label="单价(元/晚)" width="120" align="right">
          <template #default="{ row }"><span class="money">{{ fmtMoney(row.base_price) }}</span></template>
        </el-table-column>
        <el-table-column label="当前状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="tagType('site', row.status)">{{ label('siteStatus', row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="启停" width="120" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status !== 'OFF'"
              :disabled="row.status === 'OCCUPIED'"
              @change="(v) => toggle(row, v)"
            />
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="onPage"
          @size-change="onSize"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { siteApi } from '../api'
import { useTable } from '../composables/useTable'
import { fmtMoney, label, tagType } from '../utils/format'

const types = ref([])
const areas = ref([])
const { rows, total, loading, page, size, query, load, search, reset, onPage, onSize } = useTable(
  (p) => siteApi.page(p),
  { keyword: '', areaId: '' }
)

async function toggle(row, on) {
  await siteApi.update(row.id, { status: on ? 'FREE' : 'OFF' })
  ElMessage.success(on ? '已启用' : '已停用')
  load()
}

function openCreate() {
  ElMessage.info('物资请在「物资档案」中新增，本页仅维护营位价格与启停')
}

onMounted(async () => {
  const opt = await siteApi.options()
  types.value = opt.types || []
  areas.value = opt.areas || []
  load()
})
</script>

<style scoped>
.type-row { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; margin-bottom: 14px; }
.type-card {
  background: #fff; border: 1px solid var(--line); border-radius: var(--radius);
  box-shadow: var(--shadow-card); overflow: hidden;
}
.type-card.ghost { border-style: dashed; background: #fbfcfd; box-shadow: none; }
.bar-top { height: 4px; }
.tc-body { padding: 12px 13px; }
.tc-name { font-size: 14px; font-weight: 600; }
.tc-code { color: var(--text-3); margin-top: 2px; }
.tc-price { font-size: 19px; font-weight: 600; color: var(--primary); margin: 8px 0 4px; }
.tc-cap { font-size: 12px; }
@media (max-width: 1280px) { .type-row { grid-template-columns: repeat(3, 1fr); } }
</style>
