/**
 * order-card 组件
 * 可复用的订单卡片，展示订单摘要信息
 */
const util = require('../../utils/util')

Component({
  properties: {
    order: {
      type: Object,
      value: {},
    },
    showNickname: {
      type: Boolean,
      value: false,
    },
  },

  data: {
    statusText: '',
    statusColor: '',
    relativeTime: '',
    totalPrice: '',
    dishSummary: '',
  },

  observers: {
    'order': function (order) {
      if (!order || !order.id) return
      this.setData({
        statusText: util.getStatusText(order.status),
        statusColor: util.getStatusColor(order.status),
        relativeTime: util.formatRelativeTime(order.created_at),
        totalPrice: util.formatPrice(order.total_price),
        dishSummary: this._buildDishSummary(order.items),
      })
    },
  },

  methods: {
    /**
     * 构建菜品摘要文本，最多显示3个
     */
    _buildDishSummary(items) {
      if (!items || items.length === 0) return ''
      const parts = items.slice(0, 3).map(item => {
        const name = item.dish_name || (item.dish && item.dish.name) || '未知菜品'
        return `${name}×${item.quantity}`
      })
      let summary = parts.join(', ')
      if (items.length > 3) {
        summary += ', ...'
      }
      return summary
    },

    onTap() {
      this.triggerEvent('onTap', { order: this.data.order })
    },
  },
})
