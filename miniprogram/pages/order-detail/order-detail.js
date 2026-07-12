/**
 * 订单详情页面
 * 展示订单完整信息，厨师可操作订单状态变更
 */
const api = require('../../utils/api')
const storage = require('../../utils/storage')
const util = require('../../utils/util')
const app = getApp()

Page({
  data: {
    orderId: null,
    order: null,
    loading: true,
    isChef: false,
    statusText: '',
    statusColor: '',
    formattedTime: '',
    totalPrice: '',
    perPersonPrice: '',
    hasPerPerson: false,
    actionInProgress: false,
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ orderId: options.id })
      this._checkRole()
      this._waitForLoginAndLoad()
    } else {
      wx.showToast({ title: '订单ID缺失', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 1000)
    }
  },

  /**
   * 检查用户角色
   */
  _checkRole() {
    const room = storage.getActiveRoom()
    if (room && room.role === 'chef') {
      this.setData({ isChef: true })
    }
  },

  /**
   * 等待登录完成后加载
   */
  async _waitForLoginAndLoad() {
    try {
      await app.globalData.loginPromise
      await this._loadOrder()
    } catch (err) {
      console.error('加载订单详情失败:', err)
      this.setData({ loading: false })
    }
  },

  /**
   * 加载订单详情
   */
  async _loadOrder() {
    const room = storage.getActiveRoom()
    if (!room || !room.id) {
      this.setData({ loading: false })
      wx.showToast({ title: '未加入餐桌', icon: 'none' })
      return
    }

    this.setData({ loading: true })

    try {
      const order = await api.get(
        `/api/rooms/${room.id}/orders/${this.data.orderId}`
      )

      // 为每个菜品项添加调味摘要文本
      if (order.items) {
        order.items = order.items.map(item => ({
          ...item,
          seasoningText: item.seasoning_text || '',
        }))
      }

      // 计算人均价格
      const hasPerPerson = order.people_count && order.people_count > 1
      const perPersonPrice = hasPerPerson
        ? util.formatPrice(order.total_price / order.people_count)
        : ''

      this.setData({
        order,
        statusText: util.getStatusText(order.status),
        statusColor: util.getStatusColor(order.status),
        formattedTime: util.formatTime(order.created_at),
        totalPrice: util.formatPrice(order.total_price),
        perPersonPrice,
        hasPerPerson,
        loading: false,
      })
    } catch (err) {
      console.error('加载订单详情失败:', err)
      this.setData({ loading: false })
    }
  },

  /**
   * 更新订单状态
   */
  async _updateStatus(newStatus) {
    if (this.data.actionInProgress) return

    const room = storage.getActiveRoom()
    if (!room || !room.id) return

    this.setData({ actionInProgress: true })

    try {
      await api.patch(
        `/api/rooms/${room.id}/orders/${this.data.orderId}/status`,
        { status: newStatus },
        { showLoading: true }
      )

      wx.showToast({ title: '状态已更新', icon: 'success' })

      // 重新加载订单数据
      await this._loadOrder()
    } catch (err) {
      console.error('更新状态失败:', err)
    } finally {
      this.setData({ actionInProgress: false })
    }
  },

  /**
   * 开始制作 (pending → preparing)
   */
  onStartPrepare() {
    this._updateStatus('preparing')
  },

  /**
   * 标记出餐 (preparing → served)
   */
  onMarkServed() {
    this._updateStatus('served')
  },

  /**
   * 完成订单 (served → completed)
   */
  onComplete() {
    this._updateStatus('completed')
  },

  /**
   * 取消订单 (pending/preparing → cancelled)
   */
  onCancel() {
    wx.showModal({
      title: '确认取消',
      content: '确定要取消此订单吗？',
      confirmColor: '#E53935',
      success: (res) => {
        if (res.confirm) {
          this._updateStatus('cancelled')
        }
      },
    })
  },
})
