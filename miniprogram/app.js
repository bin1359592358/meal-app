/**
 * 小程序全局入口
 * 负责：静默登录、全局购物车状态、全局数据
 */

const { silentLogin } = require('./utils/auth')
const storage = require('./utils/storage')
const cartUtils = require('./utils/cart')

App({
  globalData: {
    userInfo: null,
    cart: cartUtils.createCart(),
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
          cartUtils.ensureRoom(this.globalData.cart, room.id)
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
    return cartUtils.getCount(this.globalData.cart)
  },

  /**
   * 获取购物车总价
   */
  getCartTotal() {
    return cartUtils.getTotal(this.globalData.cart)
  },

  /**
   * 添加菜品到购物车
   * @param {object} dish - 菜品对象
   * @param {object} seasonings - 调味选择
   */
  addToCart(dish, seasonings = {}) {
    const room = storage.getActiveRoom()
    return cartUtils.addItem(this.globalData.cart, dish, seasonings, room && room.id)
  },

  /**
   * 从购物车移除菜品
   */
  removeFromCart(key) {
    cartUtils.removeItem(this.globalData.cart, key)
  },

  /**
   * 清空购物车
   */
  clearCart() {
    cartUtils.clear(this.globalData.cart)
  },

  /**
   * 更新购物车商品数量
   */
  updateCartItemQty(key, qty) {
    cartUtils.setQuantity(this.globalData.cart, key, qty)
  },
})
