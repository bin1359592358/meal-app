/**
 * 菜单管理页面（主厨专属）
 * 左侧分类列表 + 右侧菜品列表
 */

const api = require('../../utils/api')
const storage = require('../../utils/storage')
const app = getApp()

Page({
  data: {
    roomId: '',
    categories: [],
    dishes: [],
    selectedCatId: null,
    loading: true,

    // 新增分类弹窗
    showCategoryDialog: false,
    newCatName: '',
    newCatIcon: '',
    emojiOptions: ['🍖', '🍗', '🥩', '🍕', '🍔', '🌮', '🍜', '🍲', '🥗', '🍣', '🍰', '🧁', '☕', '🍺', '🥤', '🍱'],

    // 编辑分类弹窗
    showEditCategoryDialog: false,
    editCatId: null,
    editCatName: '',
    editCatIcon: '',
  },

  onLoad(options) {
    this.init()
  },

  onShow() {
    // 从 dish-edit 返回时刷新
    if (this.data.roomId) {
      this.loadCategories()
      if (this.data.selectedCatId) {
        this.loadDishes()
      }
    }
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
      await this.loadCategories()
    } catch (err) {
      console.error('菜单管理初始化失败:', err)
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadCategories() {
    try {
      const data = await api.get(`/api/rooms/${this.data.roomId}/categories`)
      const categories = Array.isArray(data) ? data : []
      this.setData({ categories })

      // 自动选中第一个分类（如果之前没选）
      if (categories.length > 0 && !this.data.selectedCatId) {
        this.setData({ selectedCatId: categories[0].id })
        await this.loadDishes()
      } else if (this.data.selectedCatId) {
        // 检查当前选中分类是否还在
        const exists = categories.find(c => c.id === this.data.selectedCatId)
        if (exists) {
          await this.loadDishes()
        } else if (categories.length > 0) {
          this.setData({ selectedCatId: categories[0].id })
          await this.loadDishes()
        } else {
          this.setData({ selectedCatId: null, dishes: [] })
        }
      }
    } catch (err) {
      console.error('加载分类失败:', err)
    }
  },

  async loadDishes() {
    if (!this.data.selectedCatId) return
    try {
      const data = await api.get(
        `/api/rooms/${this.data.roomId}/dishes?category_id=${this.data.selectedCatId}`
      )
      const dishes = Array.isArray(data) ? data : []
      this.setData({ dishes })
    } catch (err) {
      console.error('加载菜品失败:', err)
    }
  },

  /**
   * 选中分类
   */
  onSelectCategory(e) {
    const catId = e.currentTarget.dataset.id
    this.setData({ selectedCatId: catId })
    this.loadDishes()
  },

  /**
   * 打开新增分类弹窗
   */
  onOpenCategoryDialog() {
    this.setData({
      showCategoryDialog: true,
      newCatName: '',
      newCatIcon: '🍖',
    })
  },

  onCloseCategoryDialog() {
    this.setData({ showCategoryDialog: false })
  },

  onCatNameInput(e) {
    this.setData({ newCatName: e.detail.value })
  },

  onSelectEmoji(e) {
    this.setData({ newCatIcon: e.currentTarget.dataset.emoji })
  },

  /**
   * 提交新增分类
   */
  async onSubmitCategory() {
    const { newCatName, newCatIcon, roomId } = this.data
    if (!newCatName.trim()) {
      wx.showToast({ title: '请输入分类名称', icon: 'none' })
      return
    }

    try {
      await api.post(`/api/rooms/${roomId}/categories`, {
        name: newCatName.trim(),
        icon: newCatIcon,
      })
      this.setData({ showCategoryDialog: false })
      wx.showToast({ title: '添加成功', icon: 'success' })
      await this.loadCategories()
    } catch (err) {
      console.error('添加分类失败:', err)
    }
  },

  /**
   * 删除分类
   */
  onDeleteCategory(e) {
    const catId = e.currentTarget.dataset.id
    const catName = e.currentTarget.dataset.name

    wx.showModal({
      title: '确认删除',
      content: `确定删除分类「${catName}」及其所有菜品吗？`,
      confirmColor: '#E53935',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await api.delete(`/api/rooms/${this.data.roomId}/categories/${catId}`)
          wx.showToast({ title: '已删除', icon: 'success' })
          await this.loadCategories()
        } catch (err) {
          console.error('删除分类失败:', err)
        }
      },
    })
  },

  /**
   * 打开编辑分类弹窗
   */
  onEditCategory(e) {
    const catId = e.currentTarget.dataset.id
    const cat = this.data.categories.find(c => c.id === catId)
    if (!cat) return

    this.setData({
      showEditCategoryDialog: true,
      editCatId: catId,
      editCatName: cat.name,
      editCatIcon: cat.icon || '🍖',
    })
  },

  onCloseEditCategoryDialog() {
    this.setData({ showEditCategoryDialog: false })
  },

  onEditCatNameInput(e) {
    this.setData({ editCatName: e.detail.value })
  },

  onSelectEditEmoji(e) {
    this.setData({ editCatIcon: e.currentTarget.dataset.emoji })
  },

  /**
   * 提交编辑分类
   */
  async onSubmitEditCategory() {
    const { editCatId, editCatName, editCatIcon, roomId } = this.data
    if (!editCatName.trim()) {
      wx.showToast({ title: '请输入分类名称', icon: 'none' })
      return
    }

    try {
      await api.put(`/api/rooms/${roomId}/categories/${editCatId}`, {
        name: editCatName.trim(),
        icon: editCatIcon,
      })
      this.setData({ showEditCategoryDialog: false })
      wx.showToast({ title: '修改成功', icon: 'success' })
      await this.loadCategories()
    } catch (err) {
      console.error('编辑分类失败:', err)
    }
  },

  /**
   * 切换菜品上下架
   */
  async onToggleDish(e) {
    const dishId = e.currentTarget.dataset.id
    try {
      await api.patch(`/api/rooms/${this.data.roomId}/dishes/${dishId}/toggle`)
      await this.loadDishes()
    } catch (err) {
      console.error('切换菜品状态失败:', err)
    }
  },

  /**
   * 编辑菜品
   */
  onEditDish(e) {
    const dishId = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/dish-edit/dish-edit?id=${dishId}` })
  },

  /**
   * 删除菜品
   */
  onDeleteDish(e) {
    const dishId = e.currentTarget.dataset.id
    const dishName = e.currentTarget.dataset.name

    wx.showModal({
      title: '确认删除',
      content: `确定删除菜品「${dishName}」吗？`,
      confirmColor: '#E53935',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await api.delete(`/api/rooms/${this.data.roomId}/dishes/${dishId}`)
          wx.showToast({ title: '已删除', icon: 'success' })
          await this.loadDishes()
          // 也刷新分类（菜品数量变化）
          this.loadCategories()
        } catch (err) {
          console.error('删除菜品失败:', err)
        }
      },
    })
  },

  /**
   * 新建菜品
   */
  onAddDish() {
    wx.navigateTo({ url: '/pages/dish-edit/dish-edit?id=-1' })
  },

  /**
   * 阻止弹窗背景滚动
   */
  preventTouchMove() {},
})
