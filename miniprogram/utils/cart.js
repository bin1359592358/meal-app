/** 购物车纯逻辑，不依赖微信运行时。 */
function stableStringify(value) {
  if (Array.isArray(value)) return `[${value.map(stableStringify).sort().join(',')}]`
  if (value && typeof value === 'object') {
    return `{${Object.keys(value).sort().map(key => `${JSON.stringify(key)}:${stableStringify(value[key])}`).join(',')}}`
  }
  return JSON.stringify(value)
}

function createCart(roomId = null) {
  return { roomId: roomId == null ? null : roomId, items: [] }
}

function makeKey(dishId, seasonings = {}) {
  return `${dishId}::${stableStringify(seasonings || {})}`
}

function ensureRoom(state, roomId) {
  if (state.items.length && state.roomId != null && String(state.roomId) !== String(roomId)) {
    throw new Error('购物车不属于当前餐桌')
  }
  state.roomId = roomId == null ? null : roomId
  return state
}

function addItem(state, dish, seasonings = {}, roomId = state.roomId) {
  if (!dish || dish.id == null) throw new Error('无效菜品')
  if (dish.is_available === false) throw new Error('菜品已售罄')
  ensureRoom(state, roomId)
  const normalized = seasonings || {}
  const key = makeKey(dish.id, normalized)
  let item = state.items.find(entry => entry.key === key)
  if (item) item.quantity += 1
  else {
    item = { key, dish, quantity: 1, seasonings: normalized }
    state.items.push(item)
  }
  return item
}

function removeItem(state, key) {
  const index = state.items.findIndex(item => item.key === key)
  if (index >= 0) state.items.splice(index, 1)
}

function setQuantity(state, key, quantity) {
  const item = state.items.find(entry => entry.key === key)
  if (!item) return
  const next = Math.max(0, Math.min(999, Number(quantity) || 0))
  if (next === 0) removeItem(state, key)
  else item.quantity = next
}

function clear(state) {
  state.items = []
  state.roomId = null
}

function getCount(state) {
  return state.items.reduce((sum, item) => sum + item.quantity, 0)
}

function getTotal(state) {
  return state.items.reduce((sum, item) => sum + Number(item.dish.price || 0) * item.quantity, 0)
}

module.exports = { createCart, makeKey, ensureRoom, addItem, removeItem, setQuantity, clear, getCount, getTotal }


