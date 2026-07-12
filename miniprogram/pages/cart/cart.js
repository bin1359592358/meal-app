/**
 * 购物车 / 确认订单页面
 * 展示购物车菜品、人数选择、备注、下单
 */
const api = require('../../utils/api')
const storage = require('../../utils/storage')
const app = getApp()

Page({
  data: {
    items: [],
    peopleCount: 1,
    note: '',
    dishCount: 0,
    totalPrice: 0,
    submitting: false,
  },

  onLoad(options) {
    this._syncCartData()
  },

  onShow() {
    this._syncCartData()
  },

  /**
   * 从全局购物车同步数据
   */
  _syncCartData() {
    const cartItems = (app.globalData.cart.items || []).map(item => ({
      ...item,
      seasoningText: this.getSeasoningText(item.seasonings),
    }))
    const totalPrice = app.getCartTotal()
    const dishCount = app.getCartCount()

    this.setData({
      items: cartItems,
      totalPrice,
      dishCount,
    })
  },

  /**
   * 构建调味摘要文本
   */
  getSeasoningText(seasonings) {
    if (!seasonings || Object.keys(seasonings).length === 0) return ''
    const parts = []
    for (const key in seasonings) {
      const val = seasonings[key]
      if (val && val !== 'default') {
        parts.push(val)
      }
    }
    return parts.join(', ')
  },

  /**
   * 增加数量
   */
  onIncrease(e) {
    const idx = e.currentTarget.dataset.index
    const item = this.data.items[idx]
    app.updateCartItemQty(idx, item.quantity + 1)
    this._syncCartData()
  },

  /**
   * 减少数量
   */
  onDecrease(e) {
    const idx = e.currentTarget.dataset.index
    const item = this.data.items[idx]
    if (item.quantity <= 1) {
      wx.showModal({
        title: '提示',
        content: '确定删除该菜品吗？',
        success: (res) => {
          if (res.confirm) {
            app.removeFromCart(idx)
            this._syncCartData()
          }
        },
      })
    } else {
      app.updateCartItemQty(idx, item.quantity - 1)
      this._syncCartData()
    }
  },

  /**
   * 删除菜品
   */
  onDelete(e) {
    const idx = e.currentTarget.dataset.index
    wx.showModal({
      title: '提示',
      content: '确定删除该菜品吗？',
      success: (res) => {
        if (res.confirm) {
          app.removeFromCart(idx)
          this._syncCartData()
        }
      },
    })
  },

  /**
   * 人数增加
   */
  onPeopleIncrease() {
    this.setData({ peopleCount: this.data.peopleCount + 1 })
  },

  /**
   * 人数减少
   */
  onPeopleDecrease() {
    if (this.data.peopleCount > 1) {
      this.setData({ peopleCount: this.data.peopleCount - 1 })
    }
  },

  /**
   * 备注输入
   */
  onNoteInput(e) {
    this.setData({ note: e.detail.value })
  },

  /**
   * 格式化价格
   */
  formatPrice(price) {
    return `¥${Number(price).toFixed(1)}`
  },

  /**
   * 确认下单
   */
  async onSubmitOrder() {
    const items = this.data.items
    if (items.length === 0) {
      wx.showToast({ title: '购物车为空', icon: 'none' })
      return
    }

    const room = storage.getActiveRoom()
    if (!room || !room.id) {
      wx.showToast({ title: '未加入餐桌', icon: 'none' })
      return
    }

    if (this.data.submitting) return
    this.setData({ submitting: true })

    // 构建订单数据
    const orderItems = items.map(item => ({
      dish_id: item.dish.id,
      quantity: item.quantity,
      seasonings: item.seasonings || {},
    }))

    const payload = {
      items: orderItems,
      note: this.data.note,
      people_count: this.data.peopleCount,
    }

    try {
      const result = await api.post(
        `/api/rooms/${room.id}/orders`,
        payload,
        { showLoading: true }
      )

      // 清空购物车
      app.clearCart()

      wx.showToast({ title: '下单成功', icon: 'success' })

      // 跳转到订单详情
      const orderId = result.id || result.order_id
      if (orderId) {
        setTimeout(() => {
          wx.redirectTo({
            url: `/pages/order-detail/order-detail?id=${orderId}`,
          })
        }, 500)
      } else {
        setTimeout(() => {
          wx.switchTab({ url: '/pages/orders/orders' })
        }, 500)
      }
    } catch (err) {
      console.error('下单失败:', err)
    } finally {
      this.setData({ submitting: false })
    }
  },
})
