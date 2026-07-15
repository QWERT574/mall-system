<template>
  <div class="product-detail-wrapper">
    <div v-if="product" class="product-detail-container">
      <div class="product-top-layout">
        <div class="product-image-section">
          <img :src="product.cover" :alt="product.name" class="product-main-image" />
        </div>

        <div class="product-info-section">
          <h1 class="product-title">{{ product.name }}</h1>
          <p class="product-desc">{{ product.description }}</p>

          <div class="product-price-section">
            <div class="price-line">
              <span class="product-price">¥{{ displayPrice }}</span>
              <span class="product-original-price" v-if="hasActivityDiscount">¥{{ product.price }}</span>
              <span class="discount-tag" v-if="hasActivityDiscount">
                {{ activityDiscountLabel }}
              </span>
            </div>
            <span class="product-stock">库存：{{ product.stock }}</span>
          </div>

          <div class="product-coupons" v-if="productCoupons.length > 0">
            <span class="coupon-section-label">可用优惠券</span>
            <div class="coupon-mini-list">
              <div class="coupon-mini" v-for="c in productCoupons" :key="c.id">
                <span class="coupon-mini-tag">{{ c.type === 1 ? '满减' : '折扣' }}</span>
                <span class="coupon-mini-text" v-if="c.type === 1">满{{ c.threshold }}减{{ c.discountValue }}</span>
                <span class="coupon-mini-text" v-else>{{ c.discountValue }}折</span>
              </div>
            </div>
            <button class="goto-coupons-btn" @click="$router.push('/coupons')">领券中心 →</button>
          </div>

          <div class="product-actions">
            <button
              class="add-to-cart-btn"
              @click="addToCart(product)"
              :disabled="product.stock <= 0"
            >
              {{ product.stock > 0 ? '加入购物车' : '已售罄' }}
            </button>
            <button class="buy-now-btn" @click="buyNow(product)" :disabled="product.stock <= 0">
              立即购买
            </button>
          </div>

          <div class="product-meta">
            <div class="meta-item">
              <span class="meta-label">销量</span>
              <span class="meta-value">{{ product.sales || 0 }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">库存</span>
              <span class="meta-value">{{ product.stock }}</span>
            </div>
          </div>

          <div class="seller-section" v-if="seller">
            <h3 class="section-title">商家信息</h3>
            <div class="seller-info">
              <div class="seller-item">
                <span class="label">商家名称：</span>
                <span class="value">{{ seller.name || '未知商家' }}</span>
              </div>
              <div class="seller-item" v-if="seller.contact">
                <span class="label">联系人：</span>
                <span class="value">{{ seller.contact }}</span>
              </div>
              <div class="seller-item" v-if="seller.phone">
                <span class="label">联系电话：</span>
                <span class="value">{{ seller.phone }}</span>
              </div>
              <div class="seller-item" v-if="seller.isVerified !== undefined">
                <span class="label">认证状态：</span>
                <span class="value" :class="seller.isVerified === 1 ? 'verified' : 'unverified'">
                  {{ seller.isVerified === 1 ? '✓ 已认证' : '✗ 未认证' }}
                </span>
              </div>
            </div>

            <div class="after-sale-actions">
              <button class="action-btn" @click="contactSeller">
                <el-icon><ChatDotRound /></el-icon>
                联系商家
              </button>
              <button class="action-btn" @click="contactAdmin">
                <el-icon><Service /></el-icon>
                联系管理员
              </button>
              <button class="action-btn warning" @click="showInterventionDialog = true">
                <el-icon><Warning /></el-icon>
                申请管理员介入
              </button>
            </div>
          </div>

          <div class="write-review-section">
            <button class="write-review-btn" @click="showWriteReviewDialog = true">
              <el-icon><Edit /></el-icon>
              发表评价
            </button>
          </div>
        </div>
      </div>

      <div class="product-detail-section">
        <h3 class="section-title">商品详情</h3>
        <div class="detail-content" v-html="product.detail || '暂无详情'"></div>
      </div>

      <ReviewSection
        ref="reviewSectionRef"
        :productId="productId"
        @previewImage="previewImage"
      />
    </div>

    <div v-else class="loading">
      <p>加载中...</p>
    </div>

    <CsDialog
      v-model="showCsDialog"
      :title="csDialogTitle"
      :chatType="currentChatType"
      :seller="seller"
      :productId="productId"
    />

    <InterventionDialog
      v-model="showInterventionDialog"
      :seller="seller"
      :productId="productId"
      :productName="product?.name"
    />

    <WriteReviewDialog
      v-model="showWriteReviewDialog"
      :productId="productId"
      :currentUserId="currentUserId"
      @submitted="handleReviewSubmitted"
    />

    <el-image-viewer
      v-if="showImageViewer"
      :url-list="[currentPreviewImage]"
      @close="showImageViewer = false"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Service, Warning, Edit } from '@element-plus/icons-vue'
import api from '@/utils/api'
import CsDialog from '@/components/CsDialog.vue'
import InterventionDialog from '@/components/InterventionDialog.vue'
import WriteReviewDialog from '@/components/WriteReviewDialog.vue'
import ReviewSection from '@/components/ReviewSection.vue'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()

const product = ref(null)
const seller = ref(null)
const productId = route.params.id
const currentUserId = ref(null)
const discountActivities = ref([])
const productCoupons = ref([])
const displayPrice = ref('0')
const hasActivityDiscount = ref(false)
const activityDiscountLabel = ref('')

const showCsDialog = ref(false)
const csDialogTitle = ref('在线客服')
const currentChatType = ref('merchant')

const showInterventionDialog = ref(false)
const showWriteReviewDialog = ref(false)

const showImageViewer = ref(false)
const currentPreviewImage = ref('')

const reviewSectionRef = ref(null)

const loadProduct = async () => {
  try {
    const userInfo = userStore.userInfo
    if (userInfo && userInfo.id) {
      currentUserId.value = userInfo.id
    } else {
      currentUserId.value = null
    }

    const response = await api.getProductDetail(productId)
    if (response.code === 0) {
      product.value = response.data
      // 商品主体加载完后,商家/活动/优惠券三个接口并行拉取,避免串行等待
      const sellerId = product.value.sellerId
      await Promise.allSettled([
        sellerId ? loadSellerInfo(sellerId) : Promise.resolve(),
        loadProductActivities(),
        loadProductCoupons()
      ])
    }
  } catch (error) {
    console.error('加载商品详情失败:', error)
  }
}

const loadProductActivities = async () => {
  try {
    const response = await api.get('/discount/active-with-products')
    if (response.code === 0 && response.data) {
      for (const act of response.data) {
        if (act.products) {
          const found = act.products.find(p => p.productId === parseInt(productId))
          if (found) {
            discountActivities.value = [act]
            displayPrice.value = found.discountPrice
            hasActivityDiscount.value = true
            if (act.type === 1) activityDiscountLabel.value = '满减'
            else if (act.type === 2) activityDiscountLabel.value = act.discountRate + '折'
            else if (act.type === 3) activityDiscountLabel.value = '秒杀'
            return
          }
        }
      }
    }
    displayPrice.value = product.value?.price || '0'
    hasActivityDiscount.value = false
  } catch (e) {}
}

const loadProductCoupons = async () => {
  try {
    const response = await api.get('/coupon/available?page=1&size=5')
    if (response.code === 0 && response.data?.records) {
      productCoupons.value = response.data.records.filter(c =>
        c.type === 1 ? c.threshold <= parseFloat(product.value?.price || 0) : true
      ).slice(0, 3)
    }
  } catch (e) {}
}

const loadSellerInfo = async (sellerId) => {
  try {
    const response = await api.getSellerInfo(sellerId)
    if (response.code === 0) {
      seller.value = response.data
    }
  } catch (error) {
    console.error('加载商家信息失败:', error)
  }
}

const contactSeller = () => {
  if (seller.value && seller.value.id) {
    router.push({ path: '/service', query: { sellerId: seller.value.id, productId: productId } })
  } else if (product.value && product.value.sellerId) {
    router.push({ path: '/service', query: { sellerId: product.value.sellerId, productId: productId } })
  } else {
    router.push('/service')
  }
}

const contactAdmin = () => {
  currentChatType.value = 'admin'
  csDialogTitle.value = '联系管理员'
  showCsDialog.value = true
}

const addToCart = (product) => {
  cartStore.addToCart({
    id: product.id,
    name: product.name,
    price: product.price,
    cover: product.cover,
    stock: product.stock
  })
  ElMessage.success({ message: '已添加到购物车', duration: 2000, showClose: true })
}

const buyNow = (product) => {
  cartStore.addToCart({
    id: product.id,
    name: product.name,
    price: product.price,
    cover: product.cover,
    stock: product.stock
  })
  router.push('/cart')
}

const previewImage = (url) => {
  currentPreviewImage.value = url
  showImageViewer.value = true
}

const handleReviewSubmitted = () => {
  if (reviewSectionRef.value) {
    reviewSectionRef.value.refresh()
  }
}

onMounted(() => {
  loadProduct()
})
</script>

<style scoped>
.product-detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.product-top-layout {
  display: flex;
  gap: 30px;
  margin-bottom: 30px;
}

.product-image-section {
  flex: 0 0 480px;
}

.product-main-image {
  width: 100%;
  max-height: 500px;
  object-fit: cover;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.product-info-section {
  flex: 1;
  background: white;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.product-title {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
}

.product-desc {
  font-size: 16px;
  color: #666;
  margin-bottom: 20px;
  line-height: 1.6;
}

.product-price-section {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.product-price {
  font-size: 36px;
  font-weight: 600;
  color: #ff6b6b;
}

.product-stock {
  font-size: 16px;
  color: #999;
}

.price-line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.product-original-price {
  font-size: 18px;
  color: #bbb;
  text-decoration: line-through;
}

.discount-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, var(--color-error), #dc2626);
}

.product-coupons {
  margin: 12px 0 16px;
  padding: 12px 16px;
  background: #fff8f0;
  border-radius: var(--radius);
  border: 1px solid #fde68a;
}

.coupon-section-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary-600);
  margin-bottom: 8px;
  display: block;
}

.coupon-mini-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.coupon-mini {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #fff;
  border: 1px dashed #fbbf24;
  border-radius: 6px;
  font-size: 12px;
}

.coupon-mini-tag {
  background: var(--color-error);
  color: #fff;
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 600;
}

.coupon-mini-text {
  color: var(--color-primary-600);
  font-weight: 600;
}

.goto-coupons-btn {
  font-size: 12px;
  color: var(--color-primary-600);
  background: none;
  border: none;
  cursor: pointer;
  font-weight: 600;
  padding: 0;
}

.goto-coupons-btn:hover { text-decoration: underline; }

.product-actions {
  display: flex;
  gap: 15px;
  margin-bottom: 30px;
}

.add-to-cart-btn,
.buy-now-btn {
  flex: 1;
  padding: 15px 30px;
  font-size: 18px;
  font-weight: 600;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.add-to-cart-btn {
  background: #4CAF50;
  color: white;
}

.add-to-cart-btn:hover:not(:disabled) {
  background: #45a049;
}

.add-to-cart-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.buy-now-btn {
  background: #ff6b6b;
  color: white;
}

.buy-now-btn:hover:not(:disabled) {
  background: #ff5252;
}

.buy-now-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.product-meta {
  display: flex;
  gap: 30px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.meta-label {
  font-size: 14px;
  color: #999;
}

.meta-value {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.product-detail-section {
  background: white;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.section-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px solid #4CAF50;
}

.detail-content {
  font-size: 16px;
  line-height: 1.8;
  color: #666;
}

.seller-section {
  background: #f8f9fa;
  padding: 25px;
  border-radius: 8px;
  margin-top: 25px;
  border: 1px solid #e9ecef;
}

.seller-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.seller-item {
  display: flex;
  gap: 10px;
  font-size: 15px;
  line-height: 1.5;
}

.seller-item .label {
  color: #6c757d;
  font-weight: 500;
  min-width: 80px;
}

.seller-item .value {
  color: #212529;
  font-weight: 600;
}

.seller-item .verified {
  color: #28a745;
}

.seller-item .unverified {
  color: #dc3545;
}

.after-sale-actions {
  display: flex;
  gap: 15px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e9ecef;
}

.action-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 20px;
  background: white;
  border: 2px solid var(--accent);
  color: var(--accent);
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.action-btn:hover {
  background: var(--accent);
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);
}

.action-btn.warning {
  border-color: #ff9800;
  color: #ff9800;
}

.action-btn.warning:hover {
  background: #ff9800;
  color: white;
  box-shadow: 0 4px 12px rgba(255, 152, 0, 0.3);
}

.write-review-section {
  margin-top: 25px;
  padding-top: 25px;
  border-top: 1px solid #e9ecef;
}

.write-review-btn {
  width: 100%;
  padding: 15px;
  background: linear-gradient(135deg, var(--color-primary-500) 0%, var(--color-primary-600) 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  transition: all 0.3s;
}

.write-review-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.loading {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
  font-size: 18px;
  color: #999;
}

.el-image-viewer {
  z-index: 9999;
}

@media (max-width: 768px) {
  .product-top-layout {
    flex-direction: column;
  }

  .product-image-section {
    flex: none;
  }

  .product-title {
    font-size: 24px;
  }

  .product-price {
    font-size: 28px;
  }

  .product-actions {
    flex-direction: column;
  }

  .product-meta {
    flex-direction: column;
    gap: 15px;
  }

  .seller-info > div {
    flex-direction: column;
    gap: 5px;
  }

  .after-sale-actions {
    flex-direction: column;
  }
}
</style>
