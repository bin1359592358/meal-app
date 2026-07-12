/** 调味默认值与展示格式。 */
function getDefaults(definitions = []) {
  const values = {}
  definitions.forEach(item => {
    if (!item || !item.name) return
    if (item.type === 'single') values[item.name] = item.default !== undefined ? item.default : ((item.options || [])[0] || '')
    else if (item.type === 'multi') values[item.name] = Array.isArray(item.default) ? [...item.default] : []
    else if (item.type === 'scale') values[item.name] = item.default !== undefined ? item.default : (item.min !== undefined ? item.min : 0)
    else values[item.name] = item.default || ''
  })
  return values
}

function toSummary(values) {
  if (!values || typeof values !== 'object') return ''
  return Object.keys(values).map(name => {
    const value = values[name]
    if (Array.isArray(value)) return value.length ? `${name}：${value.join('、')}` : ''
    if (value === '' || value == null || value === 'default') return ''
    return `${name}：${value}`
  }).filter(Boolean).join('；')
}

module.exports = { getDefaults, toSummary }
