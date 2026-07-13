const test = require('node:test')
const assert = require('node:assert/strict')

const { buildRoomShare, buildOrderShare } = require('../utils/share')

test('餐桌分享包含邀请码、标题和邀请插画', () => {
  assert.deepEqual(buildRoomShare({ name: '周末聚餐', code: 'A B/C' }), {
    title: '「周末聚餐」邀请你一起点菜',
    path: '/pages/login/login?inviteCode=A%20B%2FC',
    imageUrl: '/images/share/invite-friends.jpg',
  })
})

test('无餐桌时回退为普通小程序分享', () => {
  assert.deepEqual(buildRoomShare(null), {
    title: '来「超大一碗饭」一起点菜吧',
    path: '/pages/login/login',
    imageUrl: '/images/share/invite-friends.jpg',
  })
})

test('订单通知直达订单详情', () => {
  assert.deepEqual(buildOrderShare({ id: 21 }), {
    title: '新订单已提交，请厨师大人快速处理～',
    path: '/pages/order-detail/order-detail?id=21&from=chefShare',
    imageUrl: '/images/share/order-notify.jpg',
  })
})

test('订单 ID 缺失时回退到订单列表', () => {
  assert.equal(buildOrderShare({}).path, '/pages/orders/orders')
})
