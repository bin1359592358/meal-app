/**
 * 微信登录流程
 * wx.login() 获取 code → 后端换 token → 存储登录态
 */

const api = require('./api')
const storage = require('./storage')

/**
 * 执行微信登录
 * @param {string} [nickname] - 可选的用户昵称
 * @returns {Promise<object>} { user, token }
 */
async function wxLogin(nickname) {
  const serverUrl = storage.getServerUrl()
  console.log('[Auth] 开始微信登录, 服务器:', serverUrl)

  return new Promise((resolve, reject) => {
    wx.login({
      success: async (loginRes) => {
        console.log('[Auth] wx.login 成功, code:', loginRes.code ? loginRes.code.substring(0, 10) + '...' : 'null')
        if (!loginRes.code) {
          reject(new Error('微信登录失败: 未获取到code'))
          return
        }
        try {
          console.log('[Auth] 正在调用 wechat-login 接口...')
          const data = await api.post('/api/auth/wechat-login', {
            code: loginRes.code,
            nickname: nickname || undefined,
          }, { showError: true })
          console.log('[Auth] wechat-login 成功:', data.user ? data.user.nickname : 'unknown')
          storage.setToken(data.token)
          storage.setUserInfo(data.user)
          resolve(data)
        } catch (e) {
          console.error('[Auth] wechat-login 接口失败:', e.message)
          reject(e)
        }
      },
      fail: (err) => {
        console.error('[Auth] wx.login 失败:', err)
        reject(new Error('微信登录失败: ' + (err.errMsg || err)))
      },
    })
  })
}

/**
 * 静默登录（应用启动时调用）
 * 如果本地已有 token 且有效，直接返回；否则执行微信登录
 * @returns {Promise<object>} 用户信息
 */
async function silentLogin() {
  // 检查本地 token
  const token = storage.getToken()
  if (token) {
    try {
      const user = await api.get('/api/users/me', { showError: false })
      storage.setUserInfo(user)
      return user
    } catch (e) {
      // token 无效，需要重新登录
      storage.clearAuth()
    }
  }
  // 执行微信登录
  const data = await wxLogin()
  return data.user
}

/**
 * 退出登录
 */
function logout() {
  const token = storage.getToken()
  if (token) {
    api.post('/api/auth/logout', {}, { showError: false }).catch(() => {})
  }
  storage.clearAuth()
  try {
    const app = getApp()
    if (app && app.clearCart) app.clearCart()
  } catch (e) {}
}

module.exports = { wxLogin, silentLogin, logout }
