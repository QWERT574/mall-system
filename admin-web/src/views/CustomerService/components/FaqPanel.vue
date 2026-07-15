<template>
  <el-popover
    placement="top-start"
    :width="320"
    trigger="click"
  >
    <template #reference>
      <el-button text>
        <el-icon><Collection /></el-icon> FAQ
      </el-button>
    </template>
    <div class="faq-panel">
      <div class="faq-search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索常见问题"
          clearable
          size="small"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      <div class="faq-list">
        <div
          v-for="faq in filteredFaqs"
          :key="faq.id"
          class="faq-item"
          @click="handleInsert(faq.answer)"
        >
          <div class="faq-question">{{ faq.question }}</div>
          <div class="faq-answer">{{ truncate(faq.answer, 60) }}</div>
        </div>
        <div v-if="!filteredFaqs.length" class="faq-empty">无匹配结果</div>
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Collection, Search } from '@element-plus/icons-vue'
import { listFaqTemplates } from '@/api/customerService'

const emit = defineEmits<{ insert: [content: string] }>()

const searchKeyword = ref('')
const faqList = ref<any[]>([])

const filteredFaqs = computed(() => {
  if (!searchKeyword.value.trim()) return faqList.value
  const kw = searchKeyword.value.toLowerCase()
  return faqList.value.filter(f =>
    f.question?.toLowerCase().includes(kw) ||
    f.keywords?.toLowerCase().includes(kw)
  )
})

onMounted(async () => {
  try {
    const res = await listFaqTemplates()
    faqList.value = res || []
  } catch (e) {}
})

function handleInsert(answer: string) {
  emit('insert', answer)
}

function truncate(text: string, max: number) {
  if (!text) return ''
  return text.length > max ? text.substring(0, max) + '...' : text
}
</script>

<style scoped lang="scss">
.faq-panel {
  .faq-search {
    margin-bottom: 10px;
    padding-bottom: 10px;
    border-bottom: 1px solid #f0f0f0;
  }
  .faq-list {
    max-height: 240px;
    overflow-y: auto;
    .faq-item {
      padding: 8px;
      border-radius: 4px;
      cursor: pointer;
      margin-bottom: 4px;
      &:hover { background: var(--color-bg-page); }
      .faq-question { font-size: 13px; color: var(--color-text-primary); font-weight: 500; }
      .faq-answer { font-size: 12px; color: var(--color-text-tertiary); margin-top: 2px; }
    }
    .faq-empty { text-align: center; color: var(--color-text-tertiary); padding: 20px 0; font-size: 13px; }
  }
}
</style>
