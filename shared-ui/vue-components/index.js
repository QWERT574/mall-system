import MlButton from './MlButton.vue'
import MlCard from './MlCard.vue'
import MlInput from './MlInput.vue'
import MlTag from './MlTag.vue'
import MlModal from './MlModal.vue'

export { MlButton, MlCard, MlInput, MlTag, MlModal }

const components = [MlButton, MlCard, MlInput, MlTag, MlModal]

const install = (app) => {
  components.forEach(component => {
    app.component(component.__name, component)
  })
}

export default { install }

export const SharedUI = { install }