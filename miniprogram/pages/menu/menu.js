/**
 * 菜单页 - 主点菜页面
 * 左侧分类导航 + 右侧菜品列表 + 底部购物车栏
 */
const api = require('../../utils/api')
const storage = require('../../utils/storage')
const { buildRoomShare } = require('../../utils/share')
const app = getApp()

/** 搜索防抖定时器 */
let searchTimer = null
let searchRequestId = 0

Page({
  data: {
    /** 分类列表 */
    categories: [],
    /** 当前选中分类索引 */
    selectedCatIdx: 0,
    /** 当前分类下的菜品列表 */
    dishes: [],
    /** 搜索关键词 */
    searchKeyword: '',
    /** 是否处于搜索模式 */
    isSearchMode: false,
    /** 搜索结果 */
    searchResults: [],
    /** 购物车数量 */
    cartCount: 0,
    /** 购物车总价 */
    cartTotal: 0,
    /** 是否为管理员/厨师角色 */
    isChef: false,
    /** 调味面板是否可见 */
    seasoningVisible: false,
    /** 当前正在选择调味的菜品 */
    seasoningDish: null,
    /** 加载状态 */
    loading: false,
    /** 菜品数量映射（dishId -> quantity） */
    qtyMap: {},
    loadedRoomId: null,
    activeRoomName: '',
  },

  onLoad(options) {
    this._checkRole()
  },

  onShow() {
    const room = storage.getActiveRoom()
    const roomId = room && room.id
    if (String(roomId || '') !== String(this.data.loadedRoomId || '')) {
      this.setData({ searchKeyword: '', isSearchMode: false, searchResults: [], dishes: [], categories: [], activeRoomName: (room && room.name) || '' })
      this._checkRole()
      this._initData()
      return
    }
    this._refreshCart()
  },

  /** 下拉刷新 */
  onPullDownRefresh() {
    this._initData().then(() => {
      wx.stopPullDownRefresh()
    }).catch(() => {
      wx.stopPullDownRefresh()
    })
  },

  /** 检查用户角色 */
  _checkRole() {
    const userInfo = storage.getUserInfo()
    const room = storage.getActiveRoom()
    const role = (userInfo && userInfo.role) || (room && room.role) || 'guest'
    this.setData({ isChef: role === 'chef' })
  },

  /** 初始化数据：等待登录后加载分类和菜品 */
  async _initData() {
    this.setData({ loading: true })
    try {
      await app.globalData.loginPromise
      if (!storage.isLoggedIn()) {
        wx.redirectTo({ url: '/pages/login/login' })
        return
      }
      await this._loadCategories()
      if (this.data.categories.length > 0) {
        await this._loadDishes(0)
      }
      const room = storage.getActiveRoom()
      this.setData({ loadedRoomId: room && room.id, activeRoomName: (room && room.name) || '' })
    } catch (err) {
      console.error('初始化菜单失败:', err)
      wx.showToast({ title: '加载失败，请下拉刷新', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  /** 加载分类列表 */
  async _loadCategories() {
    const room = storage.getActiveRoom()
    if (!room) {
      wx.showToast({ title: '请先加入餐桌', icon: 'none' })
      wx.redirectTo({ url: '/pages/login/login' })
      return
    }
    const data = await api.get(`/api/rooms/${room.id}/categories`)
    const categories = Array.isArray(data) ? data : (data.items || data.list || [])
    this.setData({ categories, selectedCatIdx: 0 })
  },

  /** 加载指定分类下的菜品 */
  async _loadDishes(catIdx) {
    const room = storage.getActiveRoom()
    const cat = this.data.categories[catIdx]
    if (!room || !cat) return

    this.setData({ loading: true })
    try {
      const data = await api.get(`/api/rooms/${room.id}/dishes?category_id=${cat.id}`)
      const dishes = Array.isArray(data) ? data : (data.items || data.list || [])
      this.setData({ dishes, selectedCatIdx: catIdx })
      this._buildQtyMap()
    } catch (err) {
      console.error('加载菜品失败:', err)
    } finally {
      this.setData({ loading: false })
    }
  },

  /** 构建购物车数量映射 */
  _buildQtyMap() {
    const qtyMap = {}
    const items = app.globalData.cart.items || []
    items.forEach(item => {
      qtyMap[item.dish.id] = (qtyMap[item.dish.id] || 0) + item.quantity
    })
    this.setData({ qtyMap })
  },

  /** 刷新购物车状态 */
  _refreshCart() {
    this.setData({
      cartCount: app.getCartCount(),
      cartTotal: app.getCartTotal(),
    })
    this._buildQtyMap()
  },

  // ─── 分类切换 ───

  onCategoryTap(e) {
    const idx = e.currentTarget.dataset.index
    if (idx === this.data.selectedCatIdx && !this.data.isSearchMode) return
    this.setData({ isSearchMode: false, searchKeyword: '' })
    this._loadDishes(idx)
  },

  // ─── 搜索 ───

  onSearchInput(e) {
    const keyword = e.detail.value
    this.setData({ searchKeyword: keyword })

    if (searchTimer) clearTimeout(searchTimer)

    if (!keyword.trim()) {
      searchRequestId++
      this.setData({ isSearchMode: false, searchResults: [] })
      return
    }

    searchTimer = setTimeout(() => {
      this._doSearch(keyword.trim())
    }, 400)
  },

  onSearchClear() {
    searchRequestId++
    this.setData({ searchKeyword: '', isSearchMode: false, searchResults: [] })
  },

  async _doSearch(keyword) {
    const room = storage.getActiveRoom()
    if (!room) return

    const requestId = ++searchRequestId
    try {
      const data = await api.get(`/api/rooms/${room.id}/dishes?q=${encodeURIComponent(keyword)}`)
      if (requestId !== searchRequestId || keyword !== this.data.searchKeyword.trim()) return
      const results = Array.isArray(data) ? data : (data.items || data.list || [])
      this.setData({ isSearchMode: true, searchResults: results })
      this._buildQtyMap()
    } catch (err) {
      console.error('搜索失败:', err)
    }
  },

  // ─── 购物车操作 ───

  /** 添加菜品到购物车 */
  onDishAdd(e) {
    const dish = e.detail.dish
    if (!dish || dish.is_available === false) {
      wx.showToast({ title: '这道菜暂时售罄', icon: 'none' })
      return
    }

    // 有调味选项，弹出调味面板
    if (dish.seasonings && dish.seasonings.length > 0) {
      this.setData({
        seasoningDish: dish,
        seasoningVisible: true,
      })
      return
    }

    app.addToCart(dish, {})
    this._refreshCart()
    wx.showToast({ title: '已加入购物车', icon: 'success', duration: 600 })
  },

  /** 减少菜品 */
  onDishMinus(e) {
    const dish = e.detail.dish
    const items = app.globalData.cart.items
    // 从后向前找匹配项删除
    for (let i = items.length - 1; i >= 0; i--) {
      if (items[i].dish.id === dish.id) {
        if (items[i].quantity > 1) {
          app.updateCartItemQty(items[i].key, items[i].quantity - 1)
        } else {
          app.removeFromCart(items[i].key)
        }
        break
      }
    }
    this._refreshCart()
  },

  /** 点击菜品卡片 */
  onDishTap(e) {
    const dish = e.detail.dish
    this.setData({ seasoningDish: dish, seasoningVisible: true })
  },

  /** 调味面板确认 */
  onSeasoningConfirm(e) {
    const { dish, seasonings } = e.detail
    if (!dish || dish.is_available === false) {
      wx.showToast({ title: '这道菜暂时售罄', icon: 'none' })
      return
    }
    app.addToCart(dish, seasonings)
    this.setData({ seasoningVisible: false, seasoningDish: null })
    this._refreshCart()
    wx.showToast({ title: '已加入购物车', icon: 'success', duration: 600 })
  },

  /** 调味面板关闭 */
  onSeasoningClose() {
    this.setData({ seasoningVisible: false, seasoningDish: null })
  },

  /** 去结算 */
  onCheckout() {
    wx.navigateTo({ url: '/pages/cart/cart' })
  },

  /** 从当前结果中随机推荐一道可点菜品 */
  onRandomDish() {
    const source = this.data.isSearchMode ? this.data.searchResults : this.data.dishes
    const available = source.filter(dish => dish.is_available !== false)
    if (!available.length) {
      wx.showToast({ title: '当前没有可推荐的菜品', icon: 'none' })
      return
    }
    const dish = available[Math.floor(Math.random() * available.length)]
    this.setData({ seasoningDish: dish, seasoningVisible: true })
  },

  /** 进入管理后台 */
  onAdminTap() {
    wx.navigateTo({ url: '/pages/admin/admin' })
  },

  /** 获取菜品在购物车中的数量 */
  getDishQty(dishId) {
    return this.data.qtyMap[dishId] || 0
  },

  onShareAppMessage() {
    return buildRoomShare(storage.getActiveRoom())
  },
})
