<template>
  <div class="page">
    <div class="page-head">
      <h2>营区与营位<span class="sub">共 {{ total }} 个营位</span></h2>
      <div class="ops">
        <el-button :icon="MapLocation" @click="$router.push('/booking/calendar')">查看可订日历</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增营位</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="营位编号 / 名称" clearable :prefix-icon="Search" @keyup.enter="search" />
      <el-select v-model="query.areaId" placeholder="全部营区" clearable @change="search">
        <el-option v-for="a in areas" :key="a.id" :label="a.area_name" :value="a.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="全部状态" clearable @change="search">
        <el-option v-for="(v, k) in DICT.siteStatus" :key="k" :label="v" :value="k" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <span class="grow"></span>
      <span class="muted">相邻营位间隔与私密性说明在详情中查看</span>
    </div>

    <div class="table-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column label="图片" width="96">
          <template #default="{ row }">
            <img class="thumb" :src="row.cover_image" :alt="row.site_name" />
          </template>
        </el-table-column>
        <el-table-column prop="site_no" label="营位编号" min-width="110">
          <template #default="{ row }"><span class="mono">{{ row.site_no }}</span></template>
        </el-table-column>
        <el-table-column prop="site_name" label="营位名称" min-width="130" />
        <el-table-column label="营区 / 地形" min-width="150">
          <template #default="{ row }">
            {{ row.area_name }}<span class="muted"> · {{ row.terrain }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" min-width="126">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :color="row.type_color" style="border: none; color: #fff">
              {{ row.type_name }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="容纳人数" width="106" align="center">
          <template #default="{ row }">{{ row.std_capacity }} / {{ row.max_capacity }}</template>
        </el-table-column>
        <el-table-column label="面积" width="88" align="right">
          <template #default="{ row }">{{ row.area_size }} ㎡</template>
        </el-table-column>
        <el-table-column label="基础价" width="100" align="right">
          <template #default="{ row }"><span class="money">{{ fmtMoney(row.base_price) }}</span></template>
        </el-table-column>
        <el-table-column label="设施" min-width="190">
          <template #default="{ row }"><span class="muted">{{ row.facilities }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="94">
          <template #default="{ row }">
            <el-tag size="small" :type="tagType('site', row.status)">{{ label('siteStatus', row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="在营预约" width="94" align="center">
          <template #default="{ row }">
            <el-badge v-if="row.active_booking" :value="row.active_booking" type="warning" />
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="toggle(row)">
              {{ row.status === 'OFF' ? '启用' : '停用' }}
            </el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="onPage"
          @size-change="onSize"
        />
      </div>
    </div>

    <!-- 新增 / 编辑营位 -->
    <el-dialog v-model="dlg" :title="form.id ? '编辑营位' : '新增营位'" width="720px" destroy-on-close>
      <el-form :model="form" label-width="96px">
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="营位编号" required>
              <el-input v-model="form.siteNo" placeholder="如 CP-A07" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="营位名称" required>
              <el-input v-model="form.siteName" placeholder="如 湖畔7号营位" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属营区">
              <el-select v-model="form.areaId" style="width: 100%">
                <el-option v-for="a in areas" :key="a.id" :label="a.area_name" :value="a.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="营位类型">
              <el-select v-model="form.typeId" style="width: 100%">
                <el-option v-for="t in types" :key="t.id" :label="t.type_name" :value="t.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="标准人数">
              <el-input-number v-model="form.stdCapacity" :min="1" :max="12" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最大人数">
              <el-input-number v-model="form.maxCapacity" :min="1" :max="20" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="面积(㎡)">
              <el-input-number v-model="form.areaSize" :min="10" :max="300" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="基础价(元)">
              <el-input-number v-model="form.basePrice" :min="0" :step="10" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开放日期">
              <el-date-picker v-model="form.openDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="配套设施">
              <el-input v-model="form.facilities" placeholder="独立电源、公共水桩、烧烤台…" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="环境说明">
              <el-input v-model="form.environment" type="textarea" :rows="2" placeholder="相邻营位间隔、树荫、坡道等" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="封面图">
              <UploadField v-model="form.coverImage" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="详情图">
              <UploadField v-model="form.detailImage" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MapLocation, Plus, Search } from '@element-plus/icons-vue'
import UploadField from '../components/UploadField.vue'
import { siteApi } from '../api'
import { useTable } from '../composables/useTable'
import { DICT, fmtMoney, label, tagType } from '../utils/format'

const areas = ref([])
const types = ref([])
const { rows, total, loading, page, size, query, load, search, reset, onPage, onSize } = useTable(
  (p) => siteApi.page(p),
  { keyword: '', areaId: '', status: '' }
)

const dlg = ref(false)
const saving = ref(false)
const form = ref({})

const blank = () => ({
  id: null, siteNo: '', siteName: '', areaId: null, typeId: null, stdCapacity: 2, maxCapacity: 4,
  areaSize: 40, basePrice: 168, facilities: '', environment: '', coverImage: '', detailImage: '',
  openDate: '', deviceNote: ''
})

function openCreate() {
  form.value = blank()
  if (areas.value.length) form.value.areaId = areas.value[0].id
  if (types.value.length) form.value.typeId = types.value[0].id
  dlg.value = true
}

function openEdit(row) {
  form.value = {
    id: row.id, siteNo: row.site_no, siteName: row.site_name, areaId: row.area_id, typeId: row.type_id,
    stdCapacity: row.std_capacity, maxCapacity: row.max_capacity, areaSize: row.area_size,
    basePrice: row.base_price, facilities: row.facilities, environment: row.environment,
    coverImage: row.cover_image, detailImage: row.detail_image, openDate: row.open_date,
    deviceNote: row.device_note
  }
  dlg.value = true
}

async function save() {
  if (!form.value.siteNo || !form.value.siteName) return ElMessage.warning('请填写营位编号与名称')
  saving.value = true
  try {
    const { id, ...payload } = form.value
    if (id) await siteApi.update(id, payload)
    else await siteApi.create(payload)
    ElMessage.success('已保存')
    dlg.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggle(row) {
  const next = row.status === 'OFF' ? 'FREE' : 'OFF'
  await siteApi.update(row.id, { status: next })
  ElMessage.success(next === 'OFF' ? '已停用该营位' : '已启用该营位')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除营位 ${row.site_no} 吗？`, '删除确认', { type: 'warning' })
  await siteApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  const opt = await siteApi.options()
  areas.value = opt.areas || []
  types.value = opt.types || []
  load()
})
</script>
