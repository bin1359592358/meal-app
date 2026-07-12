/**
 * 订单列表页面
 * 展示当前餐桌下的所有订单
 */
const api = require('../../utils/api')
const storage = require('../../utils/storage')
const util = require('../../utils/util')
const app = getApp()

Page({
  data: {
    orders: [],
    loading: true,
    isChef: false,
    page: 1,
    hasMore: true,
  },

  onLoad(options) {
    this._checkRole()
    this._waitForLoginAndLoad()
  },

  onShow() {
    // 每次显示时刷新
    if (!this.data.loading) {
      this.setData({ page: 1, hasMore: true, orders: [] })
      this._loadOrders()
    }
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    this.setData({ page: 1, hasMore: true, orders: [] })
    this._loadOrders().then(() => {
      wx.stopPullDownRefresh()
    }).catch(() => {
      wx.stopPullDownRefresh()
    })
  },

  /**
   * 上拉加载更多
   */
  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this._loadOrders()
    }
  },

  /**
   * 检查当前用户角色
   */
  _checkRole() {
    const room = storage.getActiveRoom()
    if (room && room.role === 'chef') {
      this.setData({ isChef: true })
    }
  },

  /**
   * 等待登录完成后加载数据
   */
  async _waitForLoginAndLoad() {
    try {
      await app.globalData.loginPromise
      await this._loadOrders()
    } catch (err) {
      console.error('加载订单失败:', err)
      this.setData({ loading: false })
    }
  },

  /**
   * 加载订单列表
   */
  async _loadOrders() {
    const room = storage.getActiveRoom()
    if (!room || !room.id) {
      this.setData({ loading: false })
      wx.showToast({ title: '未加入餐桌', icon: 'none' })
      return
    }

    if (!this.data.hasMore) return

    this.setData({ loading: true })

    try {
      const page = this.data.page
      const result = await api.get(
        `/api/rooms/${room.id}/orders?page=${page}&page_size=20`
      )

      const list = Array.isArray(result) ? result : (result.items || result.orders || [])
      const orders = this.data.orders.concat(list)

      this.setData({
        orders,
        page: page + 1,
        hasMore: list.length >= 20,
        loading: false,
      })
      this._updateBadge()
    } catch (err) {
      console.error('加载订单失败:', err)
      this.setData({ loading: false })
    }
  },

  /**
   * 点击订单卡片
   */
  onOrderTap(e) {
    const order = e.detail.order
    if (order && order.id) {
      wx.navigateTo({
        url: `/pages/order-detail/order-detail?id=${order.id}`,
      })
    }
  },

  /**
   * 进入总览页面（厨师视角）
   */
  onGoOverview() {
    wx.navigateTo({
      url: '/pages/overview/overview',
    })
  },

  /**
   * 去点菜（空状态按钮）
   */
  onGoToMenu() {
    wx.switchTab({ url: '/pages/menu/menu' })
  },

  /**
   * 更新订单 Tab 角标（Badge 模块）
   * 显示 pending 状态的订单数
   */
  _updateBadge() {
    const pendingCount = this.data.orders.filter(o => o.status === 'pending').length
    if (pendingCount > 0) {
      wx.setTabBarBadge({ index: 1, text: String(pendingCount) })
    } else {
      wx.removeTabBarBadge({ index: 1 })
    }
  },
})
