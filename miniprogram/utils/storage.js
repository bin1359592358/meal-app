/**
 * 本地存储封装
 * 统一管理小程序本地存储的读写操作
 */

const KEYS = {
  TOKEN: 'auth_token',
  USER_INFO: 'user_info',
  ACTIVE_ROOM: 'active_room',
  ROOMS: 'rooms',
  SERVER_URL: 'server_url',
}

// 默认服务器地址（与 Android 端保持一致，指向 Railway 云端）
const DEFAULT_SERVER_URL = 'https://backend-production-a604b.up.railway.app'

const storage = {
  // ─── Token ───
  getToken() {
    return wx.getStorageSync(KEYS.TOKEN) || ''
  },
  setToken(token) {
    wx.setStorageSync(KEYS.TOKEN, token)
  },
  removeToken() {
    wx.removeStorageSync(KEYS.TOKEN)
  },

  // ─── 用户信息 ───
  getUserInfo() {
    return wx.getStorageSync(KEYS.USER_INFO) || null
  },
  setUserInfo(info) {
    wx.setStorageSync(KEYS.USER_INFO, info)
  },
  removeUserInfo() {
    wx.removeStorageSync(KEYS.USER_INFO)
  },

  // ─── 当前餐桌 ───
  getActiveRoom() {
    return wx.getStorageSync(KEYS.ACTIVE_ROOM) || null
  },
  setActiveRoom(room) {
    wx.setStorageSync(KEYS.ACTIVE_ROOM, room)
  },
  removeActiveRoom() {
    wx.removeStorageSync(KEYS.ACTIVE_ROOM)
  },

  // ─── 餐桌列表 ───
  getRooms() {
    return wx.getStorageSync(KEYS.ROOMS) || []
  },
  setRooms(rooms) {
    wx.setStorageSync(KEYS.ROOMS, rooms)
  },

  // ─── 服务器地址 ───
  getServerUrl() {
    const stored = wx.getStorageSync(KEYS.SERVER_URL)
    if (stored && !stored.includes('127.0.0.1') && !stored.includes('localhost')) {
      return stored
    }
    // 迁移：如果存储的是本地地址或空，使用云端地址
    if (stored && (stored.includes('127.0.0.1') || stored.includes('localhost'))) {
      this.setServerUrl(DEFAULT_SERVER_URL)
    }
    return DEFAULT_SERVER_URL
  },
  setServerUrl(url) {
    wx.setStorageSync(KEYS.SERVER_URL, url)
  },

  // ─── 清除所有登录态（保留服务器地址）───
  clearAuth() {
    const serverUrl = this.getServerUrl()
    wx.clearStorageSync()
    this.setServerUrl(serverUrl)
  },

  // ─── 是否已登录 ───
  isLoggedIn() {
    return !!this.getToken()
  },
}

module.exports = storage
