<template>
  <div class="order-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>订单管理</span>
        </div>
      </template>
      
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="订单号">
          <el-input v-model="queryParams.orderNo" placeholder="请输入订单号" clearable />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="全部" :value="null" />
            <el-option label="待支付" :value="0" />
            <el-option label="待发货" :value="1" />
            <el-option label="已发货" :value="2" />
            <el-option label="已完成" :value="3" />
            <el-option label="已取消" :value="4" />
            <el-option label="已退款" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
      
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="订单ID" width="80" />
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column label="买家信息" width="140">
          <template #default="{ row }">
            <div v-if="row.buyer">
              <div class="user-name">{{ row.buyer.nickname || row.buyer.username }}</div>
              <div class="user-phone">{{ row.buyer.phone || '-' }}</div>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="卖家信息" width="140">
          <template #default="{ row }">
            <div v-if="row.items && row.items.length > 0 && row.items[0].seller">
              <div class="user-name">{{ row.items[0].seller.nickname || row.items[0].seller.username }}</div>
              <div class="user-phone">{{ row.items[0].seller.shopName || '-' }}</div>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="订单金额" width="120">
          <template #default="{ row }">
            ¥{{ row.totalAmount }}
          </template>
        </el-table-column>
        <el-table-column prop="payAmount" label="实付金额" width="120">
          <template #default="{ row }">
            ¥{{ row.payAmount || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="订单状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payStatus" label="支付状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.payStatus === 1 ? 'success' : 'warning'">
              {{ row.payStatus === 1 ? '已支付' : '未支付' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="consignee" label="收货人" width="100" />
        <el-table-column prop="phone" label="收货电话" width="120" />
        <el-table-column prop="address" label="收货地址" min-width="180">
          <template #default="{ row }">
            {{ row.province }}{{ row.city }}{{ row.district }}{{ row.detail }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleDetail(row)">
              详情
            </el-button>
            <el-button link type="success" size="small" @click="handleShip(row)" v-if="row.status === 1">
              发货
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>
    
    <!-- 发货对话框 -->
    <el-dialog v-model="shipDialogVisible" title="发货操作" width="500px">
      <el-form :model="shipForm" :rules="shipRules" ref="shipFormRef" label-width="100px">
        <el-form-item label="物流公司" prop="logisticsCompany">
          <el-select v-model="shipForm.logisticsCompany" placeholder="请选择物流公司" style="width: 100%">
            <el-option label="顺丰速运" value="SF_EXPRESS" />
            <el-option label="中通快递" value="ZTO_EXPRESS" />
            <el-option label="圆通速递" value="YTO_EXPRESS" />
            <el-option label="韵达快递" value="YUNDA_EXPRESS" />
            <el-option label="申通快递" value="STO_EXPRESS" />
            <el-option label="邮政EMS" value="POST_EMS" />
          </el-select>
        </el-form-item>
        <el-form-item label="运单号" prop="trackingNumber">
          <el-input v-model="shipForm.trackingNumber" placeholder="请输入运单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleShipConfirm">确认发货</el-button>
      </template>
    </el-dialog>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="订单详细信息" :width="detailDialogWidth" destroy-on-close>
      <div class="order-detail-container">
        <!-- 订单状态提示 -->
        <el-alert
          :title="'当前订单状态：' + getStatusText(currentOrder.status)"
          :type="getOrderStatusAlertType(currentOrder.status)"
          :closable="false"
          show-icon
          style="margin-bottom: 20px;"
        />

        <!-- 买家信息区块 -->
        <el-descriptions :column="2" border class="info-section" v-if="currentOrder.buyer">
          <template #title>
            <div class="section-title">
              <el-icon><User /></el-icon> 买家信息
            </div>
          </template>
          <el-descriptions-item label="买家ID">{{ currentOrder.buyer.id }}</el-descriptions-item>
          <el-descriptions-item label="买家账号">{{ currentOrder.buyer.username }}</el-descriptions-item>
          <el-descriptions-item label="买家昵称">{{ currentOrder.buyer.nickname || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentOrder.buyer.phone || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 卖家信息区块 -->
        <el-descriptions :column="2" border class="info-section" style="margin-top: 20px;" v-if="currentOrder.items && currentOrder.items.length > 0 && currentOrder.items[0].seller">
          <template #title>
            <div class="section-title">
              <el-icon><Shop /></el-icon> 卖家信息
            </div>
          </template>
          <el-descriptions-item label="卖家ID">{{ currentOrder.items[0].seller.id }}</el-descriptions-item>
          <el-descriptions-item label="卖家账号">{{ currentOrder.items[0].seller.username }}</el-descriptions-item>
          <el-descriptions-item label="店铺名称">{{ currentOrder.items[0].seller.shopName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentOrder.items[0].seller.phone || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 订单基本信息 -->
        <el-descriptions :column="2" border class="info-section" style="margin-top: 20px;">
          <template #title>
            <div class="section-title">
              <el-icon><Document /></el-icon> 订单信息
            </div>
          </template>
          <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ currentOrder.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="getStatusType(currentOrder.status)">
              {{ getStatusText(currentOrder.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="支付方式">
            {{ currentOrder.paymentMethod === 1 ? '微信支付' : currentOrder.paymentMethod === 2 ? '支付宝' : '未支付' }}
          </el-descriptions-item>
          <el-descriptions-item label="商品总额">¥{{ (currentOrder.totalAmount || 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="优惠金额">-¥{{ (currentOrder.discountAmount || 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="实付金额" v-if="currentOrder.paymentStatus === 1">
            <span style="color: var(--color-error); font-weight: bold;">¥{{ (currentOrder.payAmount || 0).toFixed(2) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentOrder.remark || '无' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 收货地址信息 -->
        <el-descriptions :column="1" border class="info-section" style="margin-top: 20px;">
          <template #title>
            <div class="section-title">
              <el-icon><Location /></el-icon> 收货信息
            </div>
          </template>
          <el-descriptions-item label="收货人">{{ currentOrder.receiverName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentOrder.receiverPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收货地址">{{ currentOrder.receiverAddress || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 物流信息（已发货后显示） -->
        <el-descriptions :column="1" border class="info-section" style="margin-top: 20px;" v-if="currentOrder.status >= 3">
          <template #title>
            <div class="section-title">
              <el-icon><Van /></el-icon> 物流信息
            </div>
          </template>
          <el-descriptions-item label="物流公司">{{ getLogisticsCompanyText(currentOrder.logisticsCompany) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="运单号">{{ currentOrder.trackingNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发货时间">{{ currentOrder.shippedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="预计送达">{{ currentOrder.expectedDeliveryAt || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 订单项列表（带卖家信息） -->
        <div class="info-section" style="margin-top: 20px;">
          <div class="section-title">
            <el-icon><Goods /></el-icon> 商品明细
          </div>
          <el-table :data="currentOrder.items || []" size="small" stripe border>
            <el-table-column label="商品图片" width="100">
              <template #default="{ row }">
                <el-image
                  :src="row.imageUrl"
                  fit="cover"
                  style="width: 80px; height: 60px; border-radius: 4px;"
                >
                  <template #error>
                    <div class="image-error">暂无图片</div>
                  </template>
                </el-image>
              </template>
            </el-table-column>
            <el-table-column prop="productName" label="商品名称" min-width="150" show-overflow-tooltip />
            <el-table-column prop="specName" label="规格" width="120" />
            <el-table-column prop="price" label="单价(元)" width="90">
              <template #default="{ row }">
                ¥{{ row.price?.toFixed(2) }}
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="70" align="center" />
            <el-table-column label="小计(元)" width="100">
              <template #default="{ row }">
                <span style="font-weight: bold; color: var(--color-text-primary);">
                  ¥{{ ((row.price || 0) * (row.quantity || 0)).toFixed(2) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="卖家" width="120">
              <template #default="{ row }">
                <div v-if="row.seller">
                  <div>{{ row.seller.shopName || row.seller.nickname || row.seller.username }}</div>
                </div>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 操作按钮区域 -->
        <div class="order-actions" style="margin-top: 20px;">
          <el-button
            type="primary"
            @click="handleShipFromDetail"
            v-if="currentOrder.status === 2"
          >
            立即发货
          </el-button>
          <el-button type="danger" @click="handleCancelFromDetail" v-if="currentOrder.status <= 2">
            取消订单
          </el-button>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getOrderList, shipOrder } from '@/api/order'
import { User, Shop } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const shipDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const shipFormRef = ref()
const currentOrder = ref<any>({})

const windowWidth = ref(window.innerWidth)
const onResize = () => { windowWidth.value = window.innerWidth }
onMounted(() => { window.addEventListener('resize', onResize) })
onBeforeUnmount(() => { window.removeEventListener('resize', onResize) })
const shipDialogWidth = computed(() => windowWidth.value < 768 ? '90%' : '500px')
const detailDialogWidth = computed(() => windowWidth.value < 768 ? '90%' : '900px')

const queryParams = reactive({
  page: 1,
  pageSize: 20,
  status: null,
  orderNo: ''
})

const shipForm = reactive({
  orderId: 0,
  logisticsCompany: '',
  trackingNo: ''
})

const shipRules = {
  logisticsCompany: [
    { required: true, message: '请输入物流公司', trigger: 'blur' }
  ],
  trackingNo: [
    { required: true, message: '请输入物流单号', trigger: 'blur' }
  ]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getOrderList(queryParams)
    const data = res.data || res
    tableData.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    console.error('获取订单列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.page = 1
  fetchData()
}

const handleReset = () => {
  queryParams.status = null
  queryParams.orderNo = ''
  queryParams.page = 1
  fetchData()
}

const getStatusText = (status: number) => {
  const statusMap = {
    0: '待支付',
    1: '待发货',
    2: '已发货',
    3: '已完成',
    4: '已取消',
    5: '已退款'
  }
  return statusMap[status] || '未知'
}

const getStatusType = (status: number) => {
  const typeMap = {
    0: 'warning',
    1: 'info',
    2: 'primary',
    3: 'success',
    4: 'danger',
    5: 'danger'
  }
  return typeMap[status] || 'info'
}

const handleDetail = (row: any) => {
  currentOrder.value = { ...row }
  detailDialogVisible.value = true
}

const handleShipFromDetail = () => {
  shipForm.value.orderId = currentOrder.value.id
  detailDialogVisible.value = false
  shipDialogVisible.value = true
}

const handleCancelFromDetail = async () => {
  try {
    await ElMessageBox.confirm(
      '确认要取消该订单吗？此操作不可恢复',
      '警告',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    
    await cancelOrder(currentOrder.value.id)
    ElMessage.success('订单已取消')
    detailDialogVisible.value = false
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消订单失败:', error)
    }
  }
}

const getOrderStatusAlertType = (status: number) => {
  const map: Record<number, string> = {
    0: 'info',
    1: 'warning',
    2: '',
    3: 'primary',
    4: 'success',
    5: 'danger'
  }
  return (map[status] || 'info') as any
}

const getLogisticsCompanyText = (company: string) => {
  const map: Record<string, string> = {
    SF_EXPRESS: '顺丰速运',
    ZTO_EXPRESS: '中通快递',
    YTO_EXPRESS: '圆通速递',
    YUNDA_EXPRESS: '韵达快递',
    STO_EXPRESS: '申通快递',
    POST_EMS: '邮政EMS'
  }
  return map[company] || company || '-'
}

const handleShip = (row: any) => {
  shipForm.orderId = row.id
  shipForm.logisticsCompany = ''
  shipForm.trackingNo = ''
  shipDialogVisible.value = true
}

const handleShipConfirm = async () => {
  if (!shipFormRef.value) return
  
  await shipFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await shipOrder(shipForm.orderId, shipForm.logisticsCompany, shipForm.trackingNo)
        ElMessage.success('发货成功')
        shipDialogVisible.value = false
        fetchData()
      } catch (error) {
        console.error('发货失败:', error)
      }
    }
  })
}

const handleSizeChange = (val: number) => {
  queryParams.pageSize = val
  fetchData()
}

const handleCurrentChange = (val: number) => {
  queryParams.page = val
  fetchData()
}

fetchData()
</script>

<style scoped>
.order-container {
  padding: 20px;
}

.search-form {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-detail-container {
  max-height: 65vh;
  overflow-y: auto;
}

.info-section {
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 2px solid var(--color-primary-500);
}

.image-error {
  width: 80px;
  height: 60px;
  background-color: var(--color-bg-page);
  color: var(--color-text-tertiary);
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}

.user-name {
  font-weight: 500;
  color: var(--color-text-primary);
}

.user-phone {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-top: 2px;
}

@media (max-width: 768px) {
  .order-container {
    padding: 12px;
  }
  .search-form :deep(.el-form-item) {
    margin-right: 0;
    width: 100%;
  }
  .search-form :deep(.el-input),
  .search-form :deep(.el-select) {
    width: 100% !important;
  }
  :deep(.el-table) {
    font-size: 12px;
  }
  :deep(.el-table th),
  :deep(.el-table td) {
    padding: 6px 0;
  }
  :deep(.el-pagination) {
    flex-wrap: wrap;
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .order-container {
    padding: 8px;
  }
  :deep(.el-card__body) {
    padding: 10px;
  }
}
</style>
