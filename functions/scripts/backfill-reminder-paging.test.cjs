const { test } = require('node:test')
const assert = require('node:assert/strict')
const { missingFields } = require('./backfill-reminder-paging.cjs')

test('only missing fields are normalized, preserving completion and deletion', () => {
  const data = { isDeleted: true, isCompleted: true, completedAt: 123, updatedAt: new Date(456) }
  assert.deepEqual(missingFields(data, true), {})
  assert.deepEqual(missingFields({ isDeleted: true, isCompleted: true, createdAt: 42 }, true), {
    completedAt: null, updatedAt: new Date(42),
  })
})
test('legacy one-offs default to active; recurring docs do not gain completion fields', () => {
  assert.deepEqual(missingFields({}, true), {
    isDeleted: false, isCompleted: false, completedAt: null, updatedAt: new Date(0),
  })
  assert.deepEqual(missingFields({}, false), { isDeleted: false, updatedAt: new Date(0) })
})
