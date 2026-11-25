<template>
  <el-tag :type="statusType(value)" effect="light" size="small" class="st">{{ value || '-' }}</el-tag>
</template>

<script setup>
import { statusType } from '../utils/format'
// 状态标签：文字 + 颜色双重表达，不依赖颜色单独传达含义
defineProps({ value: { type: String, default: '' } })
</script>

<style scoped>
.st { font-weight: 500; }
</style>
