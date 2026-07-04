<template>
  <div :class="cardClasses">
    <div v-if="$slots.header || title" class="ml-card__header">
      <span v-if="title" class="ml-card__title">{{ title }}</span>
      <slot name="header" />
    </div>
    <div class="ml-card__body">
      <slot />
    </div>
    <div v-if="$slots.footer" class="ml-card__footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  title?: string
  variant?: 'default' | 'flat' | 'bordered' | 'hover-glow' | 'stat'
  interactive?: boolean
}>(), {
  title: '',
  variant: 'default',
  interactive: false
})

const cardClasses = computed(() => ({
  'ml-card': true,
  [`ml-card--${props.variant}`]: props.variant !== 'default',
  'ml-card--interactive': props.interactive,
  'ml-card--stat': props.variant === 'stat'
}))
</script>