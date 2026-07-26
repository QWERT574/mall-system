<template>
  <div :class="wrapperClasses">
    <label
      v-if="label"
      :for="inputId"
      :class="['ml-form-item__label', { 'ml-form-item__label--required': required }]"
    >
      {{ label }}
    </label>
    <component
      :is="isTextarea ? 'textarea' : 'input'"
      :id="inputId"
      :type="isTextarea ? undefined : type"
      :class="inputClasses"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :readonly="readonly"
      :aria-invalid="isError || undefined"
      :aria-describedby="feedbackId"
      @input="handleInput"
      @blur="$emit('blur', $event)"
      @focus="$emit('focus', $event)"
    />
    <span v-if="error" :id="`${inputId}-error`" class="ml-form-item__error" role="alert">{{ error }}</span>
    <span v-else-if="hint" :id="`${inputId}-hint`" class="ml-form-item__hint">{{ hint }}</span>
  </div>
</template>

<script lang="ts">
// 模块级计数器：为每个实例生成稳定的 label ↔ 控件关联 id
let __mlInputUid = 0
export default {}
</script>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  modelValue?: string | number
  label?: string
  id?: string
  type?: string
  placeholder?: string
  disabled?: boolean
  readonly?: boolean
  required?: boolean
  error?: string
  hint?: string
  textarea?: boolean
  status?: '' | 'error' | 'success'
  shake?: boolean
}>(), {
  modelValue: '',
  label: '',
  id: '',
  type: 'text',
  placeholder: '',
  disabled: false,
  readonly: false,
  required: false,
  error: '',
  hint: '',
  textarea: false,
  status: '',
  shake: false
})

const emit = defineEmits<{
  'update:modelValue': [value: string | number]
  blur: [event: FocusEvent]
  focus: [event: FocusEvent]
}>()

const isTextarea = computed(() => props.textarea)
const isError = computed(() => props.status === 'error' || !!props.error)

// label 与控件关联：点击 label 聚焦输入框，屏幕阅读器可正确朗读标签
const inputId = props.id || `ml-field-${++__mlInputUid}`

// 将错误/提示信息挂到 aria-describedby，辅助技术可朗读校验反馈
const feedbackId = computed(() => {
  if (props.error) return `${inputId}-error`
  if (props.hint) return `${inputId}-hint`
  return undefined
})

const wrapperClasses = computed(() => ({
  'ml-form-item': true,
  'ml-form-item--error': isError.value,
  'ml-form-item--success': props.status === 'success',
  'ml-form-item--shake': props.shake
}))

const inputClasses = computed(() => ({
  'ml-input': !props.textarea,
  'ml-textarea': props.textarea,
  [`ml-input--${props.status}`]: !!props.status
}))

const handleInput = (e: Event) => {
  const target = e.target as HTMLInputElement
  emit('update:modelValue', target.value)
}
</script>
