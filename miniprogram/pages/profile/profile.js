/**
 * 个人中心页面 - "我的"
 * 显示用户信息、当前餐桌、成员列表、菜单管理入口
 */

const api = require('../../utils/api')
const storage = require('../../utils/storage')
const { logout } = require('../../utils/auth')
const session = require('../../utils/session')
const { buildRoomShare } = require('../../utils/share')
const app = getApp()

Page({
  data: {
    userInfo: null,
    activeRoom: null,
    roomDetail: null,
    members: [],
    isChef: false,
    loading: true,
    rooms: [],
  },

  onLoad(options) {
    this.init()
  },

  onShow() {
    // 从其他页面返回时刷新数据
    if (this.data.userInfo) {
      this.loadRoomDetail()
      this.loadRooms()
    }
  },

  async init() {
    try {
      await app.globalData.loginPromise
      const userInfo = storage.getUserInfo()
      const activeRoom = storage.getActiveRoom()
      const role = activeRoom ? activeRoom.role : ''
      const isChef = role === 'chef'

      this.setData({ userInfo, activeRoom, isChef })

      if (activeRoom) {
        await this.loadRoomDetail()
      }
      await this.loadRooms()
    } catch (err) {
      console.error('个人中心初始化失败:', err)
    } finally {
      this.setData({ loading: false })
    }
  },

  /**
   * 加载我加入的所有餐桌列表（RoomSwitch 模块）
   */
  async loadRooms() {
    try {
      const list = await api.get('/api/rooms/mine', { showError: false })
      const rooms = Array.isArray(list) ? list : []
      this.setData({ rooms })
      storage.setRooms(rooms.map(r => ({
        room_id: r.id,
        name: r.name,
        role: r.chef_id === (this.data.userInfo && this.data.userInfo.id) ? 'chef' : 'guest',
      })))
    } catch (err) {
      console.error('加载餐桌列表失败:', err)
    }
  },

  /**
   * 切换当前活跃餐桌（RoomSwitch 模块）
   */
  async onSwitchRoom(e) {
    const roomId = e.currentTarget.dataset.id
    const rooms = this.data.rooms
    const target = rooms.find(r => String(r.id) === String(roomId))
    if (!target) return

    if (String(target.id) === String(this.data.activeRoom && this.data.activeRoom.id)) return
    const switchRoom = async () => {
      const activeRoom = session.toActiveRoom(target, this.data.userInfo && this.data.userInfo.id)
      app.clearCart()
      storage.setActiveRoom(activeRoom)
      this.setData({ activeRoom, isChef: activeRoom.role === 'chef' })
      await this.loadRoomDetail()
      wx.showToast({ title: '已切换餐桌', icon: 'success' })
    }
    if (!app.getCartCount()) return switchRoom()
    wx.showModal({
      title: '切换餐桌',
      content: '切换后会清空当前购物车，是否继续？',
      success: res => { if (res.confirm) switchRoom() },
    })
  },

  /**
   * 退出餐桌（食客功能）
   */
  onLeaveRoom() {
    const activeRoom = this.data.activeRoom
    if (!activeRoom || !activeRoom.id) return

    wx.showModal({
      title: '确认退出',
      content: `确定退出「${activeRoom.name || '当前餐桌'}」吗？`,
      confirmColor: '#E53935',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await api.delete(`/api/rooms/${activeRoom.id}/leave`)
          storage.removeActiveRoom()
          app.clearCart()
          this.setData({ activeRoom: null, roomDetail: null, members: [] })
          await this.loadRooms()
          const next = this.data.rooms[0]
          if (next) {
            const nextRoom = session.toActiveRoom(next, this.data.userInfo && this.data.userInfo.id)
            storage.setActiveRoom(nextRoom)
            this.setData({ activeRoom: nextRoom, isChef: nextRoom.role === 'chef' })
            await this.loadRoomDetail()
          }
          wx.showToast({ title: '已退出餐桌', icon: 'success' })
          // 如果没有餐桌了，跳转到登录页
          if (this.data.rooms.length === 0) {
            wx.reLaunch({ url: '/pages/login/login' })
          }
        } catch (err) {
          console.error('退出餐桌失败:', err)
        }
      },
    })
  },

  async loadRoomDetail() {
    const activeRoom = this.data.activeRoom
    if (!activeRoom || !activeRoom.id) return

    try {
      const detail = await api.get(`/api/rooms/${activeRoom.id}`, { showError: false })
      this.setData({
        roomDetail: detail,
        members: detail.members || [],
        isChef: detail.role === 'chef' || activeRoom.role === 'chef',
      })
    } catch (err) {
      console.error('加载餐桌详情失败:', err)
    }
  },

  /**
   * 复制邀请码
   */
  onCopyInviteCode() {
    const roomDetail = this.data.roomDetail || this.data.activeRoom
    const code = roomDetail.code || ''
    if (!code) {
      wx.showToast({ title: '暂无邀请码', icon: 'none' })
      return
    }
    wx.setClipboardData({
      data: String(code),
      success() {
        wx.showToast({ title: '已复制', icon: 'success' })
      },
    })
  },

  /**
   * 编辑昵称
   */
  onEditNickname() {
    const userInfo = this.data.userInfo
    const currentNickname = userInfo ? userInfo.nickname : ''

    wx.showModal({
      title: '修改昵称',
      editable: true,
      placeholderText: '请输入新昵称',
      content: currentNickname,
      success: async (res) => {
        if (res.confirm && res.content !== undefined) {
          const newNickname = res.content.trim()
          if (!newNickname) {
            wx.showToast({ title: '昵称不能为空', icon: 'none' })
            return
          }
          if (newNickname === currentNickname) return

          try {
            const updated = await api.put('/api/users/me', { nickname: newNickname })
            storage.setUserInfo(updated)
            this.setData({ userInfo: updated })
            wx.showToast({ title: '修改成功', icon: 'success' })
          } catch (err) {
            console.error('修改昵称失败:', err)
          }
        }
      },
    })
  },

  /**
   * 移除成员（主厨操作）
   */
  onRemoveMember(e) {
    const member = e.currentTarget.dataset.member
    const activeRoom = this.data.activeRoom

    wx.showModal({
      title: '确认移除',
      content: `确定移除「${member.nickname}」吗？`,
      confirmColor: '#E53935',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await api.delete(`/api/rooms/${activeRoom.id}/members/${member.user_id}`)
          wx.showToast({ title: '已移除', icon: 'success' })
          await this.loadRoomDetail()
        } catch (err) {
          console.error('移除成员失败:', err)
        }
      },
    })
  },

  /**
   * 进入菜单管理
   */
  onGoAdmin() {
    wx.navigateTo({ url: '/pages/admin/admin' })
  },

  /**
   * 进入订单总览
   */
  onGoOverview() {
    wx.navigateTo({ url: '/pages/overview/overview' })
  },

  /** 关闭当前餐桌（主厨操作） */
  onCloseRoom() {
    const room = this.data.activeRoom
    if (!room) return
    wx.showModal({
      title: '关闭餐桌',
      content: '关闭后将不能继续点菜，确定关闭吗？',
      confirmColor: '#E53935',
      success: async res => {
        if (!res.confirm) return
        try {
          await api.patch(`/api/rooms/${room.id}/close`)
          app.clearCart()
          storage.removeActiveRoom()
          this.setData({ activeRoom: null, roomDetail: null, members: [], isChef: false })
          await this.loadRooms()
          const next = this.data.rooms[0]
          if (next) {
            const nextRoom = session.toActiveRoom(next, this.data.userInfo && this.data.userInfo.id)
            storage.setActiveRoom(nextRoom)
            this.setData({ activeRoom: nextRoom, isChef: nextRoom.role === 'chef' })
            await this.loadRoomDetail()
          }
          wx.showToast({ title: '餐桌已关闭', icon: 'success' })
          if (!this.data.rooms.length) wx.reLaunch({ url: '/pages/login/login' })
        } catch (err) {
          console.error('关闭餐桌失败:', err)
        }
      },
    })
  },

  /**
   * 退出登录
   */
  onLogout() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      confirmColor: '#E53935',
      success(res) {
        if (res.confirm) {
          logout()
          wx.reLaunch({ url: '/pages/login/login' })
        }
      },
    })
  },

  /**
   * 分享
   */
  onShareAppMessage() {
    const roomDetail = this.data.roomDetail || this.data.activeRoom || {}
    return buildRoomShare(roomDetail)
  },
})
