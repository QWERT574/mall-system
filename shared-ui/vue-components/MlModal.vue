<template>
  <Teleport to="body">
    <Transition name="ml-modal">
      <div v-if="visible" class="ml-modal-overlay" @click.self="handleOverlayClick">
        <div :class="['ml-modal', `ml-modal--${size}`]">
          <div v-if="$slots.header || title" class="ml-modal__header">
            <span class="ml-modal__title">{{ title }}</span>
            <button class="ml-modal__close" @click="handleClose">&times;</button>
          </div>
          <div class="ml-modal__body">
            <slot />
          </div>
          <div v-if="$slots.footer" class="ml-modal__footer">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
const props = withDefaults(defineProps<{
  visible?: boolean
  title?: string
  size?: 'sm' | 'md' | 'lg' | 'xl' | 'full'
  closeOnOverlay?: boolean
}>(), {
  visible: false,
  title: '',
  size: 'md',
  closeOnOverlay: true
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
  close: []
}>()

const handleClose = () => {
  emit('update:visible', false)
  emit('close')
}

const handleOverlayClick = () => {
  if (props.closeOnOverlay) {
    handleClose()
  }
}
</script>