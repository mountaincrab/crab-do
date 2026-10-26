"""Exercise the actual Room SQL against the exported schema using host SQLite.

Run: python3 -m unittest discover -s tests -p 'test_paging_queries.py'
This verifies query behavior without an Android SDK/emulator, not Room codegen.
"""
import json
import re
import sqlite3
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DAOS = ROOT / 'app/src/commonMain/kotlin/com/mountaincrab/crabdo/data/local/dao'


def query(dao, name):
    source = (DAOS / (dao + '.kt')).read_text()
    for match in re.finditer(r'@Query\((?:"""(.*?)"""|"(.*?)")\)\s*(?:suspend\s+)?fun\s+(\w+)', source, re.S):
        if match[3] == name:
            return match[1] or match[2]
    raise AssertionError(f'Missing query {dao}.{name}')


class PagingQueries(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(':memory:')
        self.db.row_factory = sqlite3.Row
        schema = json.loads((ROOT / 'app/schemas/com.mountaincrab.crabdo.data.local.AppDatabase/9.json').read_text())
        for entity in schema['database']['entities']:
            self.db.execute(entity['createSql'].replace('${TABLE_NAME}', entity['tableName']))

    def tearDown(self):
        self.db.close()

    def seed(self, table, **values):
        row = {}
        for column in self.db.execute(f'PRAGMA table_info({table})'):
            row[column['name']] = None if not column['notnull'] else ('' if column['type'] == 'TEXT' else 0)
        row.update(values)
        fields = ','.join('"' + field + '"' for field in row)
        self.db.execute(f'INSERT INTO {table} ({fields}) VALUES ({",".join("?" for _ in row)})', list(row.values()))

    def rows(self, dao, name, **params):
        return self.db.execute(query(dao, name), params).fetchall()

    def test_board_pages_are_stable_at_equal_orders_and_exclude_other_columns_and_deleted(self):
        for index in reversed(range(123)):
            self.seed('tasks', id=f't{index:03}', boardId='b', columnId='c', order=index // 3)
        self.seed('tasks', id='deleted', boardId='b', columnId='c', isDeleted=1, order=-1)
        self.seed('tasks', id='elsewhere', boardId='other', columnId='other', order=-1)
        first = self.rows('TaskDao', 'observeTaskWindow', columnId='c', limit=51)
        more = self.rows('TaskDao', 'observeTaskWindow', columnId='c', limit=101)
        self.assertEqual([r['id'] for r in first], [f't{i:03}' for i in range(51)])
        self.assertEqual([r['id'] for r in more[:51]], [r['id'] for r in first])
        self.assertEqual(123, self.rows('TaskDao', 'observeColumnCounts', boardId='b')[0]['count'])
        self.assertEqual(40, self.rows('TaskDao', 'maxOrder', columnId='c')[0][0])
        self.assertEqual(0, self.rows('TaskDao', 'minOrder', columnId='c')[0][0])
        self.assertIsNone(self.rows('TaskDao', 'minOrder', columnId='empty')[0][0])
        # A remote/local move across the boundary fills the page without a gap.
        self.db.execute("UPDATE tasks SET columnId='other' WHERE id='t049'")
        refreshed = self.rows('TaskDao', 'observeTaskWindow', columnId='c', limit=51)
        self.assertEqual('t050', refreshed[49]['id'])
        self.assertEqual('t051', refreshed[50]['id'])

    def test_completed_history_is_bounded_sorted_and_user_scoped(self):
        for index in range(125):
            self.seed('one_off_reminders', id=f'r{index:03}', userId='u', isCompleted=1, completedAt=index // 3)
        self.seed('one_off_reminders', id='null-time', userId='u', isCompleted=1)
        self.seed('one_off_reminders', id='deleted', userId='u', isCompleted=1, isDeleted=1, completedAt=999)
        self.seed('one_off_reminders', id='active', userId='u', completedAt=999)
        self.seed('one_off_reminders', id='other', userId='other', isCompleted=1, completedAt=999)
        self.assertEqual([], self.rows('OneOffReminderDao', 'observeCompleted', userId='u', limit=0))
        rows = self.rows('OneOffReminderDao', 'observeCompleted', userId='u', limit=50)
        self.assertEqual([f'r{i:03}' for i in reversed(range(75, 125))], [r['id'] for r in rows])
        self.assertEqual(126, self.rows('OneOffReminderDao', 'observeCompletedCount', userId='u')[0][0])
        all_rows = self.rows('OneOffReminderDao', 'observeCompleted', userId='u', limit=150)
        self.assertEqual('null-time', all_rows[-1]['id'])
        self.db.execute("UPDATE one_off_reminders SET isDeleted=1 WHERE id='r124'")
        self.assertEqual('r123', self.rows('OneOffReminderDao', 'observeCompleted', userId='u', limit=50)[0]['id'])

    def test_deleted_histories_page_and_restore_without_stale_rows(self):
        for table, dao in [('one_off_reminders', 'OneOffReminderDao'), ('recurring_reminders', 'RecurringReminderDao')]:
            for index in range(111):
                self.seed(table, id=f'd{index:03}', userId='u', isDeleted=1, updatedAt=index // 2)
            self.seed(table, id='other', userId='other', isDeleted=1, updatedAt=999)
            self.seed(table, id='live', userId='u', updatedAt=999)
            self.assertEqual(111, self.rows(dao, 'observeDeletedCount', userId='u')[0][0])
            rows = self.rows(dao, 'observeDeleted', userId='u', limit=50)
            self.assertEqual([f'd{i:03}' for i in reversed(range(61, 111))], [r['id'] for r in rows])
            self.db.execute(f"UPDATE {table} SET isDeleted=0 WHERE id='d110'")
            self.assertEqual('d109', self.rows(dao, 'observeDeleted', userId='u', limit=50)[0]['id'])
            self.assertEqual(110, len(self.rows(dao, 'observeDeleted', userId='u', limit=150)))


if __name__ == '__main__':
    unittest.main()
