/**
 * cart-bar 浮动购物车栏组件
 * 固定在页面底部，展示购物车摘要信息
 */
Component({
  properties: {
    count: {
      type: Number,
      value: 0,
    },
    total: {
      type: Number,
      value: 0,
    },
  },

  methods: {
    onCheckout() {
      this.triggerEvent('onCheckout')
    },
  },
})
