<template>
  <span :class="tagClasses">
    <slot />
    <span
      v-if="closable"
      class="ml-tag__close"
      @click.stop="$emit('close')"
    >&times;</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  color?: 'default' | 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'organic' | 'promo' | 'new'
  size?: 'sm' | 'md' | 'lg'
  rounded?: boolean
  closable?: boolean
}>(), {
  color: 'default',
  size: 'md',
  rounded: false,
  closable: false
})

defineEmits<{
  close: []
}>()

const tagClasses = computed(() => ({
  'ml-tag': true,
  [`ml-tag--${props.color}`]: true,
  [`ml-tag--${props.size}`]: props.size !== 'md',
  'ml-tag--rounded': props.rounded,
  'ml-tag--closable': props.closable
}))
</script>