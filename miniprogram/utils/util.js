/**
 * 工具函数
 */

/**
 * 格式化日期时间
 * @param {string} isoStr - ISO 8601 时间字符串
 * @returns {string} 格式化后的时间字符串
 */
function formatTime(isoStr) {
  if (!isoStr) return ''
  const d = new Date(isoStr)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hour = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${min}`
}

/**
 * 格式化相对时间（几分钟前、几小时前等）
 * @param {string} isoStr - ISO 8601 时间字符串
 * @returns {string}
 */
function formatRelativeTime(isoStr) {
  if (!isoStr) return ''
  const d = new Date(isoStr)
  const now = new Date()
  const diff = (now - d) / 1000 // 秒

  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  if (diff < 604800) return `${Math.floor(diff / 86400)}天前`
  return formatTime(isoStr)
}

/**
 * 格式化价格
 * @param {number} price
 * @returns {string}
 */
function formatPrice(price) {
  return `¥${Number(price).toFixed(1)}`
}

/**
 * 订单状态文本映射
 */
const STATUS_MAP = {
  pending: '待处理',
  preparing: '制作中',
  served: '已出餐',
  completed: '已完成',
  cancelled: '已取消',
}

const STATUS_COLOR = {
  pending: '#FF9800',
  preparing: '#2196F3',
  served: '#4CAF50',
  completed: '#9E9E9E',
  cancelled: '#E53935',
}

function getStatusText(status) {
  return STATUS_MAP[status] || status
}

function getStatusColor(status) {
  return STATUS_COLOR[status] || '#9E9E9E'
}

/**
 * 防抖
 */
function debounce(fn, delay = 300) {
  let timer = null
  return function (...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => fn.apply(this, args), delay)
  }
}

module.exports = {
  formatTime,
  formatRelativeTime,
  formatPrice,
  getStatusText,
  getStatusColor,
  debounce,
}
