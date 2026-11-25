<template>
  <div class="upload-field">
    <div class="preview" v-if="modelValue">
      <img :src="modelValue" alt="已上传图片" />
      <div class="ops">
        <el-button link type="primary" size="small" @click="pick">更换</el-button>
        <el-button link type="danger" size="small" @click="remove">删除</el-button>
      </div>
    </div>
    <div v-else class="picker" @click="pick">
      <el-icon><Plus /></el-icon>
      <span>点击上传图片</span>
      <i>支持 jpg/png，单张不超过 20MB</i>
    </div>
    <el-progress v-if="uploading" :percentage="percent" :stroke-width="4" />
    <input ref="input" type="file" accept="image/*" hidden @change="onFile" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fileApi } from '../api'

/**
 * 上传组件：禁止手填图片 URL，全部走后端 /api/file/upload，
 * 返回相对路径 /uploads/...，直接写入表单字段。
 */
const props = defineProps({ modelValue: { type: String, default: '' }, bizType: { type: String, default: 'tree' } })
const emit = defineEmits(['update:modelValue'])
const input = ref(null)
const uploading = ref(false)
const percent = ref(0)

const pick = () => input.value && input.value.click()

async function onFile(e) {
  const file = e.target.files && e.target.files[0]
  if (!file) return
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.error('图片不能超过 20MB')
    return
  }
  const fd = new FormData()
  fd.append('file', file)
  uploading.value = true
  percent.value = 30
  try {
    const r = await fileApi.upload(file)
    percent.value = 100
    emit('update:modelValue', r.url)
    ElMessage.success('上传成功')
  } catch (err) {
    // 错误提示已由请求拦截器统一处理
  } finally {
    uploading.value = false
    percent.value = 0
    e.target.value = ''
  }
}

async function remove() {
  try {
    await ElMessageBox.confirm('确认删除这张图片吗？', '删除确认', { type: 'warning' })
    emit('update:modelValue', '')
  } catch (e) { /* 用户取消 */ }
}
</script>

<style scoped>
.picker {
  border: 1px dashed #b9d2d4; border-radius: 6px; padding: 16px; text-align: center;
  cursor: pointer; color: #5b6b70; background: #f7fbfb;
}
.picker span { display: block; margin-top: 4px; font-size: 13px; }
.picker i { font-style: normal; font-size: 11px; color: #8ea3a7; }
.preview { display: flex; gap: 12px; align-items: flex-start; }
.preview img { width: 120px; height: 90px; object-fit: cover; border-radius: 6px; border: 1px solid #dbe7e8; }
.ops { display: flex; flex-direction: column; }
</style>
