/**
 * 菜品编辑/创建页面（主厨专属）
 * 通过 query 参数 id 判断模式：id === "-1" 为创建，否则为编辑
 */

const api = require('../../utils/api')
const storage = require('../../utils/storage')
const app = getApp()

Page({
  data: {
    roomId: '',
    dishId: null,
    isCreate: true,
    loading: false,

    // 表单字段
    name: '',
    description: '',
    price: '',
    categoryId: null,
    categoryIndex: 0,
    categories: [],
    tags: [],
    tagInput: '',
    imageUrl: '',
    localImagePath: '',
    isAvailable: true,
    seasonings: [],
    seasoningTypes: ['single', 'multi', 'scale', 'text'],
    seasoningTypeNames: ['单选', '多选', '滑块', '文字'],
  },

  onLoad(options) {
    const isCreate = options.id === '-1'
    this.setData({
      dishId: isCreate ? null : options.id,
      isCreate,
    })

    // 动态设置标题
    wx.setNavigationBarTitle({
      title: isCreate ? '新建菜品' : '编辑菜品',
    })

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

      // 加载分类列表
      await this.loadCategories()

      // 编辑模式：加载菜品详情
      if (!this.data.isCreate) {
        await this.loadDish()
      }
    } catch (err) {
      console.error('菜品编辑初始化失败:', err)
    }
  },

  async loadCategories() {
    try {
      const data = await api.get(`/api/rooms/${this.data.roomId}/categories`)
      const categories = Array.isArray(data) ? data : []
      this.setData({ categories })

      // 创建模式默认选中第一个分类
      if (this.data.isCreate && categories.length > 0 && !this.data.categoryId) {
        this.setData({ categoryId: categories[0].id, categoryIndex: 0 })
      }
    } catch (err) {
      console.error('加载分类失败:', err)
    }
  },

  async loadDish() {
    this.setData({ loading: true })
    try {
      const dish = await api.get(`/api/rooms/${this.data.roomId}/dishes/${this.data.dishId}`)
      const categories = this.data.categories
      const catIndex = categories.findIndex(c => c.id === dish.category_id)

      this.setData({
        name: dish.name || '',
        description: dish.description || '',
        price: String(dish.price || ''),
        categoryId: dish.category_id || null,
        categoryIndex: catIndex >= 0 ? catIndex : 0,
        tags: dish.tags || [],
        imageUrl: dish.image_url || '',
        isAvailable: dish.is_available !== false,
        seasonings: (dish.seasonings || []).map(item => ({
          ...item,
          typeIndex: ['single', 'multi', 'scale', 'text'].indexOf(item.type),
          optionsText: (item.options || []).join('、'),
        })),
        loading: false,
      })
    } catch (err) {
      this.setData({ loading: false })
      console.error('加载菜品详情失败:', err)
    }
  },

  // ── 表单输入 ──

  onNameInput(e) {
    this.setData({ name: e.detail.value })
  },

  onDescInput(e) {
    this.setData({ description: e.detail.value })
  },

  onPriceInput(e) {
    this.setData({ price: e.detail.value })
  },

  onCategoryChange(e) {
    const index = Number(e.detail.value)
    const cat = this.data.categories[index]
    if (cat) {
      this.setData({ categoryId: cat.id, categoryIndex: index })
    }
  },

  onTagInput(e) {
    this.setData({ tagInput: e.detail.value })
  },

  onAddTag() {
    const tag = this.data.tagInput.trim()
    if (!tag) return
    if (this.data.tags.includes(tag)) {
      wx.showToast({ title: '标签已存在', icon: 'none' })
      return
    }
    this.setData({
      tags: [...this.data.tags, tag],
      tagInput: '',
    })
  },

  onRemoveTag(e) {
    const index = e.currentTarget.dataset.index
    const tags = [...this.data.tags]
    tags.splice(index, 1)
    this.setData({ tags })
  },

  /**
   * 选择图片
   */
  async onChooseImage() {
    try {
      const res = await new Promise((resolve, reject) => {
        wx.chooseMedia({
          count: 1,
          mediaType: ['image'],
          sourceType: ['album', 'camera'],
          success: resolve,
          fail: reject,
        })
      })

      const tempFile = res.tempFiles[0]
      this.setData({ localImagePath: tempFile.tempFilePath })

      // 上传
      const uploadRes = await api.upload(tempFile.tempFilePath)
      this.setData({ imageUrl: uploadRes.url || uploadRes.image_url || '' })
      wx.showToast({ title: '上传成功', icon: 'success' })
    } catch (err) {
      if (err.errMsg && err.errMsg.includes('cancel')) return
      console.error('选择图片失败:', err)
    }
  },

  onAvailableChange(e) {
    this.setData({ isAvailable: e.detail.value })
  },

  onAddSeasoning() {
    if (this.data.seasonings.length >= 10) return
    this.setData({
      seasonings: [...this.data.seasonings, { name: '', type: 'single', typeIndex: 0, optionsText: '默认、少量、多量' }],
    })
  },

  onRemoveSeasoning(e) {
    const seasonings = [...this.data.seasonings]
    seasonings.splice(Number(e.currentTarget.dataset.index), 1)
    this.setData({ seasonings })
  },

  onSeasoningInput(e) {
    const { index, field } = e.currentTarget.dataset
    this.setData({ [`seasonings[${index}].${field}`]: e.detail.value })
  },

  onSeasoningTypeChange(e) {
    const index = Number(e.currentTarget.dataset.index)
    const type = this.data.seasoningTypes[Number(e.detail.value)]
    const item = { ...this.data.seasonings[index], type, typeIndex: Number(e.detail.value) }
    if ((type === 'single' || type === 'multi') && !item.optionsText) item.optionsText = '默认、少量、多量'
    if (type === 'scale') Object.assign(item, { min: 0, max: 5, step: 1 })
    const seasonings = [...this.data.seasonings]
    seasonings[index] = item
    this.setData({ seasonings })
  },

  buildSeasonings() {
    return this.data.seasonings.map(item => {
      const result = { name: String(item.name || '').trim(), type: item.type }
      if (item.type === 'single' || item.type === 'multi') {
        result.options = String(item.optionsText || '').split(/[、,，]/).map(value => value.trim()).filter(Boolean)
      } else if (item.type === 'scale') {
        result.min = Number(item.min)
        result.max = Number(item.max)
        result.step = Number(item.step) || 1
      } else {
        result.max_length = Number(item.max_length) || 100
      }
      return result
    })
  },

  /**
   * 获取分类选中索引（用于 picker 显示）
   */
  getCategoryIndex() {
    const { categories, categoryId } = this.data
    const index = categories.findIndex(c => c.id === categoryId)
    return index >= 0 ? index : 0
  },

  /**
   * 保存
   */
  async onSave() {
    const { name, price, categoryId, description, tags, imageUrl, isAvailable, roomId, dishId, isCreate } = this.data

    // 校验
    if (!name.trim()) {
      wx.showToast({ title: '请输入菜品名称', icon: 'none' })
      return
    }
    if (!price || isNaN(Number(price)) || Number(price) < 0) {
      wx.showToast({ title: '请输入有效价格', icon: 'none' })
      return
    }
    if (!categoryId) {
      wx.showToast({ title: '请选择分类', icon: 'none' })
      return
    }
    if (this.data.seasonings.some(item => !String(item.name || '').trim())) {
      wx.showToast({ title: '请填写调味项名称', icon: 'none' })
      return
    }
    if (this.data.seasonings.some(item =>
      (item.type === 'single' || item.type === 'multi') &&
      !String(item.optionsText || '').split(/[、,，]/).some(value => value.trim())
    )) {
      wx.showToast({ title: '请填写调味选项', icon: 'none' })
      return
    }

    const body = {
      category_id: categoryId,
      name: name.trim(),
      description: description.trim(),
      price: Number(price),
      image_url: imageUrl,
      tags,
      is_available: isAvailable,
      seasonings: this.buildSeasonings(),
    }

    try {
      wx.showLoading({ title: '保存中...' })

      if (isCreate) {
        await api.post(`/api/rooms/${roomId}/dishes`, body, { showError: false })
      } else {
        await api.put(`/api/rooms/${roomId}/dishes/${dishId}`, body, { showError: false })
      }

      wx.hideLoading()
      wx.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(() => {
        wx.navigateBack()
      }, 800)
    } catch (err) {
      wx.hideLoading()
      console.error('保存菜品失败:', err)
    }
  },

  /**
   * 删除菜品
   */
  onDelete() {
    const { dishId, roomId, name } = this.data

    wx.showModal({
      title: '确认删除',
      content: `确定删除菜品「${name}」吗？此操作不可恢复。`,
      confirmColor: '#E53935',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await api.delete(`/api/rooms/${roomId}/dishes/${dishId}`)
          wx.showToast({ title: '已删除', icon: 'success' })
          setTimeout(() => {
            wx.navigateBack()
          }, 800)
        } catch (err) {
          console.error('删除菜品失败:', err)
        }
      },
    })
  },
})
