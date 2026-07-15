<template>
  <div class="order-list-container">
    <h2 class="page-header">订单管理</h2>
    
    <div class="filter-bar">
      <div class="search-box">
        <input 
          v-model="searchKeyword" 
          placeholder="搜索订单号/商品" 
          @keyup.enter="handleSearch"
        />
        <button @click="handleSearch">搜索</button>
      </div>
      
      <select v-model="statusFilter" @change="handleFilter">
        <option value="">全部状态</option>
        <option value="0">待付款</option>
        <option value="1">待发货</option>
        <option value="2">待收货</option>
        <option value="3">已完成</option>
        <option value="4">已取消</option>
      </select>
      
      <select v-model="sortBy" @change="handleSort">
        <option value="createTimeDesc">最新下单</option>
        <option value="createTimeAsc">最早下单</option>
        <option value="priceDesc">金额从高到低</option>
        <option value="priceAsc">金额从低到高</option>
      </select>
      
      <button class="btn-reset" @click="resetFilter">重置</button>
    </div>
    
    <div v-if="loading" class="loading-state">
      <p>加载中...</p>
    </div>
    
    <div v-else-if="filteredOrders.length === 0" class="empty-state">
      <el-empty description="暂无订单" :image-size="100" />
    </div>
    
    <div v-else class="orders-table-wrapper">
      <table class="orders-table">
        <thead>
          <tr>
            <th>订单号</th>
            <th>客户</th>
            <th>商品</th>
            <th>金额</th>
            <th>状态</th>
            <th>下单时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in filteredOrders" :key="order.id">
            <td class="order-id">{{ order.id }}</td>
            <td>{{ order.consignee || '客户' }}</td>
            <td>
              <span v-if="order.items && order.items.length > 0">
                {{ order.items[0].productName }}
                <span v-if="order.items.length > 1" class="item-count">等{{ order.items.length }}件</span>
              </span>
              <span v-else>-</span>
            </td>
            <td class="amount">{{ calculateAmount(order) }}</td>
            <td>
              <span class="status-tag" :class="'tag-' + (order.status || 0)">
                {{ getStatusText(order.status) }}
              </span>
            </td>
            <td class="time-cell">{{ formatDateTime(order.createdAt || order.createTime) }}</td>
            <td>
              <div class="action-buttons">
                <button class="btn-view" @click="$emit('view-order', order)">查看</button>
                <button 
                  v-if="order.status === 1" 
                  class="btn-ship" 
                  @click="$emit('ship-order', order)"
                >发货</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    orders: {
      type: Array,
      default: () => []
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  emits: ['ship-order', 'view-order', 'show-error', 'show-success'],
  data() {
    return {
      searchKeyword: '',
      statusFilter: '',
      sortBy: 'createTimeDesc'
    };
  },
  computed: {
    filteredOrders() {
      let result = [...this.orders];
      
      if (this.searchKeyword) {
        const keyword = this.searchKeyword.toLowerCase();
        result = result.filter(order => {
          if (String(order.id).includes(keyword)) return true;
          if (order.items && order.items.some(item => 
            item.productName && item.productName.toLowerCase().includes(keyword)
          )) return true;
          return false;
        });
      }
      
      if (this.statusFilter !== '') {
        const statusNum = parseInt(this.statusFilter);
        result = result.filter(order => order.status === statusNum);
      }
      
      result.sort((a, b) => {
        const dateA = this.parseDate(a.createdAt || a.createTime);
        const dateB = this.parseDate(b.createdAt || b.createTime);
        const priceA = this.getOrderPrice(a);
        const priceB = this.getOrderPrice(b);
        
        switch (this.sortBy) {
          case 'createTimeDesc': return dateB - dateA;
          case 'createTimeAsc': return dateA - dateB;
          case 'priceDesc': return priceB - priceA;
          case 'priceAsc': return priceA - priceB;
          default: return 0;
        }
      });
      
      return result;
    }
  },
  methods: {
    parseDate(dateStr) {
      if (!dateStr) return 0;
      if (Array.isArray(dateStr)) {
        const [year, month, day, hour, minute, second] = dateStr;
        return new Date(year, month - 1, day, hour, minute, second).getTime();
      }
      const date = new Date(dateStr);
      return isNaN(date.getTime()) ? 0 : date.getTime();
    },
    getOrderPrice(order) {
      if (!order) return 0;
      if (order.totalPrice) return parseFloat(order.totalPrice);
      if (order.payAmount) return parseFloat(order.payAmount);
      if (order.items && order.items.length > 0) {
        return order.items.reduce((sum, item) => sum + (item.price * item.quantity), 0);
      }
      return 0;
    },
    handleSearch() {},
    handleFilter() {},
    handleSort() {},
    resetFilter() {
      this.searchKeyword = '';
      this.statusFilter = '';
      this.sortBy = 'createTimeDesc';
    },
    getStatusText(status) {
      const map = { 0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消' };
      return map[status] || '未知';
    },
    formatDateTime(dateStr) {
      if (!dateStr) return '-';
      if (Array.isArray(dateStr)) {
        const [year, month, day, hour, minute] = dateStr;
        return `${year}-${String(month).padStart(2,'0')}-${String(day).padStart(2,'0')} ${String(hour).padStart(2,'0')}:${String(minute).padStart(2,'0')}`;
      }
      const date = new Date(dateStr);
      if (isNaN(date.getTime())) return '-';
      return date.toLocaleString('zh-CN');
    },
    calculateAmount(order) {
      if (!order) return '¥0';
      if (order.totalPrice) return '¥' + order.totalPrice;
      if (order.payAmount) return '¥' + order.payAmount;
      if (order.items && order.items.length > 0) {
        const total = order.items.reduce((sum, item) => sum + (item.price * item.quantity), 0);
        return '¥' + total.toFixed(2);
      }
      return '¥0';
    }
  }
};
</script>

<style scoped>
.order-list-container {
  padding: 16px;
  background: #fff;
}

.page-header {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eee;
}

.filter-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.search-box {
  display: flex;
  gap: 0;
  flex: 1;
  min-width: 200px;
}

.search-box input {
  flex: 1;
  padding: 7px 12px;
  border: 1px solid #d9d9d9;
  border-right: none;
  border-radius: 4px 0 0 4px;
  font-size: 14px;
  outline: none;
}

.search-box input:focus {
  border-color: #1890ff;
}

.search-box button {
  padding: 7px 16px;
  background: #1890ff;
  color: #fff;
  border: 1px solid #1890ff;
  border-radius: 0 4px 4px 0;
  cursor: pointer;
  font-size: 14px;
}

.search-box button:hover {
  background: #40a9ff;
}

select {
  padding: 7px 28px 7px 10px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  cursor: pointer;
  background: #fff;
  color: #333;
}

select:focus {
  border-color: #1890ff;
}

.btn-reset {
  padding: 7px 14px;
  background: #fff;
  color: #666;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn-reset:hover {
  color: #1890ff;
  border-color: #1890ff;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 48px 20px;
  color: #999;
  font-size: 14px;
}

.orders-table-wrapper {
  overflow-x: auto;
}

.orders-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.orders-table th,
.orders-table td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}

.orders-table th {
  background: #fafafa;
  font-weight: 500;
  color: #666;
  font-size: 13px;
}

.orders-table td {
  color: #333;
}

.orders-table tbody tr:hover {
  background: #fafafa;
}

.order-id {
  color: #1890ff;
}

.amount {
  font-weight: 500;
  color: #f5222d;
}

.item-count {
  color: #999;
  font-size: 12px;
}

.time-cell {
  color: #888;
  font-size: 13px;
}

.status-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 2px;
  font-size: 12px;
  line-height: 20px;
}

.tag-0 { background: #fff7e6; color: #d48806; border: 1px solid #ffe58f; }
.tag-1 { background: #e6f7ff; color: #0958d9; border: 1px solid #91caff; }
.tag-2 { background: #f0f5ff; color: #2f54eb; border: 1px solid #adc6ff; }
.tag-3 { background: #f6ffed; color: #389e0d; border: 1px solid #b7eb8f; }
.tag-4 { background: #fff1f0; color: #cf1322; border: 1px solid #ffa39e; }

.action-buttons {
  display: flex;
  gap: 8px;
}

.btn-view,
.btn-ship {
  padding: 4px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  background: #fff;
  color: #333;
}

.btn-view:hover {
  color: #1890ff;
  border-color: #1890ff;
}

.btn-ship {
  background: #1890ff;
  color: #fff;
  border-color: #1890ff;
}

.btn-ship:hover {
  background: #40a9ff;
}

@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-box {
    min-width: 0;
  }

  select {
    width: 100%;
  }

  .btn-reset {
    width: 100%;
  }

  .orders-table-wrapper {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  .orders-table-wrapper::after {
    content: '← 左右滑动查看更多 →';
    display: block;
    text-align: center;
    color: #999;
    font-size: 12px;
    padding: 8px 0 0;
  }
}

@media (max-width: 480px) {
  .order-list-container {
    padding: 10px;
  }

  .page-header {
    font-size: 16px;
  }

  .orders-table th,
  .orders-table td {
    padding: 8px 6px;
    font-size: 12px;
  }
}
</style>
