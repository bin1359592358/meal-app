/** 与登录无关的餐桌会话辅助函数。 */
function roomId(room) {
  return room && room.id != null ? String(room.id) : ''
}

function hasRoomChanged(previous, current) {
  return roomId(previous) !== roomId(current)
}

function toActiveRoom(room, userId) {
  if (!room) return null
  return {
    id: room.id,
    name: room.name,
    code: room.code,
    role: String(room.chef_id) === String(userId) ? 'chef' : (room.role || 'guest'),
  }
}

module.exports = { hasRoomChanged, toActiveRoom }
