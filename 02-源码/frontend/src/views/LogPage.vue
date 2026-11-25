<template>
  <div class="page">
    <div class="page-head">
      <h2>操作日志<span class="sub">共 {{ total }} 条</span></h2>
    </div>

    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="操作人 / 接口 / 描述" clearable :prefix-icon="Search" @keyup.enter="search" />
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <span class="grow"></span>
      <span class="muted">登录、状态流转、物资发放等关键动作均落库留痕</span>
    </div>

    <div class="table-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ fmtMinute(row.createTime || row.create_time) }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="120">
          <template #default="{ row }">{{ row.operatorName || row.operator_name || '-' }}</template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="120">
          <template #default="{ row }"><span class="muted">{{ row.module || '-' }}</span></template>
        </el-table-column>
        <el-table-column prop="action" label="动作" min-width="150">
          <template #default="{ row }">{{ row.action || row.description || '-' }}</template>
        </el-table-column>
        <el-table-column prop="method" label="接口" min-width="220">
          <template #default="{ row }"><span class="mono">{{ row.method || row.url || '-' }}</span></template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="130">
          <template #default="{ row }">{{ row.ip || '-' }}</template>
        </el-table-column>
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 200 || row.success ? 'success' : 'info'">
              {{ row.status || (row.success ? '成功' : '-') }}
            </el-tag>
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
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { systemApi } from '../api'
import { useTable } from '../composables/useTable'
import { fmtMinute } from '../utils/format'

const { rows, total, loading, page, size, query, load, search, reset, onPage, onSize } = useTable(
  (p) => systemApi.logs(p),
  { keyword: '' }
)

onMounted(load)
</script>
