<template>
  <div class="dashboard">
    <div class="top-bar">
      <div class="tb-left">
        <h1 class="tb-title">数据概览</h1>
        <span class="tb-date">
          <el-icon><Calendar /></el-icon>
          {{ todayStr }}
        </span>
      </div>
      <div class="tb-right">
        <span class="tb-refresh" @click="refreshAll">
          <el-icon :class="{ spinning: refreshing }"><Refresh /></el-icon>
          刷新数据
        </span>
        <el-button type="primary" size="small" @click="$router.push('/system')">
          系统设置
        </el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col :xs="12" :sm="12" :md="6" v-for="(card, idx) in statCards" :key="card.key">
        <div class="stat-card" :class="'sc-' + card.theme" :style="{ animationDelay: idx * 0.06 + 's' }">
          <div class="sc-head">
            <div class="sc-icon-wrap" :style="{ background: card.iconBg }">
              <el-icon :size="20" :color="card.iconColor"><component :is="card.icon" /></el-icon>
            </div>
            <div class="sc-trend-pill" :class="card.trend >= 0 ? 'up' : 'down'" v-if="card.trend !== null && card.trend !== undefined">
              <el-icon :size="11"><Top v-if="card.trend > 0" /><Bottom v-else /></el-icon>
              {{ card.trend > 0 ? '+' : '' }}{{ Math.abs(card.trend) }}%
            </div>
          </div>
          <div class="sc-body">
            <div class="sc-val">{{ card.prefix }}{{ formatVal(card.value, card.key) }}</div>
            <div class="sc-label">{{ card.label }}</div>
          </div>
          <div class="sc-foot">{{ card.desc }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :sm="24" :md="16">
        <el-card shadow="never" class="chart-card main-chart-card">
          <template #header>
            <div class="cc-header">
              <div class="cch-left">
                <span class="cch-title">销售趋势</span>
                <el-tag size="small" type="success" effect="light">近7天</el-tag>
              </div>
              <div class="cch-tabs">
                <span class="cht-item" :class="{ active: chartMode === 'all' }" @click="switchChart('all')">全部</span>
                <span class="cht-divider"></span>
                <span class="cht-item" :class="{ active: chartMode === 'sales' }" @click="switchChart('sales')">销售额</span>
                <span class="cht-divider"></span>
                <span class="cht-item" :class="{ active: chartMode === 'orders' }" @click="switchChart('orders')">订单数</span>
              </div>
            </div>
          </template>
          <div id="salesChart" class="chart-container"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="8">
        <el-card shadow="never" class="chart-card side-card">
          <template #header>
            <span class="cch-title">订单状态分布</span>
          </template>
          <div id="orderStatusChart" class="chart-container-sm"></div>
          <div class="os-legend" v-if="orderStatusLegend.length">
            <div class="osl-item" v-for="item in orderStatusLegend" :key="item.name">
              <i class="osl-dot" :style="{ background: item.color }"></i>
              <span class="osl-name">{{ item.name }}</span>
              <span class="osl-val">{{ item.value }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="data-row">
      <el-col :xs="24" :sm="24" :md="12">
        <el-card shadow="never" class="data-card">
          <template #header>
            <div class="dc-header">
              <span class="cch-title">商品分类统计</span>
              <el-button text type="primary" size="small" @click="$router.push('/product')">
                查看全部 →
              </el-button>
            </div>
          </template>
          <div id="categoryChart" class="chart-container-md"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="12">
        <el-card shadow="never" class="data-card">
          <template #header>
            <div class="dc-header">
              <span class="cch-title">热销商品 TOP10</span>
              <el-tag size="small" type="info" effect="plain" v-if="topProducts.length">共 {{ topProducts.length }} 件</el-tag>
            </div>
          </template>
          <el-table :data="topProducts" size="small" class="rank-table" stripe :show-header="true">
            <el-table-column width="44" align="center">
              <template #default="{ $index }">
                <span class="rank-num" :class="$index < 3 ? 'top' : ''">{{ $index + 1 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="productName" label="商品名称" min-width="120" show-overflow-tooltip />
            <el-table-column prop="sales" label="销量" width="68" align="center" />
            <el-table-column label="销售额" width="90" align="right">
              <template #default="{ row }">
                <span class="money-text">¥{{ (row.totalAmount || 0).toFixed(2) }}</span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!topProducts.length" description="暂无商品数据" :image-size="56" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="bottom-row">
      <el-col :xs="24" :sm="24" :md="12">
        <el-card shadow="never" class="data-card">
          <template #header>
            <div class="dc-header">
              <span class="cch-title">最近助农活动</span>
              <el-button text type="primary" size="small" @click="$router.push('/activity')">查看全部 →</el-button>
            </div>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="(item, i) in recentActivities"
              :key="i"
              :timestamp="item.createdAt"
              placement="top"
              :type="getActivityStatusType(item.status)"
              :hollow="true"
            >
              <div class="tl-content">
                <div class="tl-title">{{ item.name }}</div>
                <p class="tl-desc">{{ item.description?.substring(0, 60) }}{{ item.description?.length > 60 ? '...' : '' }}</p>
                <div class="tl-tags">
                  <el-tag size="small" :type="getActivityTypeType(item.activityType)" effect="light">
                    {{ getActivityTypeText(item.activityType) }}
                  </el-tag>
                  <el-tag size="small" :type="item.status === 1 ? 'success' : 'info'" effect="plain">
                    {{ getActivityStatusText(item.status) }}
                  </el-tag>
                </div>
              </div>
            </el-timeline-item>
            <el-timeline-item v-if="!recentActivities.length" :timestamp="''" placement="top">
              <p class="empty-hint">暂无活动记录</p>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="12">
        <el-card shadow="never" class="data-card alert-card">
          <template #header>
            <div class="dc-header">
              <span class="cch-title">
                待处理售后预警
                <el-badge :value="pendingAfterSalesCount" :max="99" :hidden="pendingAfterSalesCount === 0" />
              </span>
              <el-button text type="warning" size="small" @click="$router.push('/aftersale')">
                去处理 →
              </el-button>
            </div>
          </template>
          <el-table :data="pendingAfterSales" size="small" class="alert-table" stripe max-height="260">
            <el-table-column prop="id" label="ID" width="52" />
            <el-table-column prop="orderId" label="订单号" width="100" show-overflow-tooltip />
            <el-table-column prop="reason" label="原因" min-width="130" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="申请时间" width="150" />
            <el-table-column label="操作" width="64" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleGoToAfterSale(row)">处理</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!pendingAfterSales.length" description="暂无待处理售后" :image-size="48" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { Goods, ShoppingCart, Timer, Money, Top, User, Calendar, Refresh, Bottom } from '@element-plus/icons-vue'
import { getDashboardStats, getDashboardChartData } from '@/api/system'
import { getActivityList } from '@/api/activity'
import { getAfterSaleList } from '@/api/aftersale'

const router = useRouter()
const refreshing = ref(false)
const chartMode = ref('all')

let salesChartInstance: any = null
let orderStatusChartInstance: any = null
let categoryChartInstance: any = null

const statistics = ref({
  totalProducts: 0, totalOrders: 0, totalUsers: 0, totalSales: 0,
  pendingOrders: 0, productTrend: null, orderTrend: null,
  userTrend: null, salesTrend: null
})

const topProducts = ref<any[]>([])
const recentActivities = ref<any[]>([])
const pendingAfterSales = ref<any[]>([])
const pendingAfterSalesCount = ref(0)
const orderStatusLegend = ref<any[]>([])

const todayStr = computed(() => {
  const d = new Date()
  const w = ['日','一','二','三','四','五','六']
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} 周${w[d.getDay()]}`
})

const statCards = computed(() => [
  {
    key: 'totalProducts', label: '在售商品', value: statistics.value.totalProducts || 0,
    prefix: '', desc: '件商品在售', icon: Goods, theme: 'gold',
    iconBg: '#fef9e7', iconColor: '#b8860b',
    trend: statistics.value.productTrend ?? null
  },
  {
    key: 'totalOrders', label: '订单总数', value: statistics.value.totalOrders || 0,
    prefix: '', desc: '笔累计订单', icon: ShoppingCart, theme: 'green',
    iconBg: '#e8faf0', iconColor: '#16a34a',
    trend: statistics.value.orderTrend ?? null
  },
  {
    key: 'pendingOrders', label: '待处理订单', value: statistics.value.pendingOrders || 0,
    prefix: '', desc: '需尽快处理', icon: Timer, theme: 'orange',
    iconBg: '#fff8e6', iconColor: '#d48806',
    trend: null
  },
  {
    key: 'totalSales', label: '总销售额', value: Number(statistics.value.totalSales || 0),
    prefix: '¥', desc: '平台累计收入', icon: Money, theme: 'red',
    iconBg: '#fef2f2', iconColor: '#dc2626',
    trend: statistics.value.salesTrend ?? null
  }
])

function formatVal(val: any, key: string): string {
  const n = Number(val || 0)
  if (key === 'totalSales') return n >= 10000 ? (n / 10000).toFixed(2) + '万' : n.toFixed(2)
  return n.toLocaleString()
}

function switchChart(mode: string) {
  chartMode.value = mode
  nextTick(() => initSalesChart())
}

async function refreshAll() {
  if (refreshing.value) return
  refreshing.value = true
  await Promise.all([
    fetchDashboardData(),
    fetchRecentActivities(),
    fetchPendingAfterSales()
  ])
  await fetchDashboardChartData()
  setTimeout(() => { refreshing.value = false }, 600)
}

const fetchDashboardData = async () => {
  try {
    const res = await getDashboardStats()
    if (res) statistics.value = res.data || res
  } catch (e: any) {
    if (e?.response?.status !== 401) console.error('获取仪表盘数据失败:', e)
  }
}

const fetchDashboardChartData = async () => {
  try {
    const res = await getDashboardChartData()
    if (res) updateChartsWithData(res.data || res)
  } catch (e: any) {
    if (e?.response?.status !== 401) { console.error('获取图表数据失败:', e); initCharts() }
  }
}

const fetchRecentActivities = async () => {
  try {
    const res = await getActivityList({ page: 1, pageSize: 5 })
    recentActivities.value = (res.data || res)?.records || []
  } catch (e: any) { if (e?.response?.status !== 401) console.error('获取最近活动失败:', e) }
}

const fetchPendingAfterSales = async () => {
  try {
    const res = await getAfterSaleList({ page: 1, pageSize: 5, status: 0 })
    const data = res.data || res
    pendingAfterSales.value = data.records || []
    pendingAfterSalesCount.value = data.total || 0
  } catch (e: any) { if (e?.response?.status !== 401) console.error('获取待处理售后失败:', e) }
}

function initCharts() {
  nextTick(() => { initSalesChart(); initOrderStatusChart(); initCategoryChart() })
}

function initSalesChart(data?: any[]) {
  const dom = document.getElementById('salesChart')
  if (!dom) return
  if (salesChartInstance) salesChartInstance.dispose()
  salesChartInstance = echarts.init(dom)

  const hasData = data && data.length > 0
  const sData = hasData ? data.map((d: any) => Number(d.sales || 0)) : []
  const oData = hasData ? data.map((d: any) => Number(d.orderCount || 0)) : []
  const xData = hasData ? data.map((d: any) => d.dayName || d.date) : []

  const showAll = chartMode.value === 'all'

  salesChartInstance.setOption({
    tooltip: {
      trigger: 'axis', axisPointer: { type: 'cross' },
      backgroundColor: 'rgba(255,255,255,0.97)', borderColor: '#e5e7eb',
      borderWidth: 1, textStyle: { color: '#374151', fontSize: 12 },
      extraCssText: 'box-shadow: 0 6px 16px rgba(0,0,0,0.08);border-radius:6px',
      formatter: (params: any) => {
        let h = `<div style="font-weight:600;margin-bottom:6px;font-size:13px">${params[0].axisValue}</div>`
        params.forEach((p: any) => {
          const v = p.seriesName.includes('销售额') ? `¥${Number(p.value||0).toFixed(2)}` : `${p.value} 笔`
          h += `<div style="display:flex;align-items:center;gap:6px;margin:3px 0;font-size:12px">
            <span style="width:8px;height:8px;border-radius:50%;background:${p.color};flex-shrink:0"></span>${p.seriesName}: <b style="margin-left:4px">${v}</b></div>`
        })
        return h
      }
    },
    legend: showAll
      ? { data: ['销售额','订单数'], bottom: 0, textStyle: { color: '#9ca3af', fontSize: 12 }, itemWidth: 12, itemHeight: 12 }
      : { show: false },
    grid: { top: 15, right: showAll ? 22 : 14, bottom: showAll ? 32 : 12, left: 48, containLabel: false },
    xAxis: {
      type: 'category', data: xData, boundaryGap: true,
      axisLine: { lineStyle: { color: '#f3f4f6' } }, axisLabel: { color: '#9ca3af', fontSize: 12 }, axisTick: { show: false }
    },
    yAxis: [{
      type: 'value', name: '销售额(元)', position: 'left',
      nameTextStyle: { color: '#9ca3af', fontSize: 11 },
      axisLabel: { color: '#9ca3af', fontSize: 11, formatter: (v: number) => v >= 10000 ? (v/10000).toFixed(1)+'w' : v },
      splitLine: { lineStyle: { color: '#f9fafb', type: 'dashed' } },
      axisLine: { show: false }, axisTick: { show: false }
    },{
      type: 'value', name: '订单数(笔)', position: 'right',
      nameTextStyle: { color: '#9ca3af', fontSize: 11 },
      axisLabel: { color: '#9ca3af', fontSize: 11 },
      splitLine: { show: false }, axisLine: { show: false }, axisTick: { show: false },
      show: showAll
    }],
    series: showAll ? [
      {
        name: '销售额', type: 'bar', data: sData, barWidth: '32%',
        itemStyle: {
          borderRadius: [5, 5, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#d4a574' }, { offset: 1, color: '#c49b6a' }
          ])
        }
      },
      {
        name: '订单数', type: 'line', yAxisIndex: 1, data: oData,
        smooth: true, symbol: 'circle', symbolSize: 7,
        lineStyle: { color: '#8B6914', width: 2.5 },
        itemStyle: { color: '#8B6914', borderColor: '#fff', borderWidth: 2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(139,105,20,0.15)' }, { offset: 1, color: 'rgba(139,105,20,0)' }
          ])
        }
      }
    ] : [
      {
        type: chartMode.value === 'sales' ? 'bar' : 'line',
        name: chartMode.value === 'sales' ? '销售额' : '订单数',
        data: chartMode.value === 'sales' ? sData : oData,
        ...(chartMode.value === 'sales' ? {
          barWidth: '36%', itemStyle: {
            borderRadius: [5, 5, 0, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#d4a574' }, { offset: 1, color: '#c49b6a' }
            ])
          }
        } : {
          smooth: true, symbol: 'circle', symbolSize: 8,
          lineStyle: { color: '#8B6914', width: 3 },
          itemStyle: { color: '#8B6914', borderColor: '#fff', borderWidth: 2 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(139,105,20,0.18)' }, { offset: 1, color: 'rgba(139,105,20,0)' }
            ])
          }
        })
      }
    ]
  })
}

function initOrderStatusChart(data?: any[]) {
  const dom = document.getElementById('orderStatusChart')
  if (!dom) return
  if (orderStatusChartInstance) orderStatusChartInstance.dispose()
  orderStatusChartInstance = echarts.init(dom)

  const chartData = data?.length ? data : []

  orderStatusLegend.value = chartData.map(d => ({ name: d.name, value: d.value, color: d.itemStyle?.color || '#999' }))

  orderStatusChartInstance.setOption({
    tooltip: { trigger: 'item', backgroundColor: 'rgba(255,255,255,0.97)', borderColor: '#e5e7eb', borderWidth: 1, textStyle: { fontSize: 12 }, formatter: '{b}: <b>{c}</b> ({d}%)' },
    series: [{
      type: 'pie', radius: ['45%', '70%'], center: ['50%', '46%'],
      avoidLabelOverlap: true, itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, position: 'outside', formatter: '{b}\n{d}%', fontSize: 11, color: '#6b7280' },
      emphasis: { label: { show: true, fontSize: 13, fontWeight: 'bold' } },
      data: chartData, animationType: 'scale', animationEasing: 'elasticOut'
    }]
  })
}

function initCategoryChart(data?: any[]) {
  const dom = document.getElementById('categoryChart')
  if (!dom) return
  if (categoryChartInstance) categoryChartInstance.dispose()
  categoryChartInstance = echarts.init(dom)

  const chartData = data?.length ? data : []

  const names = chartData.map((d: any) => d.categoryName)
  const vals = chartData.map((d: any) => d.sales)
  const colors = ['#67C23A','#E6A23C','#F56C6C','#d4a574','#909399','#409EFF','#b37feb']

  categoryChartInstance.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, backgroundColor: 'rgba(255,255,255,0.97)', borderColor: '#e5e7eb', borderWidth: 1 },
    grid: { left: '3%', right: '4%', bottom: '6%', top: '4%', containLabel: true },
    xAxis: { type: 'category', data: names, axisLabel: { rotate: 25, color: '#9ca3af', fontSize: 11 }, axisLine: { lineStyle: { color: '#f3f4f6' } } },
    yAxis: { type: 'value', axisLine: { show: false }, axisTick: { show: false }, splitLine: { lineStyle: { color: '#f9fafb' } }, axisLabel: { color: '#9ca3af' } },
    series: [{
      type: 'bar', barWidth: '48%',
      itemStyle: {
        borderRadius: [5, 5, 0, 0],
        color: (p: any) => new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: colors[p.dataIndex % colors.length] },
          { offset: 1, color: colors[p.dataIndex % colors.length] + 'aa' }
        ])
      }, data: vals
    }]
  })
}

function updateChartsWithData(data: any) {
  nextTick(() => {
    if (data.salesTrend) initSalesChart(data.salesTrend)
    else initSalesChart()
    if (data.orderStatusDist?.data) initOrderStatusChart(data.orderStatusDist.data)
    else initOrderStatusChart()
    if (data.categoryStats) initCategoryChart(data.categoryStats)
    else initCategoryChart()
    if (data.topProducts && data.topProducts.length) {
      topProducts.value = data.topProducts
    }
  })
}

const getActivityTypeText = (type: number) => ({ 1:'大宗采购', 2:'农场参观', 3:'实地观光' }[type]) || '未知'
const getActivityTypeType = (type: number) => ({ 1:'primary', 2:'success', 3:'warning' }[type] as any) || 'info'
const getActivityStatusText = (status: number) => ({ 0:'筹备中', 1:'进行中', 2:'已结束', 3:'已取消' }[status]) || '未知'
const getActivityStatusType = (status: number) => ({ 0:'info', 1:'success', 2:'', 3:'danger' }[status] as any) || 'warning'

const handleGoToAfterSale = (row: any) => router.push({ path: '/aftersale', query: { id: row.id } })

const handleResize = () => requestAnimationFrame(() => {
  salesChartInstance?.resize(); orderStatusChartInstance?.resize(); categoryChartInstance?.resize()
})

onMounted(async () => {
  await Promise.all([fetchDashboardData(), fetchRecentActivities(), fetchPendingAfterSales()])
  await fetchDashboardChartData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  salesChartInstance?.dispose(); orderStatusChartInstance?.dispose(); categoryChartInstance?.dispose()
})
</script>

<style scoped>
.dashboard { padding: 20px; background-color: var(--color-bg-page); min-height: calc(100vh - 84px); }

.top-bar {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 20px; padding: 0 2px;
}
.tb-left { display: flex; align-items: baseline; gap: 12px; }
.tb-title { font-size: 20px; font-weight: 700; color: var(--text-primary); margin: 0; letter-spacing: -0.01em; }
.tb-date { font-size: 13px; color: var(--text-secondary); display: flex; align-items: center; gap: 4px; }
.tb-right { display: flex; align-items: center; gap: 12px; }
.tb-refresh {
  cursor: pointer; display: inline-flex; align-items: center; gap: 4px;
  font-size: 13px; color: var(--text-secondary); transition: color 0.2s; border: none; background: none;
}
.tb-refresh:hover { color: var(--color-primary-600); }
.tb-refresh .spinning { animation: spin 0.8s linear infinite; }

@keyframes spin { to { transform: rotate(360deg); } }

.stats-row { margin-bottom: 16px; }

.stat-card {
  border-radius: 12px; padding: 18px 20px; height: 100%;
  border: 1px solid #f0f0f0; box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  transition: all 0.25s ease; cursor: default; position: relative; overflow: hidden;
  animation: fadeUp 0.4s ease both;
  background: var(--bg-white);
}
.stat-card::after {
  content: ''; position: absolute; right: 0; top: 0; bottom: 0; width: 80px;
  opacity: 0.04; pointer-events: none;
}
.stat-card.sc-gold::after { background: linear-gradient(to left, #d4a574, transparent); }
.stat-card.sc-green::after { background: linear-gradient(to left, #10b981, transparent); }
.stat-card.sc-orange::after { background: linear-gradient(to left, #f59e0b, transparent); }
.stat-card.sc-red::after { background: linear-gradient(to left, #ef4444, transparent); }

.stat-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.07); transform: translateY(-2px); }

.sc-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.sc-icon-wrap {
  width: 36px; height: 36px; border-radius: 10px; display: flex; align-items: center; justify-content: center;
}
.sc-trend-pill {
  display: flex; align-items: center; gap: 2px; font-size: 11.5px; font-weight: 600;
  padding: 2px 8px; border-radius: 10px;
}
.sc-trend-pill.up { background: #ecfdf5; color: #059669; }
.sc-trend-pill.down { background: #fef2f2; color: #dc2626; }

.sc-body { margin-bottom: 4px; }
.sc-val { font-size: 25px; font-weight: 700; color: var(--text-primary); letter-spacing: -0.02em; line-height: 1.2; }
.sc-label { font-size: 12.5px; color: var(--text-secondary); }
.sc-foot { font-size: 11.5px; color: var(--text-muted); }

.chart-row { margin-bottom: 16px; }
.chart-card { border-radius: 12px; border: 1px solid #f0f0f0; overflow: hidden; animation: fadeUp 0.4s 0.1s ease both; }
.main-chart-card { min-height: 380px; }
.side-card { min-height: 380px; display: flex; flex-direction: column; }

.cc-header { display: flex; justify-content: space-between; align-items: center; }
.cch-left { display: flex; align-items: center; gap: 8px; }
.cch-title { font-size: 15px; font-weight: 600; color: var(--text-primary); }
.cch-tabs { display: flex; align-items: center; gap: 0; background: #f3f4f6; border-radius: 8px; padding: 2px; }
.cht-item { padding: 4px 12px; font-size: 12px; color: #6b7280; cursor: pointer; border-radius: 6px; transition: all 0.2s; font-weight: 500; white-space: nowrap; }
.cht-item.active { background: #fff; color: var(--text-primary); box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.cht-divider { width: 1px; height: 14px; background: #e5e7eb; }

.chart-container { width: 100%; height: 290px; }
.chart-container-sm { width: 100%; height: 190px; }
.chart-container-md { width: 100%; height: 240px; }

.os-legend { display: flex; flex-wrap: wrap; gap: 8px; padding: 0 16px 14px; justify-content: center; }
.osl-item { display: flex; align-items: center; gap: 5px; font-size: 12px; color: var(--text-secondary); }
.osl-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.osl-name { }
.osl-val { font-weight: 600; color: var(--text-primary); margin-left: auto; }

.data-row { margin-bottom: 16px; }
.data-card { border-radius: 12px; border: 1px solid #f0f0f0; animation: fadeUp 0.4s 0.2s ease both; }
.dc-header { display: flex; justify-content: space-between; align-items: center; }

.rank-table { --el-table-border-color: #f5f5f5; }
.rank-num { font-size: 13px; font-weight: 700; color: #9ca3af; width: 20px; text-align: center; display: inline-block; }
.rank-num.top { color: #ef4444; }
.money-text { font-weight: 600; color: var(--text-primary); font-size: 13px; }

.bottom-row { margin-bottom: 0; }
.alert-card { }

.tl-content { padding-left: 4px; }
.tl-title { font-size: 14px; font-weight: 600; color: var(--text-primary); margin-bottom: 4px; }
.tl-desc { font-size: 12.5px; color: var(--text-tertiary); margin: 0 0 8px; line-height: 1.5; }
.tl-tags { display: flex; gap: 6px; flex-wrap: wrap; }
.empty-hint { color: var(--text-tertiary); font-size: 13px; margin: 0; }

.alert-table { --el-table-border-color: #f5f5f5; }

:deep(.el-card__header) { padding: 14px 20px; border-bottom: 1px solid #f5f5f5; }
:deep(.el-card__body) { padding: 16px 20px; }
:deep(.el-timeline-item__tail) { border-left-color: #e5e7eb; }
:deep(.el-timeline-item__wrapper) { padding-left: 24px; }

@keyframes fadeUp { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

@media (max-width: 992px) {
  .main-chart-card, .side-card { min-height: auto; }
  .chart-container { height: 250px; }
  .chart-container-sm { height: 200px; }
  .chart-container-md { height: 220px; }
}
@media (max-width: 768px) {
  .dashboard { padding: 12px; }
  .top-bar { flex-direction: column; gap: 10px; align-items: flex-start; }
  .tb-right { width: 100%; justify-content: space-between; }
  .chart-container { height: 220px; }
  .chart-container-sm { height: 180px; }
  .cch-tabs { display: none; }
}
</style>
