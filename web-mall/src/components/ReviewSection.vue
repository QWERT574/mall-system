<template>
  <div class="review-section">
    <div class="review-header">
      <h3 class="section-title">商品评价</h3>
      <div class="review-summary" v-if="reviewStats">
        <div class="rating-overview">
          <div class="average-rating">{{ reviewStats.averageRating.toFixed(1) }}</div>
          <div class="rating-stars">
            <el-rate v-model="reviewStats.averageRating" disabled :colors="['#99A9BF', '#F7BA2A', '#FF9900']" />
          </div>
          <div class="rating-count">共{{ reviewStats.total }}条评价</div>
        </div>
        <div class="rating-distribution">
          <div
            v-for="(item, index) in reviewStats.distribution"
            :key="index"
            class="rating-bar-item"
          >
            <span class="star-label">{{ 5 - index }}星</span>
            <el-progress
              :percentage="item.percentage"
              :color="getProgressColor(5 - index)"
              :stroke-width="6"
              :show-text="false"
            />
            <span class="star-count">{{ item.count }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="review-filters">
      <el-button
        :type="currentFilter === 'all' ? 'primary' : 'default'"
        @click="filterReviews('all')"
      >
        全部 ({{ reviewStats?.total || 0 }})
      </el-button>
      <el-button
        :type="currentFilter === 'good' ? 'primary' : 'default'"
        @click="filterReviews('good')"
      >
        好评 ({{ reviewStats?.goodCount || 0 }})
      </el-button>
      <el-button
        :type="currentFilter === 'medium' ? 'primary' : 'default'"
        @click="filterReviews('medium')"
      >
        中评 ({{ reviewStats?.mediumCount || 0 }})
      </el-button>
      <el-button
        :type="currentFilter === 'bad' ? 'primary' : 'default'"
        @click="filterReviews('bad')"
      >
        差评 ({{ reviewStats?.badCount || 0 }})
      </el-button>
      <el-button
        :type="hasImageFilter ? 'primary' : 'default'"
        @click="filterByImage"
      >
        <el-icon><Picture /></el-icon>
        有图
      </el-button>
    </div>

    <div class="review-list">
      <div
        v-for="review in reviews"
        :key="review.id"
        class="review-item"
      >
        <div class="reviewer-info">
          <div class="reviewer-avatar">
            <el-icon :size="32"><User /></el-icon>
          </div>
          <div class="reviewer-name">
            {{ review.user_name || '匿名用户' }}
          </div>
        </div>

        <div class="review-content">
          <div class="review-rating">
            <el-rate v-model="review.rating" disabled :colors="['#99A9BF', '#F7BA2A', '#FF9900']" />
            <span class="review-time">{{ formatDate(review.created_at) }}</span>
          </div>

          <div class="review-text">
            {{ review.content }}
          </div>

          <div class="review-images" v-if="review.images">
            <div
              v-for="(img, idx) in parseImages(review.images)"
              :key="idx"
              class="review-image-item"
              @click="$emit('previewImage', img)"
            >
              <img :src="img" :alt="'评价图片' + (idx + 1)" />
            </div>
          </div>

          <div class="seller-reply" v-if="review.reply">
            <div class="reply-header">
              <el-icon><Shop /></el-icon>
              <span class="reply-label">商家回复：</span>
            </div>
            <div class="reply-content">
              {{ review.reply.replyContent }}
            </div>
            <div class="reply-time">
              {{ formatDate(review.reply.createdAt) }}
            </div>
          </div>
        </div>
      </div>

      <div class="empty-reviews" v-if="reviews.length === 0">
        <el-empty description="暂无评价" />
      </div>
    </div>

    <div class="review-pagination" v-if="reviews.length > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadReviews"
        @size-change="loadReviews"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { Picture, User, Shop } from '@element-plus/icons-vue'
import api from '@/utils/api'

const props = defineProps({
  productId: { type: [String, Number], required: true }
})

defineEmits(['previewImage'])

const reviews = ref([])
const reviewStats = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const currentFilter = ref('all')
const hasImageFilter = ref(false)

const loadReviews = async () => {
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }

    if (currentFilter.value === 'good') {
      params.minRating = 4
    } else if (currentFilter.value === 'medium') {
      params.minRating = 3
      params.maxRating = 3
    } else if (currentFilter.value === 'bad') {
      params.maxRating = 2
    }

    if (hasImageFilter.value) {
      params.hasImage = true
    }

    const response = await api.getProductReviews(props.productId, params)
    if (response.code === 0) {
      reviews.value = response.data.records || []
      total.value = response.data.total || 0

      if (currentPage.value === 1 && !reviewStats.value) {
        await loadReviewStats()
      }
    }
  } catch (error) {
    console.error('加载评价失败:', error)
  }
}

const loadReviewStats = async () => {
  try {
    const response = await api.getProductReviews(props.productId, {
      page: 1,
      size: 1000
    })

    if (response.code === 0) {
      const allReviews = response.data.records || []
      const totalReviews = allReviews.length

      let sum = 0
      let goodCount = 0
      let mediumCount = 0
      let badCount = 0
      const distribution = [
        { count: 0, percentage: 0 },
        { count: 0, percentage: 0 },
        { count: 0, percentage: 0 },
        { count: 0, percentage: 0 },
        { count: 0, percentage: 0 }
      ]

      allReviews.forEach(review => {
        sum += review.rating || 0

        if (review.rating >= 5) distribution[0].count++
        else if (review.rating === 4) distribution[1].count++
        else if (review.rating === 3) distribution[2].count++
        else if (review.rating === 2) distribution[3].count++
        else if (review.rating === 1) distribution[4].count++

        if (review.rating >= 4) goodCount++
        else if (review.rating === 3) mediumCount++
        else badCount++
      })

      distribution.forEach(item => {
        item.percentage = totalReviews > 0 ? Math.round((item.count / totalReviews) * 100) : 0
      })

      reviewStats.value = {
        averageRating: totalReviews > 0 ? sum / totalReviews : 0,
        total: totalReviews,
        goodCount,
        mediumCount,
        badCount,
        distribution
      }
    }
  } catch (error) {
    console.error('加载评价统计失败:', error)
  }
}

const filterReviews = (filter) => {
  currentFilter.value = filter
  hasImageFilter.value = false
  currentPage.value = 1
  loadReviews()
}

const filterByImage = () => {
  hasImageFilter.value = !hasImageFilter.value
  currentFilter.value = 'all'
  currentPage.value = 1
  loadReviews()
}

const getProgressColor = (rating) => {
  if (rating >= 4) return '#67C23A'
  if (rating === 3) return '#E6A23C'
  return 'var(--color-error)'
}

const parseImages = (imagesStr) => {
  if (!imagesStr) return []
  try {
    if (typeof imagesStr === 'string') return JSON.parse(imagesStr)
    return imagesStr
  } catch (e) {
    return []
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date

  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  const month = 30 * day

  if (diff < minute) return '刚刚'
  if (diff < hour) return Math.floor(diff / minute) + '分钟前'
  if (diff < day) return Math.floor(diff / hour) + '小时前'
  if (diff < month) return Math.floor(diff / day) + '天前'
  return date.toLocaleDateString('zh-CN')
}

const refresh = () => {
  reviewStats.value = null
  currentPage.value = 1
  loadReviews()
}

watch(() => props.productId, () => {
  refresh()
})

onMounted(() => {
  loadReviews()
})

defineExpose({ refresh })
</script>

<style scoped>
.review-section {
  background: white;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-top: 30px;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid #f0f0f0;
}

.review-summary {
  display: flex;
  gap: 40px;
}

.rating-overview {
  text-align: center;
}

.average-rating {
  font-size: 48px;
  font-weight: 600;
  color: #FF9900;
  line-height: 1;
  margin-bottom: 10px;
}

.rating-stars {
  margin-bottom: 8px;
}

.rating-count {
  font-size: 14px;
  color: #999;
}

.rating-distribution {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 300px;
}

.rating-bar-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.star-label {
  font-size: 14px;
  color: #666;
  width: 30px;
}

.star-count {
  font-size: 14px;
  color: #999;
  width: 40px;
  text-align: right;
}

.review-filters {
  display: flex;
  gap: 10px;
  margin-bottom: 25px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.review-filters .el-button {
  padding: 8px 20px;
  font-size: 15px;
  border-radius: 20px;
}

.review-list {
  min-height: 200px;
}

.review-item {
  display: flex;
  gap: 20px;
  padding: 25px 0;
  border-bottom: 1px solid #f0f0f0;
}

.review-item:last-child {
  border-bottom: none;
}

.reviewer-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  min-width: 80px;
}

.reviewer-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-500) 0%, var(--color-primary-600) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.reviewer-name {
  font-size: 14px;
  color: #666;
  text-align: center;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-content {
  flex: 1;
}

.review-rating {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.review-time {
  font-size: 13px;
  color: #999;
}

.review-text {
  font-size: 15px;
  line-height: 1.8;
  color: #333;
  margin-bottom: 15px;
}

.review-images {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 15px;
}

.review-image-item {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #e0e0e0;
  transition: all 0.3s;
}

.review-image-item:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.review-image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.seller-reply {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  margin-top: 15px;
  border-left: 3px solid #4CAF50;
}

.reply-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 14px;
  color: #666;
}

.reply-label {
  font-weight: 600;
}

.reply-content {
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  margin-bottom: 8px;
}

.reply-time {
  font-size: 12px;
  color: #999;
  text-align: right;
}

.empty-reviews {
  padding: 60px 0;
}

.review-pagination {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: center;
}

.section-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px solid #4CAF50;
}

@media (max-width: 768px) {
  .review-header {
    flex-direction: column;
    gap: 20px;
  }

  .review-summary {
    flex-direction: column;
    gap: 20px;
    width: 100%;
  }

  .rating-distribution {
    width: 100%;
  }

  .review-filters {
    flex-wrap: wrap;
  }

  .review-item {
    flex-direction: column;
  }

  .reviewer-info {
    flex-direction: row;
    min-width: auto;
  }

  .review-images {
    gap: 8px;
  }

  .review-image-item {
    width: 80px;
    height: 80px;
  }
}
</style>
