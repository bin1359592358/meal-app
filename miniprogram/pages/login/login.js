// pages/login/login.js

const api    = require('../../utils/api');
const storage = require('../../utils/storage');
const { wxLogin } = require('../../utils/auth');
const app    = getApp();

Page({
  data: {
    activeTab  : 'create',
    roomName   : '',
    inviteCode : '',
    loading    : false,

    // 微信登录失败回退
    loginFailed: false,
    loginError : '',
    fbUsername : '',
    fbPin      : '',
    fbMode     : 'login',  // 'login' | 'register'
    fbNickname : '',
  },

  onLoad(options) {
    if (options.inviteCode) {
      this.setData({
        activeTab  : 'join',
        inviteCode : options.inviteCode,
      });
      this._autoJoinAfterLogin = true;
    }

    // 检查登录状态
    this._checkLoginStatus();
  },

  async _checkLoginStatus() {
    try {
      await app.globalData.loginPromise;
      if (!storage.isLoggedIn()) {
        this.setData({
          loginFailed: true,
          loginError: '微信登录未成功，可使用账号密码登录',
        });
      } else if (this._autoJoinAfterLogin) {
        this._tryAutoJoin();
      }
    } catch (err) {
      this.setData({
        loginFailed: true,
        loginError: '登录异常: ' + (err.message || err),
      });
    }
  },

  onTabTap(e) {
    const tab = e.currentTarget.dataset.tab;
    if (tab === this.data.activeTab) return;
    this.setData({ activeTab: tab });
  },

  onRoomNameInput(e) {
    this.setData({ roomName: e.detail.value.trim() });
  },

  onInviteCodeInput(e) {
    this.setData({ inviteCode: e.detail.value.trim() });
  },

  // ─── 回退登录相关 ───

  onFbUsernameInput(e) {
    this.setData({ fbUsername: e.detail.value.trim() });
  },

  onFbPinInput(e) {
    this.setData({ fbPin: e.detail.value.trim() });
  },

  onFbNicknameInput(e) {
    this.setData({ fbNickname: e.detail.value.trim() });
  },

  onFbModeSwitch() {
    this.setData({
      fbMode: this.data.fbMode === 'login' ? 'register' : 'login',
    });
  },

  /** 回退登录（用户名+PIN） */
  async onFallbackLogin() {
    const { fbUsername, fbPin, fbNickname, fbMode } = this.data;
    if (!fbUsername || !fbPin) {
      wx.showToast({ title: '请输入用户名和密码', icon: 'none' });
      return;
    }

    this.setData({ loading: true });
    try {
      let data;
      if (fbMode === 'register') {
        if (!fbNickname) {
          wx.showToast({ title: '请输入昵称', icon: 'none' });
          this.setData({ loading: false });
          return;
        }
        data = await api.post('/api/auth/register', {
          username: fbUsername,
          nickname: fbNickname,
          pin: fbPin,
        });
      } else {
        data = await api.post('/api/auth/login', {
          username: fbUsername,
          pin: fbPin,
        });
      }

      storage.setToken(data.token);
      storage.setUserInfo(data.user);
      this.setData({ loginFailed: false, loginError: '' });
      wx.showToast({ title: '登录成功', icon: 'success' });
    } catch (err) {
      console.error('[login] 回退登录失败:', err);
    } finally {
      this.setData({ loading: false });
    }
  },

  /** 重试微信登录 */
  async onRetryWxLogin() {
    this.setData({ loading: true });
    try {
      await wxLogin();
      if (storage.isLoggedIn()) {
        this.setData({ loginFailed: false, loginError: '' });
        wx.showToast({ title: '登录成功', icon: 'success' });
      }
    } catch (err) {
      this.setData({ loginError: '微信登录失败: ' + (err.message || err) });
    } finally {
      this.setData({ loading: false });
    }
  },

  // ─── 创建/加入餐桌 ───

  async onCreateRoom() {
    const { roomName } = this.data;
    if (!roomName) return;

    if (!storage.isLoggedIn()) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    this.setData({ loading: true });

    try {
      const res = await api.post('/api/rooms', { name: roomName });

      const userInfo = storage.getUserInfo();
      app.clearCart();
      storage.setActiveRoom({
        id   : res.id,
        name : res.name,
        code : res.code,
        role : (res.chef_id === (userInfo && userInfo.id)) ? 'chef' : 'guest',
      });

      wx.switchTab({ url: '/pages/menu/menu' });
    } catch (err) {
      console.error('[login] 创建餐桌失败:', err);
      const msg = (err && err.message) ? err.message : '创建失败';
      wx.showToast({ title: msg, icon: 'none', duration: 3000 });
    } finally {
      this.setData({ loading: false });
    }
  },

  async onJoinRoom() {
    const { inviteCode } = this.data;
    if (!inviteCode) return;

    if (!storage.isLoggedIn()) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    this.setData({ loading: true });

    try {
      const res = await api.post('/api/rooms/join', { code: inviteCode });

      const userInfo = storage.getUserInfo();
      app.clearCart();
      storage.setActiveRoom({
        id   : res.id,
        name : res.name,
        code : res.code,
        role : (res.chef_id === (userInfo && userInfo.id)) ? 'chef' : 'guest',
      });

      wx.switchTab({ url: '/pages/menu/menu' });
    } catch (err) {
      console.error('[login] 加入餐桌失败:', err);
      const msg = (err && err.message) ? err.message : '加入失败';
      wx.showToast({ title: msg, icon: 'none', duration: 3000 });
    } finally {
      this.setData({ loading: false });
    }
  },

  async _tryAutoJoin() {
    try {
      await this.onJoinRoom();
    } catch (err) {
      console.error('[login] 自动加入失败:', err);
    }
  },
});
