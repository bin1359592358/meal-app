const test = require('node:test')
const assert = require('node:assert/strict')
const session = require('../utils/session')

test('正确判断餐桌是否变化', () => {
  assert.equal(session.hasRoomChanged(null, { id: 1 }), true)
  assert.equal(session.hasRoomChanged({ id: 1 }, { id: '1' }), false)
  assert.equal(session.hasRoomChanged({ id: 1 }, { id: 2 }), true)
  assert.equal(session.hasRoomChanged(null, null), false)
})

test('从剩余餐桌选择新活跃餐桌', () => {
  const room = session.toActiveRoom({ id: 3, name: '晚餐', code: 'ABC', chef_id: 9 }, 9)
  assert.deepEqual(room, { id: 3, name: '晚餐', code: 'ABC', role: 'chef' })
})
