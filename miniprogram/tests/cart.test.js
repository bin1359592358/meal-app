const test = require('node:test')
const assert = require('node:assert/strict')
const cart = require('../utils/cart')

test('同菜同调味合并，不同调味独立', () => {
  const state = cart.createCart(1)
  const dish = { id: 7, name: '米饭', price: 3 }
  cart.addItem(state, dish, { 辣度: '微辣', 配料: ['葱', '蒜'] })
  cart.addItem(state, dish, { 配料: ['蒜', '葱'], 辣度: '微辣' })
  cart.addItem(state, dish, { 辣度: '中辣' })
  assert.equal(state.items.length, 2)
  assert.equal(state.items[0].quantity, 2)
  assert.notEqual(state.items[0].key, state.items[1].key)
})

test('购物车按 key 更新并隔离餐桌', () => {
  const state = cart.createCart(1)
  const item = cart.addItem(state, { id: 2, price: 8 }, {})
  cart.setQuantity(state, item.key, 3)
  assert.equal(cart.getCount(state), 3)
  assert.throws(() => cart.ensureRoom(state, 2), /不属于当前餐桌/)
  cart.clear(state)
  assert.deepEqual(state, { roomId: null, items: [] })
})

