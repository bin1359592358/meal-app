/**
 * 小程序全局入口
 * 负责：静默登录、全局购物车状态、全局数据
 */

const { silentLogin } = require('./utils/auth')
const storage = require('./utils/storage')

App({
  globalData: {
    userInfo: null,
    cart: {
      items: [],    // [{ dish, quantity, seasonings }]
      roomId: null, // 关联的餐桌 ID
    },
    loginPromise: null, // 登录 Promise，供页面等待登录完成
  },

  onLaunch() {
    // 执行静默登录
    this.globalData.loginPromise = silentLogin()
      .then(user => {
        this.globalData.userInfo = user
        // 检查是否有活跃餐桌
        const room = storage.getActiveRoom()
        if (room) {
          // 有餐桌，购物车关联
          this.globalData.cart.roomId = room.id
        }
        return user
      })
      .catch(err => {
        console.error('静默登录失败:', err)
        return null
      })
  },

  /**
   * 获取购物车总数量
   */
  getCartCount() {
    return this.globalData.cart.items.reduce((sum, item) => sum + item.quantity, 0)
  },

  /**
   * 获取购物车总价
   */
  getCartTotal() {
    return this.globalData.cart.items.reduce(
      (sum, item) => sum + item.dish.price * item.quantity, 0
    )
  },

  /**
   * 添加菜品到购物车
   * @param {object} dish - 菜品对象
   * @param {object} seasonings - 调味选择
   */
  addToCart(dish, seasonings = {}) {
    const cart = this.globalData.cart
    // 检查是否已在购物车中（同菜品 + 同调味）
    const seasoningKey = JSON.stringify(seasonings)
    const existing = cart.items.find(
      item => item.dish.id === dish.id && JSON.stringify(item.seasonings) === seasoningKey
    )
    if (existing) {
      existing.quantity++
    } else {
      cart.items.push({
        dish,
        quantity: 1,
        seasonings,
      })
    }
    cart.roomId = storage.getActiveRoom()?.id
  },

  /**
   * 从购物车移除菜品
   */
  removeFromCart(index) {
    this.globalData.cart.items.splice(index, 1)
  },

  /**
   * 清空购物车
   */
  clearCart() {
    this.globalData.cart.items = []
  },

  /**
   * 更新购物车商品数量
   */
  updateCartItemQty(index, qty) {
    if (qty <= 0) {
      this.removeFromCart(index)
    } else {
      this.globalData.cart.items[index].quantity = qty
    }
  },
})
