<template>
  <div class="not-found-page">
    <div class="not-found-content">
      <div class="error-code">404</div>
      <h2>页面未找到</h2>
      <p>抱歉，您访问的页面不存在或已被移除</p>
      <div class="actions">
        <el-button type="primary" size="large" @click="$router.push('/')">返回首页</el-button>
        <el-button size="large" @click="$router.back()">返回上页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.not-found-page {
  min-height: 80vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7ed 100%);
}
.not-found-content {
  text-align: center;
  padding: 60px 40px;
}
.error-code {
  font-size: 120px;
  font-weight: 800;
  background: linear-gradient(135deg, #67c23a, #409eff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  line-height: 1;
  margin-bottom: 16px;
}
h2 {
  font-size: 28px;
  color: #303133;
  margin: 0 0 12px;
}
p {
  font-size: 16px;
  color: #909399;
  margin: 0 0 32px;
}
.actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}
</style>
