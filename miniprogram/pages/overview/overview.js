/**
 * 订单总览页面（主厨专属）
 * 展示订单统计和菜品聚合数据
 */

const api = require('../../utils/api')
const storage = require('../../utils/storage')
const app = getApp()

Page({
  data: {
    roomId: '',
    loading: true,
    totalOrders: 0,
    totalPrice: 0,
    summary: [],
    sortBy: 'quantity', // 'quantity' | 'name'
  },

  onLoad(options) {
    this.init()
  },

  async init() {
    try {
      await app.globalData.loginPromise
      const activeRoom = storage.getActiveRoom()
      if (!activeRoom || !activeRoom.id) {
        wx.showToast({ title: '请先加入餐桌', icon: 'none' })
        return
      }
      this.setData({ roomId: activeRoom.id })
      await this.loadSummary()
    } catch (err) {
      console.error('订单总览初始化失败:', err)
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadSummary() {
    try {
      const res = await api.get(`/api/rooms/${this.data.roomId}/orders/summary`)
      const summary = res.summary || []
      this.setData({
        totalOrders: res.total_orders || 0,
        totalPrice: res.total_price || 0,
        summary: this.sortSummary(summary, this.data.sortBy),
      })
    } catch (err) {
      console.error('加载订单总览失败:', err)
    }
  },

  /**
   * 排序聚合数据
   */
  sortSummary(list, sortBy) {
    const sorted = [...list]
    if (sortBy === 'quantity') {
      sorted.sort((a, b) => (b.total_quantity || 0) - (a.total_quantity || 0))
    } else if (sortBy === 'name') {
      sorted.sort((a, b) => (a.dish_name || '').localeCompare(b.dish_name || ''))
    }
    return sorted
  },

  /**
   * 切换排序方式
   */
  onToggleSort() {
    const newSort = this.data.sortBy === 'quantity' ? 'name' : 'quantity'
    this.setData({
      sortBy: newSort,
      summary: this.sortSummary(this.data.summary, newSort),
    })
  },
})
