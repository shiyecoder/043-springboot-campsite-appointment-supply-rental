<template>
  <div class="page">
    <div class="page-head">
      <h2>物资档案<span class="sub">共 {{ total }} 种租赁物资</span></h2>
      <div class="ops">
        <el-button :icon="Van" @click="$router.push('/goods/pick')">今日备货</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增物资</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="物资编号 / 名称" clearable :prefix-icon="Search" @keyup.enter="search" />
      <el-select v-model="query.categoryId" placeholder="全部类别" clearable @change="search">
        <el-option v-for="c in categories" :key="c.id" :label="c.category_name || c.name" :value="c.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="全部状态" clearable @change="search">
        <el-option v-for="(v, k) in DICT.goodsStatus" :key="k" :label="v" :value="k" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <div class="table-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column label="图片" width="96">
          <template #default="{ row }"><img class="thumb" :src="row.image" :alt="row.goods_name" /></template>
        </el-table-column>
        <el-table-column prop="goods_no" label="编号" width="106">
          <template #default="{ row }"><span class="mono">{{ row.goods_no }}</span></template>
        </el-table-column>
        <el-table-column prop="goods_name" label="物资名称" min-width="140" />
        <el-table-column prop="category_name" label="类别" width="110" />
        <el-table-column prop="spec" label="规格" min-width="160">
          <template #default="{ row }"><span class="muted">{{ row.spec }}</span></template>
        </el-table-column>
        <el-table-column label="总库存" width="92" align="center">
          <template #default="{ row }">
            <span :class="row.total_stock <= 3 ? 'danger-text strong' : ''">{{ row.total_stock }}</span>
          </template>
        </el-table-column>
        <el-table-column label="租金" width="112" align="right">
          <template #default="{ row }">
            <span class="money">{{ fmtMoney(row.rent_price) }}</span>
            <span class="muted"> /{{ row.rent_unit }}</span>
          </template>
        </el-table-column>
        <el-table-column label="押金" width="100" align="right">
          <template #default="{ row }">{{ fmtMoney(row.deposit) }}</template>
        </el-table-column>
        <el-table-column label="周转(h)" width="92" align="center">
          <template #default="{ row }">{{ row.turnover_hours }}</template>
        </el-table-column>
        <el-table-column label="状态" width="104">
          <template #default="{ row }">
            <el-tag size="small" :type="tagType('goods', row.status)">{{ label('goodsStatus', row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="96" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dlg" :title="form.id ? '编辑物资' : '新增物资'" width="640px" destroy-on-close>
      <el-form :model="form" label-width="92px">
        <el-row :gutter="14">
          <el-col :span="12"><el-form-item label="物资编号"><el-input v-model="form.goodsNo" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="物资名称" required><el-input v-model="form.goodsName" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="类别">
              <el-select v-model="form.categoryId" style="width: 100%">
                <el-option v-for="c in categories" :key="c.id" :label="c.category_name || c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="规格"><el-input v-model="form.spec" placeholder="如 2 人 / 210×150cm" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="总库存"><el-input-number v-model="form.totalStock" :min="0" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="租金(元)"><el-input-number v-model="form.rentPrice" :min="0" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="8">
            <el-form-item label="计量单位">
              <el-select v-model="form.rentUnit" style="width: 100%">
                <el-option label="晚" value="晚" />
                <el-option label="天" value="天" />
                <el-option label="次" value="次" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="押金(元)"><el-input-number v-model="form.deposit" :min="0" :step="50" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="周转时长(h)"><el-input-number v-model="form.turnoverHours" :min="0" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="物资图片"><UploadField v-model="form.image" /></el-form-item></el-col>
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
import { ElMessage } from 'element-plus'
import { Plus, Search, Van } from '@element-plus/icons-vue'
import UploadField from '../components/UploadField.vue'
import { goodsApi, siteApi } from '../api'
import { useTable } from '../composables/useTable'
import { DICT, fmtMoney, label, tagType } from '../utils/format'

const categories = ref([])
const dlg = ref(false)
const saving = ref(false)
const form = ref({})

const { rows, total, loading, page, size, query, load, search, reset, onPage, onSize } = useTable(
  (p) => goodsApi.page(p),
  { keyword: '', categoryId: '', status: '' }
)

function openCreate() {
  form.value = {
    id: null, goodsNo: '', goodsName: '', categoryId: categories.value[0] && categories.value[0].id,
    spec: '', totalStock: 10, rentPrice: 30, rentUnit: '晚', deposit: 100, turnoverHours: 8, remark: '', image: ''
  }
  dlg.value = true
}

function openEdit(row) {
  form.value = {
    id: row.id, goodsNo: row.goods_no, goodsName: row.goods_name, categoryId: row.category_id,
    spec: row.spec, totalStock: row.total_stock, rentPrice: row.rent_price, rentUnit: row.rent_unit,
    deposit: row.deposit, turnoverHours: row.turnover_hours, remark: row.remark, image: row.image
  }
  dlg.value = true
}

async function save() {
  if (!form.value.goodsName) return ElMessage.warning('请填写物资名称')
  saving.value = true
  try {
    const { id, ...payload } = form.value
    if (id) await goodsApi.update(id, payload)
    else await goodsApi.create(payload)
    ElMessage.success('已保存')
    dlg.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const opt = await siteApi.options()
  categories.value = opt.categories || []
  load()
})
</script>
