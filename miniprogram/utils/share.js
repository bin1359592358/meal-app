const INVITE_IMAGE = '/images/share/invite-friends.jpg'
const ORDER_IMAGE = '/images/share/order-notify.jpg'

function buildRoomShare(room) {
  const name = room && room.name
  const code = room && room.code

  return {
    title: name ? `「${name}」邀请你一起点菜` : '来「超大一碗饭」一起点菜吧',
    path: code
      ? `/pages/login/login?inviteCode=${encodeURIComponent(code)}`
      : '/pages/login/login',
    imageUrl: INVITE_IMAGE,
  }
}

function buildOrderShare(order) {
  const id = order && (order.id || order.order_id)

  return {
    title: '新订单已提交，请厨师大人快速处理～',
    path: id
      ? `/pages/order-detail/order-detail?id=${encodeURIComponent(id)}&from=chefShare`
      : '/pages/orders/orders',
    imageUrl: ORDER_IMAGE,
  }
}

module.exports = {
  buildRoomShare,
  buildOrderShare,
}
