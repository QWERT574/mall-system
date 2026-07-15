const { get, post, put } = require('../../utils/request')

Page({
  data: {
    id: null,
    isEdit: false,
    form: {
      consignee: '',
      phone: '',
      province: '',
      city: '',
      district: '',
      detail: '',
      isDefault: false
    }
  },

  onLoad(options) {
    if (options.id) {
      this.id = options.id
      this.isEdit = true
      this.loadAddress()
    }
  },

  async loadAddress() {
    try {
      const address = await get(`/api/user/address/detail/${this.id}`)
      // 映射后端字段到表单
      this.setData({
        form: {
          consignee: address.consignee || address.name || '',
          phone: address.phone || '',
          province: address.province || '',
          city: address.city || '',
          district: address.district || '',
          detail: address.detail || '',
          isDefault: address.isDefault || false
        }
      })
    } catch (e) {
      console.error('加载地址失败', e)
      wx.showToast({ title: '加载地址失败', icon: 'none' })
    }
  },

  onNameInput(e) {
    this.setData({ 'form.consignee': e.detail.value })
  },

  onPhoneInput(e) {
    let val = e.detail.value.replace(/\D/g, '')
    this.setData({ 'form.phone': val })
  },

  onRegionChange(e) {
    const [province, city, district] = e.detail.value
    this.setData({
      'form.province': province,
      'form.city': city,
      'form.district': district
    })
  },

  onDetailInput(e) {
    this.setData({ 'form.detail': e.detail.value })
  },

  onDefaultChange(e) {
    this.setData({ 'form.isDefault': e.detail.value })
  },

  async saveAddress() {
    const { consignee, phone, province, detail } = this.data.form

    if (!consignee || !consignee.trim()) {
      return wx.showToast({ title: '请输入收货人姓名', icon: 'none' })
    }

    if (!phone || phone.length !== 11) {
      return wx.showToast({ title: '请输入正确的手机号码', icon: 'none' })
    }

    if (!province) {
      return wx.showToast({ title: '请选择所在地区', icon: 'none' })
    }

    if (!detail || !detail.trim()) {
      return wx.showToast({ title: '请输入详细地址', icon: 'none' })
    }

    try {
      if (this.isEdit) {
        await post(`/api/user/address/update`, { ...this.data.form, id: Number(this.id) })
      } else {
        await post('/api/user/address/add', this.data.form)
      }
      wx.showToast({
        title: this.isEdit ? '修改成功' : '添加成功',
        icon: 'success'
      })
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
    } catch (e) {
      console.error('保存地址失败', e)
      wx.showToast({ title: '保存失败，请重试', icon: 'none' })
    }
  }
})
