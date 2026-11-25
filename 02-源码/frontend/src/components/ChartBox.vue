<template>
  <div ref="el" class="chart" :style="{ height: height + 'px' }"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

/** 图表容器：统一主题色与自适应，页面只传 option */
const props = defineProps({
  option: { type: Object, required: true },
  height: { type: Number, default: 240 }
})
const el = ref(null)
let chart = null

const PALETTE = ['#2F6B8A', '#3B627A', '#2E7D5B', '#B07B2B', '#6E8FA5', '#9DB4C4', '#1F2937']

function render() {
  if (!el.value) return
  if (!chart) chart = echarts.init(el.value)
  chart.setOption({ color: PALETTE, ...props.option }, true)
}

const resize = () => chart && chart.resize()

onMounted(async () => {
  await nextTick()
  render()
  window.addEventListener('resize', resize)
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart && chart.dispose()
  chart = null
})
</script>

<style scoped>
.chart { width: 100%; }
</style>
