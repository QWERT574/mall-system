<template>
  <button
    :class="buttonClasses"
    :disabled="disabled"
    :type="nativeType"
    @click="$emit('click', $event)"
  >
    <slot name="icon-left" />
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

defineEmits<{
  click: [event: MouseEvent]
}>()

const buttonClasses = computed(() => ({
  'ml-btn': true,
  [`ml-btn--${props.type}`]: true,
  [`ml-btn--${props.size}`]: true,
  'ml-btn--block': props.block,
  'ml-btn--icon': props.icon
}))
</script>