<template>
  <button
    :class="buttonClasses"
    :disabled="disabled || loading"
    :aria-busy="loading || undefined"
    :type="nativeType"
    @click="handleClick"
  >
    <span v-if="loading" class="ml-btn__spinner" aria-hidden="true"></span>
    <slot v-else name="icon-left" />
    <slot />
    <slot name="icon-right" />
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  type?: 'primary' | 'secondary' | 'success' | 'danger' | 'warning' | 'text' | 'ghost'
  size?: 'sm' | 'md' | 'lg'
  block?: boolean
  icon?: boolean
  disabled?: boolean
  loading?: boolean
  nativeType?: 'button' | 'submit' | 'reset'
}>(), {
  type: 'primary',
  size: 'md',
  block: false,
  icon: false,
  disabled: false,
  loading: false,
  nativeType: 'button'
})

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

const buttonClasses = computed(() => ({
  'ml-btn': true,
  [`ml-btn--${props.type}`]: true,
  [`ml-btn--${props.size}`]: true,
  'ml-btn--block': props.block,
  'ml-btn--icon': props.icon,
  'ml-btn--loading': props.loading
}))

// 加载/禁用状态下拦截点击，防止重复提交
const handleClick = (event: MouseEvent) => {
  if (props.disabled || props.loading) return
  emit('click', event)
}
</script>
