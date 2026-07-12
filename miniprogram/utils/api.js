/**
 * API 请求封装
 * - 所有请求自动附加 Authorization: Bearer <token>
 * - 401 响应自动清除登录态
 * - 统一错误提示
 */

const storage = require('./storage')

/**
 * 发送 HTTP 请求
 * @param {string} path - API 路径（如 /api/auth/login）
 * @param {object} options - 请求选项
 * @param {string} options.method - 请求方法，默认 GET
 * @param {object} options.data - 请求体数据
 * @param {boolean} options.showLoading - 是否显示加载中，默认 true
 * @param {boolean} options.showError - 是否显示错误提示，默认 true
 * @param {boolean} options.raw - 是否返回原始响应（不解析 ApiResponse 包装），默认 false
 * @returns {Promise<object>} 响应数据
 */
function request(path, options = {}) {
  const {
    method = 'GET',
    data = null,
    showLoading = false,
    showError = true,
    raw = false,
  } = options

  const baseUrl = storage.getServerUrl()
  const token = storage.getToken()
  const url = `${baseUrl}${path}`

  const header = {
    'Content-Type': 'application/json',
  }
  if (token) {
    header['Authorization'] = `Bearer ${token}`
  }

  if (showLoading) {
    wx.showLoading({ title: '加载中...' })
  }

  return new Promise((resolve, reject) => {
    wx.request({
      url,
      method,
      data,
      header,
      success(res) {
        if (showLoading) wx.hideLoading()

        // HTTP 状态码检查
        if (res.statusCode === 401) {
          storage.clearAuth()
          wx.reLaunch({ url: '/pages/login/login' })
          reject(new Error('登录已过期，请重新登录'))
          return
        }

        if (res.statusCode < 200 || res.statusCode >= 300) {
          const errMsg = (res.data && res.data.detail) || `请求失败 (${res.statusCode})`
          if (showError) {
            wx.showToast({ title: errMsg, icon: 'none', duration: 2000 })
          }
          reject(new Error(errMsg))
          return
        }

        // 解析 ApiResponse 包装
        if (raw) {
          resolve(res.data)
          return
        }

        const body = res.data
        if (body && body.code !== undefined) {
          if (body.code === 0) {
            resolve(body.data)
          } else {
            const errMsg = body.message || '请求失败'
            if (showError) {
              wx.showToast({ title: errMsg, icon: 'none', duration: 2000 })
            }
            reject(new Error(errMsg))
          }
        } else {
          resolve(body)
        }
      },
      fail(err) {
        if (showLoading) wx.hideLoading()
        console.error('[API] 请求失败:', url, err)
        const errMsg = err.errMsg || '网络请求失败'
        if (showError) {
          wx.showToast({ title: errMsg, icon: 'none', duration: 3000 })
        }
        reject(new Error(errMsg))
      },
    })
  })
}

// 快捷方法
const api = {
  get(path, options = {}) {
    return request(path, { ...options, method: 'GET' })
  },
  post(path, data, options = {}) {
    return request(path, { ...options, method: 'POST', data })
  },
  put(path, data, options = {}) {
    return request(path, { ...options, method: 'PUT', data })
  },
  patch(path, data, options = {}) {
    return request(path, { ...options, method: 'PATCH', data })
  },
  delete(path, options = {}) {
    return request(path, { ...options, method: 'DELETE' })
  },

  /**
   * 上传文件
   * @param {string} filePath - 本地文件路径
   * @param {string} name - 文件字段名
   * @returns {Promise<object>} 上传结果
   */
  upload(filePath, name = 'file') {
    const baseUrl = storage.getServerUrl()
    const token = storage.getToken()

    wx.showLoading({ title: '上传中...' })

    return new Promise((resolve, reject) => {
      wx.uploadFile({
        url: `${baseUrl}/api/upload/image`,
        filePath,
        name,
        header: {
          'Authorization': `Bearer ${token}`,
        },
        success(res) {
          wx.hideLoading()
          if (res.statusCode === 200) {
            const data = JSON.parse(res.data)
            resolve(data.data || data)
          } else {
            wx.showToast({ title: '上传失败', icon: 'none' })
            reject(new Error('上传失败'))
          }
        },
        fail() {
          wx.hideLoading()
          wx.showToast({ title: '上传失败', icon: 'none' })
          reject(new Error('上传失败'))
        },
      })
    })
  },
}

module.exports = api
