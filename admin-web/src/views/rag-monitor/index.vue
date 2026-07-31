<template>
  <div class="rag-monitor">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>RAG 智能客服监控</h2>
      <el-button type="primary" @click="refreshAll" :loading="loading">
        <el-icon><Refresh /></el-icon> 刷新数据
      </el-button>
    </div>

    <!-- RAG 测试区域 -->
    <el-card class="test-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>🧪 RAG 功能测试</span>
          <el-tag :type="(dashboard?.hitRate ?? 0) > 0 ? 'success' : 'info'" size="large">
            {{ (dashboard?.hitRate ?? 0) > 0 ? 'RAG 已启用' : 'RAG 未活跃' }}
          </el-tag>
        </div>
      </template>
      <div class="test-section">
        <el-input
          v-model="testQuery"
          placeholder="输入问题测试 RAG 检索效果，如：退款政策是什么？"
          size="large"
          @keyup.enter="runTest"
        >
          <template #append>
            <el-button type="primary" @click="runTest" :loading="testing">
              测试
            </el-button>
          </template>
        </el-input>

        <!-- 测试结果 -->
        <div v-if="testResult" class="test-result">
          <el-alert
            :title="testResult.ragEnabled ? '✅ RAG 已生效 - 回答基于知识库检索' : '⚠️ RAG 未启用'"
            :type="testResult.ragEnabled ? 'success' : 'warning'"
            :closable="false"
            show-icon
          />
          <el-descriptions :column="4" border size="small" class="test-metrics">
            <el-descriptions-item label="检索得分">
              <el-tag :type="testResult.retrievalScore > 0.6 ? 'success' : testResult.retrievalScore > 0.3 ? 'warning' : 'danger'">
                {{ (testResult.retrievalScore * 100).toFixed(1) }}%
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="检索耗时">{{ testResult.retrievalTimeMs }}ms</el-descriptions-item>
            <el-descriptions-item label="总响应时间">{{ testResult.responseTimeMs }}ms</el-descriptions-item>
            <el-descriptions-item label="知识来源数">{{ testResult.sourceCount }} 条</el-descriptions-item>
          </el-descriptions>

          <!-- 知识来源 -->
          <div v-if="testResult.sources && testResult.sources.length > 0" class="sources-section">
            <h4>📚 知识来源溯源</h4>
            <el-table :data="testResult.sources" size="small" border>
              <el-table-column prop="type" label="类型" width="120">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.type === 'faq' ? 'warning' : 'primary'">
                    {{ row.type === 'faq' ? 'FAQ问答' : '知识文档' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="title" label="标题" min-width="150" />
              <el-table-column prop="score" label="匹配度" width="100">
                <template #default="{ row }">
                  {{ (row.score * 100).toFixed(1) }}%
                </template>
              </el-table-column>
              <el-table-column prop="snippet" label="内容摘要" min-width="250" show-overflow-tooltip />
            </el-table>
          </div>

          <!-- AI 回答 -->
          <div class="answer-section">
            <h4>🤖 AI 回答</h4>
            <div class="answer-content">{{ testResult.response }}</div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 核心指标卡片 -->
    <el-row :gutter="16" class="metrics-row">
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-value" :class="getHealthClass(dashboard?.healthScore)">
            {{ dashboard?.healthScore?.toFixed(1) || '0' }}
          </div>
          <div class="metric-label">系统健康度</div>
          <el-progress :percentage="dashboard?.healthScore || 0" :show-text="false" 
            :color="getHealthColor(dashboard?.healthScore)" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-value">{{ ((dashboard?.hitRate || 0) * 100).toFixed(1) }}%</div>
          <div class="metric-label">知识库命中率</div>
          <el-progress :percentage="(dashboard?.hitRate || 0) * 100" :show-text="false" color="#67c23a" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-value">{{ ((dashboard?.llmSuccessRate || 0) * 100).toFixed(1) }}%</div>
          <div class="metric-label">LLM 成功率</div>
          <el-progress :percentage="(dashboard?.llmSuccessRate || 0) * 100" :show-text="false" color="#409eff" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-value">{{ dashboard?.avgRetrievalTimeMs?.toFixed(1) || '0' }}ms</div>
          <div class="metric-label">平均检索耗时</div>
          <el-progress :percentage="Math.min(100, (dashboard?.avgRetrievalTimeMs || 0) / 10)" :show-text="false" color="#e6a23c" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 详细面板 -->
    <el-row :gutter="16">
      <!-- 向量检索信息 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>🔍 向量检索引擎</span>
          </template>
          <el-descriptions :column="2" border size="small" v-if="vectorInfo">
            <el-descriptions-item label="Embedding 模型">
              {{ vectorInfo.embeddingModel }}
            </el-descriptions-item>
            <el-descriptions-item label="向量维度">
              {{ vectorInfo.embeddingDimension }}
            </el-descriptions-item>
            <el-descriptions-item label="外部 API">
              <el-tag :type="vectorInfo.usingExternalApi ? 'success' : 'warning'" size="small">
                {{ vectorInfo.usingExternalApi ? '已连接' : '本地 TF-IDF 降级' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="HNSW 索引">
              <el-tag :type="vectorInfo.searchStrategy?.chunkHnswEnabled ? 'success' : 'info'" size="small">
                {{ vectorInfo.searchStrategy?.chunkHnswEnabled ? '已启用' : '暴力搜索' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="知识分块数">
              {{ vectorInfo.searchStrategy?.chunkCount || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="FAQ 数量">
              {{ vectorInfo.searchStrategy?.faqCount || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="领域词典">
              {{ vectorInfo.lexiconInfo?.domainLexiconSize || 0 }} 词
            </el-descriptions-item>
            <el-descriptions-item label="同义词">
              {{ vectorInfo.lexiconInfo?.synonymEntries || 0 }} 组
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 知识库覆盖率 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>📊 知识库覆盖率</span>
              <el-button size="small" @click="handleInitFaqs" :loading="initFaqsLoading">
                初始化种子FAQ
              </el-button>
            </div>
          </template>
          <div v-if="coverage" class="coverage-section">
            <div class="coverage-chart">
              <el-progress
                type="dashboard"
                :percentage="(coverage.coverageRate * 100)"
                :color="coverage.coverageRate >= coverage.alertThreshold ? '#67c23a' : '#f56c6c'"
                :width="150"
              >
                <template #default="{ percentage }">
                  <span class="percentage-value">{{ percentage.toFixed(0) }}%</span>
                  <span class="percentage-label">覆盖率</span>
                </template>
              </el-progress>
            </div>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="FAQ 总数">{{ coverage.faqCount }}</el-descriptions-item>
              <el-descriptions-item label="关键词总数">{{ coverage.totalKeywords }}</el-descriptions-item>
              <el-descriptions-item label="已覆盖">{{ coverage.coveredCount }}</el-descriptions-item>
              <el-descriptions-item label="未覆盖">
                <el-tag :type="coverage.uncoveredCount > 0 ? 'danger' : 'success'" size="small">
                  {{ coverage.uncoveredCount }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
            <el-alert
              v-if="coverage.needAlert"
              title="知识库覆盖率低于阈值，建议补充知识"
              type="warning"
              :closable="false"
              show-icon
              style="margin-top: 12px"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Embedding 质量 & 意图分布 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <!-- Embedding 质量 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>🎯 Embedding 语义质量</span>
              <el-button size="small" @click="loadEmbeddingQuality" :loading="qualityLoading">
                运行测试
              </el-button>
            </div>
          </template>
          <div v-if="embeddingQuality">
            <el-alert
              :title="embeddingQuality.passed ? '✅ 语义质量达标' : '⚠️ 语义质量未达标'"
              :type="embeddingQuality.passed ? 'success' : 'warning'"
              :closable="false"
              show-icon
              style="margin-bottom: 12px"
            />
            <el-descriptions :column="3" border size="small">
              <el-descriptions-item label="相似文本均值">
                {{ embeddingQuality.positiveAvg.toFixed(3) }}
              </el-descriptions-item>
              <el-descriptions-item label="不同文本均值">
                {{ embeddingQuality.negativeAvg.toFixed(3) }}
              </el-descriptions-item>
              <el-descriptions-item label="区分度">
                <el-tag :type="embeddingQuality.discrimination >= 0.2 ? 'success' : 'danger'" size="small">
                  {{ embeddingQuality.discrimination.toFixed(3) }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </div>
          <el-empty v-else description="点击「运行测试」评估 Embedding 质量" :image-size="80" />
        </el-card>
      </el-col>

      <!-- 意图分布 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>📈 意图分类分布</span>
          </template>
          <div v-if="intentStats">
            <div class="intent-list">
              <div v-for="(item, key) in intentStats.intents" :key="key" class="intent-item">
                <div class="intent-header">
                  <span>{{ item.label }}</span>
                  <span>{{ item.count }} 次 ({{ item.percentage.toFixed(1) }}%)</span>
                </div>
                <el-progress :percentage="item.percentage" :show-text="false" 
                  :color="getIntentColor(key as string)" />
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无意图统计数据" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 高频查询 & 最近查询 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>🔥 高频查询 Top 10</span>
          </template>
          <el-table :data="topQueries" size="small" border v-if="topQueries.length > 0">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="query" label="查询内容" />
            <el-table-column prop="count" label="次数" width="80" />
          </el-table>
          <el-empty v-else description="暂无查询记录" :image-size="60" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>🕐 最近查询</span>
          </template>
          <div class="recent-queries" v-if="dashboard?.recentQueries?.length">
            <el-tag
              v-for="(q, i) in dashboard.recentQueries"
              :key="i"
              size="small"
              class="query-tag"
            >
              {{ q }}
            </el-tag>
          </div>
          <el-empty v-else description="暂无最近查询" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <!-- AI 对话日志 -->
    <el-card shadow="hover" style="margin-top: 16px">
      <template #header>
        <div class="card-header">
          <span>💬 AI 对话日志</span>
          <div>
            <el-select v-model="logFilter.serviceType" placeholder="服务类型" clearable size="small" style="width: 120px; margin-right: 8px" @change="loadAILogs">
              <el-option label="商品咨询" :value="1" />
              <el-option label="物流查询" :value="2" />
              <el-option label="售后咨询" :value="3" />
            </el-select>
            <el-button size="small" @click="loadAILogs" :loading="logsLoading">刷新</el-button>
          </div>
        </div>
      </template>
      <el-table :data="aiLogs" size="small" border v-loading="logsLoading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="userId" label="用户ID" width="70" />
        <el-table-column prop="query" label="用户提问" min-width="180" show-overflow-tooltip />
        <el-table-column prop="response" label="AI 回答" min-width="250" show-overflow-tooltip />
        <el-table-column prop="serviceType" label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.serviceType === 1 ? 'primary' : row.serviceType === 2 ? 'warning' : 'success'">
              {{ row.serviceType === 1 ? '商品' : row.serviceType === 2 ? '物流' : '售后' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="160" />
        <el-table-column label="操作" width="70" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" size="small" link @click="handleDeleteLog(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper" v-if="logTotal > 0">
        <el-pagination
          v-model:current-page="logPage"
          :page-size="10"
          :total="logTotal"
          layout="total, prev, pager, next"
          @current-change="loadAILogs"
        />
      </div>
      <el-empty v-if="aiLogs.length === 0 && !logsLoading" description="暂无对话记录" :image-size="60" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  getRagDashboard,
  getVectorSearchInfo,
  getEmbeddingQuality,
  getIntentStatistics,
  getKnowledgeCoverage,
  initSeedFaqs,
  testRagQuery,
  getAILogs,
  deleteAILog,
  type RagDashboard,
  type VectorSearchInfo,
  type EmbeddingQuality,
  type IntentStatistics,
  type KnowledgeCoverage,
  type TopQuery,
  type RagTestResult,
  type AILog
} from '@/api/ragMonitor'

// 数据
const loading = ref(false)
const testing = ref(false)
const qualityLoading = ref(false)
const initFaqsLoading = ref(false)

const dashboard = ref<RagDashboard | null>(null)
const vectorInfo = ref<VectorSearchInfo | null>(null)
const embeddingQuality = ref<EmbeddingQuality | null>(null)
const intentStats = ref<IntentStatistics | null>(null)
const coverage = ref<KnowledgeCoverage | null>(null)
const topQueries = ref<TopQuery[]>([])

const testQuery = ref('')
const testResult = ref<RagTestResult | null>(null)

// AI 日志数据
const aiLogs = ref<AILog[]>([])
const logTotal = ref(0)
const logPage = ref(1)
const logsLoading = ref(false)
const logFilter = ref<{ serviceType?: number }>({})

// 加载所有数据
const refreshAll = async () => {
  loading.value = true
  try {
    await Promise.all([
      loadDashboard(),
      loadVectorInfo(),
      loadIntentStats(),
      loadCoverage()
    ])
    ElMessage.success('数据刷新成功')
  } catch (e) {
    ElMessage.error('部分数据加载失败')
  } finally {
    loading.value = false
  }
}

const loadDashboard = async () => {
  try {
    const data: any = await getRagDashboard()
    dashboard.value = data?.dashboard || null
    topQueries.value = data?.topQueries || []
  } catch (e) {
    console.error('加载仪表盘失败', e)
  }
}

const loadVectorInfo = async () => {
  try {
    vectorInfo.value = await getVectorSearchInfo() as any
  } catch (e) {
    console.error('加载向量检索信息失败', e)
  }
}

const loadEmbeddingQuality = async () => {
  qualityLoading.value = true
  try {
    embeddingQuality.value = await getEmbeddingQuality() as any
  } catch (e) {
    ElMessage.error('Embedding 质量测试失败')
  } finally {
    qualityLoading.value = false
  }
}

const loadIntentStats = async () => {
  try {
    intentStats.value = await getIntentStatistics() as any
  } catch (e) {
    console.error('加载意图统计失败', e)
  }
}

const loadCoverage = async () => {
  try {
    coverage.value = await getKnowledgeCoverage() as any
  } catch (e) {
    console.error('加载覆盖率失败', e)
  }
}

const handleInitFaqs = async () => {
  initFaqsLoading.value = true
  try {
    coverage.value = await initSeedFaqs() as any
    ElMessage.success('种子FAQ初始化成功')
  } catch (e) {
    ElMessage.error('初始化失败')
  } finally {
    initFaqsLoading.value = false
  }
}

// RAG 测试
const runTest = async () => {
  if (!testQuery.value.trim()) {
    ElMessage.warning('请输入测试问题')
    return
  }
  testing.value = true
  testResult.value = null
  try {
    testResult.value = await testRagQuery(testQuery.value) as any
    // 刷新仪表盘和日志
    loadDashboard()
    loadAILogs()
  } catch (e: any) {
    ElMessage.error('测试失败: ' + (e.message || '未知错误'))
  } finally {
    testing.value = false
  }
}

// AI 日志
const loadAILogs = async () => {
  logsLoading.value = true
  try {
    const data: any = await getAILogs({
      page: logPage.value,
      size: 10,
      serviceType: logFilter.value.serviceType
    })
    aiLogs.value = data?.records || []
    logTotal.value = data?.total || 0
  } catch (e) {
    console.error('加载AI日志失败', e)
  } finally {
    logsLoading.value = false
  }
}

const handleDeleteLog = async (id: number) => {
  try {
    await deleteAILog(id)
    ElMessage.success('删除成功')
    loadAILogs()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

// 辅助方法
const getHealthClass = (score?: number) => {
  if (!score) return ''
  if (score >= 80) return 'health-good'
  if (score >= 60) return 'health-warning'
  return 'health-danger'
}

const getHealthColor = (score?: number) => {
  if (!score) return '#909399'
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

const getIntentColor = (intent: string) => {
  const colors: Record<string, string> = {
    PRODUCT_QUERY: '#409eff',
    FAQ_CONSULT: '#67c23a',
    TECH_SUPPORT: '#e6a23c',
    COMPLAINT: '#f56c6c',
    CHITCHAT: '#909399'
  }
  return colors[intent] || '#409eff'
}

onMounted(() => {
  refreshAll()
  loadAILogs()
})
</script>

<style scoped>
.rag-monitor {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 22px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.test-card {
  margin-bottom: 20px;
}

.test-section {
  max-width: 800px;
}

.test-result {
  margin-top: 20px;
}

.test-metrics {
  margin-top: 12px;
}

.sources-section {
  margin-top: 16px;
}

.sources-section h4,
.answer-section h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #606266;
}

.answer-section {
  margin-top: 16px;
}

.answer-content {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.metrics-row {
  margin-bottom: 16px;
}

.metric-card {
  text-align: center;
}

.metric-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.metric-value.health-good { color: #67c23a; }
.metric-value.health-warning { color: #e6a23c; }
.metric-value.health-danger { color: #f56c6c; }

.metric-label {
  font-size: 13px;
  color: #909399;
  margin: 4px 0 8px;
}

.coverage-section {
  text-align: center;
}

.coverage-chart {
  margin-bottom: 16px;
}

.percentage-value {
  font-size: 24px;
  font-weight: bold;
  display: block;
}

.percentage-label {
  font-size: 12px;
  color: #909399;
}

.intent-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.intent-item {
  padding: 8px 0;
}

.intent-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 13px;
}

.recent-queries {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.query-tag {
  margin: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
