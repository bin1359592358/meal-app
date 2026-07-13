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
  const roomId = order && (order.room_id || order.roomId)
  const detailPath = id
    ? `/pages/order-detail/order-detail?id=${encodeURIComponent(id)}&from=chefShare`
    : '/pages/orders/orders'

  return {
    title: '饭宝来送单啦～厨师大人，快来看看吧！',
    path: id && roomId ? `${detailPath}&roomId=${encodeURIComponent(roomId)}` : detailPath,
    imageUrl: ORDER_IMAGE,
  }
}

module.exports = {
  buildRoomShare,
  buildOrderShare,
}
