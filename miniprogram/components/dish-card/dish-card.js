/**
 * dish-card 菜品卡片组件
 * 展示单个菜品信息，支持加减操作
 */
Component({
  properties: {
    dish: {
      type: Object,
      value: {},
    },
    quantity: {
      type: Number,
      value: 0,
    },
  },

  methods: {
    /** 点击卡片 */
    onTap() {
      if (this.data.dish.is_available === false) return
      this.triggerEvent('onTap', { dish: this.data.dish })
    },

    /** 点击加号 */
    onAdd(e) {
      if (this.data.dish.is_available === false) return
      this.triggerEvent('onAdd', { dish: this.data.dish })
    },

    /** 点击减号 */
    onMinus(e) {
      this.triggerEvent('onMinus', { dish: this.data.dish })
    },

    /** 阻止冒泡 */
    preventBubble() {},
  },
})
