const app = getApp()
const { get, post, put } = require('../../utils/request')

Page({
  data: {
    detail: null,
    records: [],
    loading: true,
    showEvidenceModal: false,
    evidenceText: '',
    showLogisticsModal: false,
    logisticsCompany: '',
    logisticsNo: ''
  },
  onLoad(options) {
    if (options.id) this.loadDetail(options.id)
  },
  async loadDetail(id) {
    try {
      const res = await get(`/api/aftersale/${id}`)
      res.createdAtText = this.formatTime(res.createdAt)
      this.setData({ detail: res })
      this.loadRecords(id)
    } catch (e) {
      console.error('加载详情失败:', e)
    } finally {
      this.setData({ loading: false })
    }
  },
  async loadRecords(id) {
    try {
      const res = await get(`/api/aftersale/${id}/records`)
      const records = (res || []).map(r => ({
        ...r,
        createdAtText: this.formatTime(r.createdAt)
      }))
      this.setData({ records })
    } catch (e) {
      console.error('加载处理记录失败:', e)
    }
  },
  formatTime(t) {
    if (!t) return ''
    const d = new Date(t)
    return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
  },

  // 撤销售后申请
  cancelAftersale() {
    wx.showModal({
      title: '确认撤销',
      content: '确定要撤销该售后申请吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            wx.showLoading({ title: '处理中...' });
            await post(`/api/aftersale/${this.data.detail.id}/cancel`, { userId: app.globalData.userInfo.id });
            wx.hideLoading();
            wx.showToast({ title: '已撤销', icon: 'success' });
            this.loadDetail(this.data.detail.id);
          } catch (e) {
            wx.hideLoading();
            wx.showToast({ title: '撤销失败', icon: 'none' });
          }
        }
      }
    });
  },

  // 联系客服
  contactService() {
    wx.navigateTo({ url: '/pages/chat/chat' });
  },

  // 补充证据
  showAddEvidence() {
    this.setData({ showEvidenceModal: true, evidenceText: '' });
  },
  hideEvidenceModal() {
    this.setData({ showEvidenceModal: false });
  },
  onEvidenceInput(e) {
    this.setData({ evidenceText: e.detail.value });
  },
  async submitEvidence() {
    if (!this.data.evidenceText.trim()) {
      wx.showToast({ title: '请输入补充说明', icon: 'none' });
      return;
    }
    try {
      wx.showLoading({ title: '提交中...' });
      await post(`/api/aftersale/${this.data.detail.id}/evidence`, {
        evidence: this.data.evidenceText,
        userId: app.globalData.userInfo.id
      });
      wx.hideLoading();
      wx.showToast({ title: '提交成功', icon: 'success' });
      this.setData({ showEvidenceModal: false });
      this.loadDetail(this.data.detail.id);
    } catch (e) {
      wx.hideLoading();
      wx.showToast({ title: '提交失败', icon: 'none' });
    }
  },

  // 填写退货物流
  showAddLogistics() {
    this.setData({ showLogisticsModal: true, logisticsCompany: '', logisticsNo: '' });
  },
  hideLogisticsModal() {
    this.setData({ showLogisticsModal: false });
  },
  onLogisticsCompanyInput(e) {
    this.setData({ logisticsCompany: e.detail.value });
  },
  onLogisticsNoInput(e) {
    this.setData({ logisticsNo: e.detail.value });
  },
  async submitLogistics() {
    if (!this.data.logisticsCompany.trim() || !this.data.logisticsNo.trim()) {
      wx.showToast({ title: '请填写完整物流信息', icon: 'none' });
      return;
    }
    try {
      wx.showLoading({ title: '提交中...' });
      await put(`/api/aftersale/${this.data.detail.id}/logistics`, {
        logisticsCompany: this.data.logisticsCompany,
        logisticsNo: this.data.logisticsNo,
        userId: app.globalData.userInfo.id
      });
      wx.hideLoading();
      wx.showToast({ title: '提交成功', icon: 'success' });
      this.setData({ showLogisticsModal: false });
      this.loadDetail(this.data.detail.id);
    } catch (e) {
      wx.hideLoading();
      wx.showToast({ title: '提交失败', icon: 'none' });
    }
  }
})
