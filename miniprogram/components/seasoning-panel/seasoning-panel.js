/**
 * seasoning-panel 调味选择面板组件
 * 底部弹出面板，支持 single/multi/scale/text 四种调味类型
 */
Component({
  properties: {
    dish: {
      type: Object,
      value: {},
      observer(newVal) {
        if (newVal && newVal.seasonings) {
          this._initSelections()
        }
      },
    },
    visible: {
      type: Boolean,
      value: false,
    },
  },

  data: {
    /** 用户选择结果，key 为调味名 */
    selections: {},
  },

  methods: {
    /** 初始化默认选择 */
    _initSelections() {
      const selections = {}
      const seasonings = this.data.dish.seasonings || []
      seasonings.forEach(s => {
        if (s.type === 'single') {
          selections[s.name] = s.default || s.options[0] || ''
        } else if (s.type === 'multi') {
          selections[s.name] = Array.isArray(s.default) ? [...s.default] : []
        } else if (s.type === 'scale') {
          selections[s.name] = s.default !== undefined ? s.default : (s.min || 0)
        } else if (s.type === 'text') {
          selections[s.name] = s.default || ''
        }
      })
      this.setData({ selections })
    },

    /** 单选类型：选择选项 */
    onSingleSelect(e) {
      const { name, option } = e.currentTarget.dataset
      this.setData({
        [`selections.${name}`]: option,
      })
    },

    /** 多选类型：切换选项 */
    onMultiToggle(e) {
      const { name, option } = e.currentTarget.dataset
      const current = this.data.selections[name] || []
      const idx = current.indexOf(option)
      const updated = [...current]
      if (idx >= 0) {
        updated.splice(idx, 1)
      } else {
        updated.push(option)
      }
      this.setData({
        [`selections.${name}`]: updated,
      })
    },

    /** 刻度类型：滑块变化 */
    onScaleChange(e) {
      const { name } = e.currentTarget.dataset
      this.setData({
        [`selections.${name}`]: e.detail.value,
      })
    },

    /** 文本类型：输入变化 */
    onTextInput(e) {
      const { name } = e.currentTarget.dataset
      this.setData({
        [`selections.${name}`]: e.detail.value,
      })
    },

    /** 判断多选项是否选中 */
    isMultiSelected(name, option) {
      const current = this.data.selections[name] || []
      return current.includes(option)
    },

    /** 确认加入购物车 */
    onConfirm() {
      this.triggerEvent('onConfirm', {
        dish: this.data.dish,
        seasonings: { ...this.data.selections },
      })
    },

    /** 关闭面板 */
    onClose() {
      this.triggerEvent('onClose')
    },

    /** 阻止冒泡 */
    preventBubble() {},
  },
})
