<template>
  <div class="system-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>系统设置</span>
        </div>
      </template>
      
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基础配置" name="basic">
          <el-form :model="basicForm" label-width="150px">
            <el-form-item label="网站名称">
              <el-input v-model="basicForm.siteName" placeholder="请输入网站名称" />
            </el-form-item>
            <el-form-item label="网站描述">
              <el-input
                v-model="basicForm.siteDescription"
                type="textarea"
                :rows="3"
                placeholder="请输入网站描述"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSaveBasic">
                <el-icon><Check /></el-icon>
                保存配置
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <el-tab-pane label="AI配置" name="ai">
          <el-form :model="aiForm" label-width="150px">
            <el-form-item label="启用AI助手">
              <el-switch v-model="aiForm.enabled" />
            </el-form-item>
            <el-form-item label="API密钥">
              <el-input
                v-model="aiForm.apiKey"
                type="password"
                show-password
                placeholder="请输入AI服务API密钥"
              />
            </el-form-item>
            <el-form-item label="API地址">
              <el-input v-model="aiForm.apiUrl" placeholder="请输入AI服务API地址" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSaveAi">
                <el-icon><Check /></el-icon>
                保存配置
              </el-button>
              <el-button @click="handleTestAi">
                <el-icon><Connection /></el-icon>
                测试连接
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <el-tab-pane label="系统信息" name="info">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="系统版本">v1.0.0</el-descriptions-item>
            <el-descriptions-item label="Java版本">Java 8</el-descriptions-item>
            <el-descriptions-item label="Spring Boot">2.7.9</el-descriptions-item>
            <el-descriptions-item label="数据库">MySQL 8.0</el-descriptions-item>
            <el-descriptions-item label="缓存">Redis</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { getSystemConfigList, updateSystemConfig, getAiConfig, updateAiConfig } from '@/api/system'

const activeTab = ref('basic')

const basicForm = reactive({
  siteName: '乡村振兴农产品销售平台',
  siteDescription: '助力乡村振兴，推广优质农产品'
})

const aiForm = reactive({
  enabled: true,
  apiKey: '',
  apiUrl: ''
})

const handleSaveBasic = async () => {
  try {
    await updateSystemConfig(1, basicForm.siteName)
    await updateSystemConfig(2, basicForm.siteDescription)
    ElMessage.success('保存成功')
  } catch (error) {
    console.error('保存配置失败:', error)
  }
}

const handleSaveAi = async () => {
  try {
    await updateAiConfig(aiForm.apiKey, aiForm.apiUrl, aiForm.enabled)
    ElMessage.success('保存成功')
  } catch (error) {
    console.error('保存AI配置失败:', error)
  }
}

const handleTestAi = async () => {
  try {
    ElMessage.info('测试AI连接...')
  } catch (error) {
    console.error('测试失败:', error)
  }
}
</script>

<style scoped>
.system-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.el-form {
  max-width: 600px;
  margin-top: 20px;
}
</style>
