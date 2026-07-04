<template>
  <div class="ml-form-item">
    <label
      v-if="label"
      :class="['ml-form-item__label', { 'ml-form-item__label--required': required }]"
    >
      {{ label }}
    </label>
    <component
      :is="isTextarea ? 'textarea' : 'input'"
      :type="isTextarea ? undefined : type"
      :class="inputClasses"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :readonly="readonly"
      @input="handleInput"
      @blur="$emit('blur', $event)"
      @focus="$emit('focus', $event)"
    />
    <span v-if="error" class="ml-form-item__error">{{ error }}</span>
    <span v-else-if="hint" class="ml-form-item__hint">{{ hint }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  modelValue?: string | number
  label?: string
  type?: string
  placeholder?: string
  disabled?: boolean
  readonly?: boolean
  required?: boolean
  error?: string
  hint?: string
  textarea?: boolean
  status?: '' | 'error' | 'success'
}>(), {
  modelValue: '',
  label: '',
  type: 'text',
  placeholder: '',
  disabled: false,
  readonly: false,
  required: false,
  error: '',
  hint: '',
  textarea: false,
  status: ''
})

const emit = defineEmits<{
  'update:modelValue': [value: string | number]
  blur: [event: FocusEvent]
  focus: [event: FocusEvent]
}>()

const isTextarea = computed(() => props.textarea)

const inputClasses = computed(() => ({
  'ml-input': !props.textarea,
  'ml-textarea': props.textarea,
  [`ml-input--${props.status}`]: props.status
}))

const handleInput = (e: Event) => {
  const target = e.target as HTMLInputElement
  emit('update:modelValue', target.value)
}
</script>