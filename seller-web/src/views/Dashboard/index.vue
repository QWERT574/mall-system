<template>
  <div class="dashboard">
    <div class="page-header">
      <div class="header-left">
        <h2 class="greeting">{{ greeting }}，{{ sellerStore.user.username || '商家' }}</h2>
        <p class="date-info">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
          {{ currentDate }}
        </p>
      </div>
      <div class="header-right">
        <div class="quick-stats" v-if="dashboardData.totalOrders > 0">
          <span class="qs-item">
            <i class="qs-dot green"></i>今日订单: <strong>{{ todayOrderCount }}</strong>
          </span>
          <span class="qs-item">
            <i class="qs-dot orange"></i>待发货: <strong>{{ dashboardData.pendingOrders || 0 }}</strong>
          </span>
        </div>
        <div class="header-actions">
          <button class="header-btn primary" @click="router.push('/product/create')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            发布商品
          </button>
          <button class="header-btn" @click="router.push('/orders')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
            订单管理
          </button>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="loading-shimmer" v-for="n in 4" :key="n" :style="{height: n === 1 ? '120px' : n === 4 ? '200px' : '140px'}"></div>
    </div>

    <template v-else>
      <div class="stats-row">
        <div class="stat-card" v-for="(stat, i) in stats" :key="stat.key"
             :class="[stat.class]" :style="{ animationDelay: `${i * 0.06}s` }">
          <div class="sc-top">
            <div class="sc-icon">
              <component :is="stat.iconSvg" />
            </div>
            <div class="sc-trend" :class="stat.trendClass" v-if="stat.trend !== null && stat.trend !== undefined">
              <svg v-if="stat.trend > 0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="14" height="14"><polyline points="18 15 12 9 6 15"/></svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="14" height="14"><polyline points="6 9 12 15 18 9"/></svg>
              <span>{{ stat.trend > 0 ? '+' : '' }}{{ Math.abs(stat.trend) }}%</span>
            </div>
          </div>
          <div class="sc-value">{{ stat.prefix }}{{ formatStatValue(stat.value, stat.key) }}</div>
          <div class="sc-label">{{ stat.label }}</div>
          <div class="sc-sub">{{ stat.sub }}</div>
          <div class="sc-action" v-if="stat.actionText" @click="router.push(stat.actionTo)">
            {{ stat.actionText }} &rarr;
          </div>
        </div>
      </div>

      <div class="main-content-row">
        <div class="chart-card main-chart">
          <div class="cc-header">
            <div class="cch-left">
              <h3>销售趋势</h3>
              <span class="cch-badge">近7天</span>
            </div>
            <div class="cch-right">
              <button class="tab-btn" :class="{active: salesView === 'sales'}" @click="salesView = 'sales'">销售额</button>
              <button class="tab-btn" :class="{active: salesView === 'orders'}" @click="salesView = 'orders'">订单数</button>
            </div>
          </div>
          <div class="cc-body">
            <div ref="salesChartRef" class="echart-box"></div>
          </div>
        </div>

        <div class="side-panel">
          <div class="chart-card mini-chart">
            <div class="cc-header">
              <h3>订单状态</h3>
            </div>
            <div class="cc-body">
              <div ref="orderChartRef" class="echart-box-sm"></div>
            </div>
          </div>

          <div class="quick-entry-card">
            <h4>快捷入口</h4>
            <div class="qe-grid">
              <a class="qe-item" v-for="item in quickEntries" :key="item.label" @click="router.push(item.to)">
                <div class="qei-icon" :style="{background: item.bg}">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18" v-html="item.icon"></svg>
                </div>
                <span>{{ item.label }}</span>
                <span class="qei-count" v-if="item.count !== undefined">{{ item.count }}</span>
              </a>
            </div>
          </div>
        </div>
      </div>

      <div class="table-section">
        <div class="table-card">
          <div class="tc-header">
            <div class="tch-left">
              <h3>最近订单</h3>
              <span class="tc-count">共 {{ recentOrders.length }} 条</span>
            </div>
            <button class="tc-more" @click="router.push('/orders')">
              查看全部
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><polyline points="9 18 15 12 9 6"/></svg>
            </button>
          </div>
          <div class="tc-body">
            <table class="order-table">
              <thead>
                <tr>
                  <th>订单号</th>
                  <th>商品信息</th>
                  <th>金额</th>
                  <th>买家</th>
                  <th>状态</th>
                  <th>下单时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="recentOrders.length === 0">
                  <td colspan="7" class="empty-row">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="40" height="40"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
                    暂无订单数据
                  </td>
                </tr>
                <tr v-for="order in recentOrders" :key="order.id">
                  <td class="td-order-no">{{ order.orderNo || ('#' + order.id) }}</td>
                  <td class="td-product">
                    <div class="tp-info">
                      <span class="tp-name">{{ order.productName || '-' }}</span>
                      <span class="tp-spec">x{{ order.quantity || 1 }}</span>
                    </div>
                  </td>
                  <td class="td-money">¥{{ Number(order.totalAmount || order.payAmount || 0).toFixed(2) }}</td>
                  <td class="td-buyer">{{ order.buyerName || order.receiverName || '-' }}</td>
                  <td><span class="status-tag" :class="'st-' + (order.status ?? 0)">{{ getStatusText(order.status) }}</span></td>
                  <td class="td-time">{{ formatTime(order.createdAt) }}</td>
                  <td class="td-action">
                    <button class="action-link" @click="goOrderDetail(order)">详情</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick, h } from 'vue'
import { useRouter } from 'vue-router'
import { useSellerStore } from '@/stores/seller'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import api from '@/utils/api'

const router = useRouter()
const sellerStore = useSellerStore()
const loading = ref(true)
const salesView = ref('sales')

const salesChartRef = ref(null)
const orderChartRef = ref(null)
let salesChart = null
let orderChart = null

const dashboardData = ref({
  totalProducts: 0, totalOrders: 0, pendingOrders: 0, totalRevenue: 0
})
const salesTrendData = ref([])
const orderStatusDataRaw = ref([])
const recentOrders = ref([])

const currentDate = computed(() => {
  const d = new Date()
  const w = ['日','一','二','三','四','五','六']
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${w[d.getDay()]}`
})

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 9) return '早上好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const todayOrderCount = computed(() => {
  if (!salesTrendData.value.length) return 0
  const today = salesTrendData.value[salesTrendData.value.length - 1]
  return today ? today.orderCount : 0
})

const PackageIcon = {
  render: () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': 2 }, [
    h('path', { d: 'M12 3l8 5v8l-8 5-8-5V8z' }),
    h('path', { d: 'M12 3v10' }),
    h('line', { x1: 3, y1: 8, x2: 21, y2: 16 })
  ])
}
const OrderIcon = {
  render: () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': 2 }, [
    h('path', { d: 'M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z' }),
    h('polyline', { points: '14 2 14 8 20 8' }),
    h('line', { x1: 16, y1: 13, x2: 8, y2: 13 }),
    h('line', { x1: 16, y1: 17, x2: 8, y2: 17 })
  ])
}
const ClockIcon = {
  render: () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': 2 }, [
    h('circle', { cx: 12, cy: 12, r: 10 }),
    h('polyline', { points: '12 6 12 12 16 14' })
  ])
}
const MoneyIcon = {
  render: () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': 2 }, [
    h('line', { x1: 12, y1: 1, x2: 12, y2: 23 }),
    h('path', { d: 'M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6' })
  ])
}

const stats = computed(() => [
  {
    key:'products', label:'在售商品', value: dashboardData.value.totalProducts || 0,
    prefix:'', sub:'件商品在售中', iconSvg: PackageIcon, class:'gold',
    trend: null, actionText:'管理商品', actionTo:'/products'
  },
  {
    key:'orders', label:'累计订单', value: dashboardData.value.totalOrders || 0,
    prefix:'', sub:'笔历史订单', iconSvg: OrderIcon, class:'green',
    trend: dashboardData.value.totalOrders > 0 ? 12 : null, trendClass: 'up',
    actionText:'查看全部', actionTo:'/orders'
  },
  {
    key:'pending', label:'待处理', value: dashboardData.value.pendingOrders || 0,
    prefix:'', sub:'需要您处理', iconSvg: ClockIcon, class:'orange',
    trend: null, actionText:'立即处理', actionTo:'/orders?status=1'
  },
  {
    key:'revenue', label:'总销售额', value: Number(dashboardData.value.totalRevenue || 0),
    prefix:'¥', sub:'累计收入', iconSvg: MoneyIcon, class:'blue',
    trend: 8, trendClass: 'up', actionText:null, actionTo:null
  }
])

const quickEntries = computed(() => [
  { label: '发布新商品', to: '/product/create', bg: '#fef3c7', icon: '<line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>' },
  { label: '待发货订单', to: '/orders?status=1', bg: '#dbeafe', icon: '<rect x="1" y="3" width="15" height="13"/><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/>' },
  { label: '优惠券管理', to: '/coupons', bg: '#dcfce7', icon: '<path d="M20 12V8H4v4"/><path d="M4 8l8-4 8 4"/>' },
  { label: '售后处理', to: '/aftersale', bg: '#fee2e2', icon: '<circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/>' }
])

const weekLabels = computed(() => {
  if (salesTrendData.value?.length) return salesTrendData.value.map(d => d.dayName || d.date)
  return ['周一','周二','周三','周四','周五','周六','周日']
})
const weekRevenue = computed(() => {
  if (salesTrendData.value?.length) return salesTrendData.value.map(d => Number(d.sales || 0))
  return Array(7).fill(0)
})
const weekOrders = computed(() => {
  if (salesTrendData.value?.length) return salesTrendData.value.map(d => Number(d.orderCount || 0))
  return Array(7).fill(0)
})

const orderStatusData = computed(() => {
  if (orderStatusDataRaw.value?.length) return orderStatusDataRaw.value
  return [
    { name: '待发货', value: 0, itemStyle: { color: '#f59e0b' } },
    { name: '已完成', value: 0, itemStyle: { color: '#10b981' } },
    { name: '已取消', value: 0, itemStyle: { color: '#ef4444' } },
    { name: '已发货', value: 0, itemStyle: { color: '#3b82f6' } }
  ]
})

function formatStatValue(val, key) {
  if (key === 'revenue') {
    const n = Number(val || 0)
    return n >= 10000 ? (n / 10000).toFixed(2) + '万' : n.toFixed(2)
  }
  return val ?? 0
}

function getStatusText(status) {
  const map = { 0: '待发货', 1: '待付款', 2: '已完成', 3: '已取消', 4: '已发货', 5: '已签收', 6: '已取消' }
  return map[status] || '未知'
}

function formatTime(time) {
  if (!time) return '-'
  const d = new Date(time)
  return `${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

function goOrderDetail(order) {
  router.push({ path: '/orders', query: { id: order.id, orderNo: order.orderNo } })
}

const initSalesChart = () => {
  if (!salesChartRef.value) return
  if (salesChart) salesChart.dispose()
  salesChart = echarts.init(salesChartRef.value)

  const isSalesMode = salesView.value === 'sales'
  const mainData = isSalesMode ? weekRevenue.value : weekOrders.value
  const secondaryData = isSalesMode ? weekOrders.value : weekRevenue.value
  const mainName = isSalesMode ? '销售额' : '订单数'
  const secondName = isSalesMode ? '订单数' : '销售额'

  salesChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: { color: '#374151', fontSize: 12 },
      extraCssText: 'box-shadow: 0 4px 12px rgba(0,0,0,0.08)',
      formatter: (params) => {
        let html = `<div style="font-weight:600;margin-bottom:6px">${params[0].axisValue}</div>`
        params.forEach(p => {
          const val = p.seriesName.includes('销售额') ? `¥${Number(p.value||0).toFixed(2)}` : p.value
          html += `<div style="display:flex;align-items:center;gap:6px;margin:3px 0;font-size:12px">
            <span style="width:8px;height:8px;border-radius:50%;background:${p.color};flex-shrink:0"></span>
            ${p.seriesName}: <b>${val}</b></div>`
        })
        return html
      }
    },
    legend: { data: [mainName, secondName], bottom: 0, textStyle: { color: '#9ca3af', fontSize: 12 }, itemWidth: 12, itemHeight: 12 },
    grid: { top: 15, right: 20, bottom: 32, left: 50 },
    xAxis: {
      type: 'category', data: weekLabels.value,
      axisLine: { lineStyle: { color: '#f3f4f6' } }, axisLabel: { color: '#9ca3af', fontSize: 12 }, axisTick: { show: false },
      boundaryGap: true
    },
    yAxis: [{
      type: 'value',
      name: isSalesMode ? '元' : '笔',
      nameTextStyle: { color: '#9ca3af', fontSize: 11 },
      axisLabel: { color: '#9ca3af', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f9fafb', type: 'dashed' } },
      axisLine: { show: false }, axisTick: { show: false }
    },{
      type: 'value',
      name: isSalesMode ? '笔' : '元',
      nameTextStyle: { color: '#9ca3af', fontSize: 11 },
      axisLabel: { color: '#9ca3af', fontSize: 11 },
      splitLine: { show: false },
      axisLine: { show: false }, axisTick: { show: false }
    }],
    series: [{
      name: mainName, type: 'bar', data: mainData, barWidth: 22,
      itemStyle: {
        borderRadius: [4, 4, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#d4a574' }, { offset: 1, color: '#c49b6a' }
        ])
      },
      animationDelay: (idx) => idx * 80
    },{
      name: secondName, type: 'line', yAxisIndex: 1, data: secondaryData,
      smooth: true, symbol: 'circle', symbolSize: 6,
      lineStyle: { color: '#8B5E3C', width: 2 },
      itemStyle: { color: '#8B5E3C', borderColor: '#fff', borderWidth: 2 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(139,94,60,0.12)' }, { offset: 1, color: 'rgba(139,94,60,0)' }
        ])
      },
      animationDelay: (idx) => idx * 80 + 150
    }]
  })
}

const initOrderChart = () => {
  if (!orderChartRef.value) return
  if (orderChart) orderChart.dispose()
  orderChart = echarts.init(orderChartRef.value)

  const data = orderStatusData.value.filter(d => d.value > 0)
  const hasData = data.length > 0

  orderChart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255,255,255,0.96)', borderColor: '#e5e7eb', borderWidth: 1,
      textStyle: { fontSize: 12, color: '#374151' },
      formatter: '{b}: <b>{c}</b> 笔 ({d}%)'
    },
    series: [{
      type: 'pie', radius: hasData ? ['42%', '70%'] : ['45%', '68%'],
      center: ['50%', '48%'], avoidLabelOverlap: true,
      itemStyle: { borderRadius: 5, borderColor: '#fff', borderWidth: 2 },
      label: {
        show: hasData, position: 'outside', fontSize: 11, color: '#6b7280',
        formatter: '{b}\n{d}%'
      },
      emphasis: { label: { show: true, fontSize: 13, fontWeight: 'bold' } },
      data: hasData ? data : orderStatusData.value,
      animationType: 'scale', animationEasing: 'elasticOut',
      itemStyle: { color: (p) => p.data.itemStyle?.color || '#d1d5db' }
    }]
  })

  if (!hasData) {
    orderChart.setOption({
      graphic: [{ type: 'text', left: 'center', top: '48%', style: { text: '暂无数据', fontSize: 13, fill: '#9ca3af' } }]
    })
  }
}

const loadData = async () => {
  try {
    const user = sellerStore.user
    if (!user?.id || !sellerStore.token) {
      ElMessage.warning('请先登录')
      router.push('/login')
      return
    }

    const [dashResult, ordersResult] = await Promise.allSettled([
      api.getSellerDashboard(user.id),
      api.getSellerOrdersPage(user.id, 1, 8)
    ])

    if (dashResult.status === 'fulfilled' && dashResult.value) {
      dashboardData.value = dashResult.value
      salesTrendData.value = dashResult.value.salesTrend || []
      if (dashResult.value.orderStatusDist?.data) {
        orderStatusDataRaw.value = dashResult.value.orderStatusDist.data
      }
    }

    if (ordersResult.status === 'fulfilled' && ordersResult.value) {
      const data = ordersResult.value.records || ordersResult.value
      recentOrders.value = Array.isArray(data) ? data.slice(0, 8) : []
    }
  } catch (error) {
    if (error.message?.includes('未登录') || error.message?.includes('登录已过期')) {
      ElMessage.warning('登录已过期，请重新登录')
      setTimeout(() => router.push('/login'), 1000)
    }
  } finally {
    loading.value = false
    await nextTick()
    initSalesChart()
    initOrderChart()
  }
}

const handleResize = () => {
  requestAnimationFrame(() => { salesChart?.resize(); orderChart?.resize() })
}

watch(salesView, () => { nextTick(initSalesChart) })

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  salesChart?.dispose()
  orderChart?.dispose()
})
</script>

<style scoped>
.dashboard { animation: fadeIn 0.35s ease; }

.page-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  margin-bottom: 24px; gap: 20px; flex-wrap: wrap;
}
.header-left { min-width: 200px; }
.greeting {
  font-size: 22px; font-weight: 700; color: var(--text-primary); margin: 0 0 4px 0;
  letter-spacing: -0.01em;
}
.date-info {
  font-size: 13px; color: var(--text-secondary); display: flex; align-items: center; gap: 5px;
  margin: 0;
}
.header-right { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.quick-stats {
  display: flex; gap: 16px; padding: 8px 16px; background: #fafafa; border-radius: 8px; border: 1px solid #f0f0f0;
}
.qs-item { font-size: 13px; color: var(--text-secondary); display: flex; align-items: center; gap: 5px; }
.qs-item strong { color: var(--text-primary); }
.qs-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; flex-shrink: 0; }
.qs-dot.green { background: #10b981; }
.qs-dot.orange { background: #f59e0b; }
.header-actions { display: flex; gap: 8px; }
.header-btn {
  display: inline-flex; align-items: center; gap: 6px; padding: 8px 16px;
  border: 1px solid var(--border); border-radius: 8px; font-size: 13px; font-weight: 500;
  cursor: pointer; transition: all 0.2s; background: var(--bg-white); color: var(--text-primary);
  white-space: nowrap;
}
.header-btn:hover { border-color: var(--color-primary-400); color: var(--color-primary-600); box-shadow: 0 2px 8px rgba(212,165,116,0.12); }
.header-btn.primary {
  background: linear-gradient(135deg, var(--color-primary-500), var(--color-primary-600));
  color: #fff; border-color: transparent;
}
.header-btn.primary:hover { transform: translateY(-1px); box-shadow: 0 4px 16px rgba(217,119,6,0.25); }
.header-btn svg { width: 15px; height: 15px; }

.loading-state { display: flex; flex-direction: column; gap: 16px; }
.loading-shimmer {
  border-radius: 12px; background: linear-gradient(90deg, #f9fafb 25%, #f3f4f6 50%, #f9fafb 75%);
  background-size: 200% 100%; animation: shimmer 1.5s infinite;
}

.stats-row {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px;
}
.stat-card {
  background: var(--bg-white); border-radius: 12px; padding: 20px 22px;
  border: 1px solid var(--border-light); box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  transition: all 0.25s ease; cursor: default; position: relative; overflow: hidden;
  animation: fadeUp 0.4s ease both;
}
.stat-card::before {
  content: ''; position: absolute; top: 0; left: 0; right: 0; height: 3px;
}
.stat-card.gold::before { background: linear-gradient(90deg, #d4a574, #e8c49a); }
.stat-card.green::before { background: linear-gradient(90deg, #10b981, #6ee7b7); }
.stat-card.orange::before { background: linear-gradient(90deg, #f59e0b, #fcd34d); }
.stat-card.blue::before { background: linear-gradient(90deg, #3b82f6, #93c5fd); }
.stat-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.08); transform: translateY(-2px); }

.sc-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.sc-icon {
  width: 38px; height: 38px; border-radius: 10px; display: flex; align-items: center; justify-content: center;
}
.gold .sc-icon { background: #fefce8; color: #d4a574; }
.green .sc-icon { background: #ecfdf5; color: #059669; }
.orange .sc-icon { background: #fffbeb; color: #d97706; }
.blue .sc-icon { background: #eff6ff; color: #2563eb; }
.sc-icon svg { width: 20px; height: 20px; }

.sc-trend {
  display: flex; align-items: center; gap: 2px; font-size: 12px; padding: 2px 8px;
  border-radius: 12px; font-weight: 600;
}
.sc-trend.up { background: #ecfdf5; color: #059669; }
.sc-trend.down { background: #fef2f2; color: #dc2626; }

.sc-value { font-size: 26px; font-weight: 700; color: var(--text-primary); letter-spacing: -0.02em; line-height: 1.2; }
.sc-label { font-size: 13px; color: var(--text-secondary); margin-top: 2px; }
.sc-sub { font-size: 11.5px; color: var(--text-muted); margin-top: 2px; }
.sc-action {
  margin-top: 10px; font-size: 12px; color: var(--color-primary-600); cursor: pointer;
  display: inline-block; transition: color 0.2s;
}
.sc-action:hover { color: var(--color-primary-700); text-decoration: underline; }

.main-content-row {
  display: grid; grid-template-columns: 1fr 300px; gap: 16px; margin-bottom: 20px;
}

.chart-card {
  background: var(--bg-white); border-radius: 12px; border: 1px solid var(--border-light);
  box-shadow: 0 1px 3px rgba(0,0,0,0.04); animation: fadeUp 0.4s 0.15s ease both;
}
.main-chart { min-width: 0; }
.mini-chart { margin-bottom: 16px; }

.cc-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 20px 0; margin-bottom: 4px;
}
.cch-left { display: flex; align-items: center; gap: 8px; }
.cc-header h3 { font-size: 15px; font-weight: 600; color: var(--text-primary); margin: 0; }
.cch-badge {
  font-size: 11px; padding: 2px 8px; background: #f0fdf4; color: #16a34a;
  border-radius: 10px; font-weight: 500;
}
.cch-right { display: flex; gap: 2px; background: #f3f4f6; border-radius: 8px; padding: 2px; }
.tab-btn {
  padding: 4px 12px; border: none; background: transparent; font-size: 12px; color: #6b7280;
  cursor: pointer; border-radius: 6px; transition: all 0.2s; font-weight: 500;
}
.tab-btn.active { background: #fff; color: var(--text-primary); box-shadow: 0 1px 3px rgba(0,0,0,0.08); }

.cc-body { padding: 0 16px 16px; }
.echart-box { width: 100%; height: 280px; }
.echart-box-sm { width: 100%; height: 180px; }

.side-panel { display: flex; flex-direction: column; min-width: 0; }

.quick-entry-card {
  background: var(--bg-white); border-radius: 12px; border: 1px solid var(--border-light);
  padding: 16px 18px; box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  animation: fadeUp 0.4s 0.25s ease both;
}
.quick-entry-card h4 { font-size: 14px; font-weight: 600; color: var(--text-primary); margin: 0 0 12px 0; }
.qe-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.qe-item {
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  padding: 12px 8px 10px; border-radius: 10px; cursor: pointer;
  transition: all 0.2s; text-decoration: none; border: 1px solid transparent;
}
.qe-item:hover { background: #fafafa; border-color: #f0f0f0; }
.qei-icon {
  width: 36px; height: 36px; border-radius: 10px; display: flex; align-items: center; justify-content: center;
  color: #78716c;
}
.qe-item span { font-size: 12px; color: var(--text-secondary); text-align: center; line-height: 1.3; }
.qei-count {
  font-size: 11px; font-weight: 700; color: var(--color-error); background: #fef2f2;
  padding: 1px 7px; border-radius: 8px; min-width: 20px; text-align: center;
}

.table-section { animation: fadeUp 0.4s 0.3s ease both; }
.table-card {
  background: var(--bg-white); border-radius: 12px; border: 1px solid var(--border-light);
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.tc-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 20px; border-bottom: 1px solid #f5f5f5;
}
.tch-left { display: flex; align-items: center; gap: 10px; }
.tc-header h3 { font-size: 15px; font-weight: 600; color: var(--text-primary); margin: 0; }
.tc-count { font-size: 12px; color: var(--text-muted); }
.tc-more {
  display: inline-flex; align-items: center; gap: 4px; font-size: 13px; color: var(--color-primary-600);
  cursor: pointer; background: none; border: none; font-weight: 500; padding: 4px 0;
  transition: color 0.2s;
}
.tc-more:hover { color: var(--color-primary-700); }

.order-table { width: 100%; border-collapse: collapse; }
.order-table th {
  text-align: left; padding: 10px 16px; font-size: 12px; font-weight: 600;
  color: var(--text-muted); background: #fafafa; border-bottom: 1px solid #f0f0f0;
  white-space: nowrap;
}
.order-table td {
  padding: 12px 16px; font-size: 13px; color: var(--text-primary);
  border-bottom: 1px solid #f8f8f8; vertical-align: middle;
}
.order-table tbody tr:hover { background: #fafbfc; }
.order-table tbody tr:last-child td { border-bottom: none; }

.empty-row td {
  text-align: center; padding: 40px 16px; color: var(--text-muted); font-size: 13px;
}
.empty-row svg { margin-bottom: 8px; opacity: 0.4; }

.td-order-no { font-family: 'SF Mono', Monaco, monospace; font-size: 12.5px; color: var(--text-secondary); }
.tp-info { display: flex; align-items: center; gap: 6px; }
.tp-name { max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tp-spec { color: var(--text-muted); font-size: 12px; flex-shrink: 0; }
.td-money { font-weight: 600; color: var(--text-primary); white-space: nowrap; }
.td-buyer { color: var(--text-secondary); max-width: 80px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.td-time { color: var(--text-muted); font-size: 12px; white-space: nowrap; }
.td-action { white-space: nowrap; }

.status-tag {
  display: inline-block; padding: 3px 10px; border-radius: 6px; font-size: 12px; font-weight: 500; white-space: nowrap;
}
.st-0 { background: #fef3c7; color: #92400e; }
.st-1 { background: #fee2e2; color: #991b1b; }
.st-2 { background: #d1fae5; color: #065f46; }
.st-3, .st-6 { background: #f3f4f6; color: #6b7280; }
.st-4 { background: #dbeafe; color: #1e40af; }
.st-5 { background: #ede9fe; color: #5b21b6; }

.action-link {
  background: none; border: none; color: var(--color-primary-600); font-size: 13px;
  cursor: pointer; padding: 2px 6px; font-weight: 500; border-radius: 4px;
  transition: all 0.15s;
}
.action-link:hover { background: var(--color-primary-50); color: var(--color-primary-700); }

@keyframes shimmer { 0% { background-position: -200% 0; } 100% { background-position: 200% 0; } }
@keyframes fadeUp { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

@media (max-width: 1200px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .main-content-row { grid-template-columns: 1fr; }
  .side-panel { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
}
@media (max-width: 768px) {
  .stats-row { grid-template-columns: 1fr 1fr; gap: 12px; }
  .page-header { flex-direction: column; }
  .header-right { width: 100%; justify-content: space-between; }
  .quick-stats { display: none; }
  .echart-box { height: 220px; }
  .qe-grid { grid-template-columns: repeat(4, 1fr); }
  .order-table { font-size: 12px; }
  .order-table th, .order-table td { padding: 8px 10px; }
}
</style>
