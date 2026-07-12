const test = require('node:test')
const assert = require('node:assert/strict')
const seasoning = require('../utils/seasoning')

test('按定义生成四类调味默认值', () => {
  const defs = [
    { name: '辣度', type: 'single', options: ['不辣', '微辣'], default: '微辣' },
    { name: '配料', type: 'multi', default: ['葱'] },
    { name: '甜度', type: 'scale', min: 0, default: 0 },
    { name: '备注', type: 'text' },
  ]
  assert.deepEqual(seasoning.getDefaults(defs), { 辣度: '微辣', 配料: ['葱'], 甜度: 0, 备注: '' })
})

test('摘要正确处理数组、数字和空值', () => {
  assert.equal(seasoning.toSummary({ 辣度: '微辣', 配料: ['葱', '蒜'], 甜度: 0, 备注: '' }), '辣度：微辣；配料：葱、蒜；甜度：0')
  assert.equal(seasoning.toSummary({}), '')
})
