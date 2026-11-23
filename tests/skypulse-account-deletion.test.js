import test from 'node:test';
import assert from 'node:assert/strict';
import { dataGroups, deleteSkyPulseAccount } from '../docs/account-deletion-core.js';

function setup(options = {}) {
  const account = {uid: 'first', isAnonymous: false};
  const another = {uid: 'second', isAnonymous: false};
  const docs = new Map(dataGroups.map(group => [group, ['record-a', 'record-b']]));
  const history = [];
  let current = account;
  let marker = false;
  let stopped = false;
  const api = {
    isCurrentAccount(user) { return user === current; },
    async reauthenticate(user) {
      assert.equal(user, account); history.push('reauth');
      if (options.invalidLogin) throw new Error('auth/invalid-credential');
      if (options.otherLogin) throw new Error('auth/user-mismatch');
      if (options.changeWhileAuthenticating) current = another;
    },
    async beginDeletion(uid) {
      assert.equal(uid, 'first'); history.push('barrier'); marker = true;
    },
    async readServerPage(uid, group) {
      assert.equal(uid, 'first'); assert.ok(marker); history.push('read:' + group);
      if (options.offline) throw new Error('unavailable');
      if (options.changeDuringRead) current = another;
      if (options.neverEmpty) return ['record-a'];
      return docs.get(group).slice(0, 1);
    },
    async removePage(uid, group, page) {
      assert.equal(uid, 'first'); history.push('remove:' + group);
      if (options.interrupted && group === 'saved_airports' && !stopped) {
        stopped = true; throw new Error('connection lost');
      }
      docs.set(group, docs.get(group).filter(id => !page.includes(id)));
    },
    async removeAccount(user) {
      assert.equal(user, account);
      assert.ok([...docs.values()].every(group => group.length === 0));
      history.push('delete-user'); current = null;
    },
  };
  return { account, another, docs, api, history };
}

test('deletes all server-confirmed groups before the account', async () => {
  const {account, api, history} = setup();
  await deleteSkyPulseAccount(api, account, 'DELETE');
  assert.deepEqual(history.slice(0,2), ['reauth','barrier']);
  assert.equal(history.at(-1), 'delete-user');
  assert.equal(history.filter(x => x.startsWith('remove:')).length, dataGroups.length * 2);
});
test('a missing typed confirmation leaves all data untouched', async () => {
  const {account, api, history} = setup();
  await assert.rejects(deleteSkyPulseAccount(api, account, 'delete'), /not-confirmed/);
  assert.deepEqual(history, []);
});
test('signed-out and guest sessions do not start deletion', async () => {
  const {api, history} = setup();
  await assert.rejects(deleteSkyPulseAccount(api, null, 'DELETE'), /not-confirmed/);
  await assert.rejects(deleteSkyPulseAccount(api, {uid:'guest', isAnonymous:true}, 'DELETE'), /not-confirmed/);
  assert.deepEqual(history, []);
});
test('failed password and different Google account leave data untouched', async () => {
  for (const config of [{invalidLogin:true}, {otherLogin:true}]) {
    const {account, api, history} = setup(config);
    await assert.rejects(deleteSkyPulseAccount(api, account, 'DELETE'), /auth\//);
    assert.deepEqual(history, ['reauth']);
  }
});
test('if the active account changes during verification, stop before the lock', async () => {
  const {account, api, history} = setup({changeWhileAuthenticating:true});
  await assert.rejects(deleteSkyPulseAccount(api, account, 'DELETE'), /account-changed/);
  assert.deepEqual(history, ['reauth']);
});
test('if the active account changes while reading, stop before deleting any rows', async () => {
  const {account, api, history} = setup({changeDuringRead:true});
  await assert.rejects(deleteSkyPulseAccount(api, account, 'DELETE'), /account-changed/);
  assert.ok(!history.some(x => x.startsWith('remove:') || x === 'delete-user'));
});
test('offline does not count an empty cache as complete', async () => {
  const {account, api, history} = setup({offline:true});
  await assert.rejects(deleteSkyPulseAccount(api, account, 'DELETE'), /unavailable/);
  assert.ok(!history.includes('delete-user'));
});
test('a partial deletion can be safely retried', async () => {
  const {account, api, docs, history} = setup({interrupted:true});
  await assert.rejects(deleteSkyPulseAccount(api, account, 'DELETE'), /connection lost/);
  assert.deepEqual(docs.get('saved_aircraft'), []);
  assert.equal(docs.get('saved_airports').length, 2);
  assert.ok(!history.includes('delete-user'));
  await deleteSkyPulseAccount(api, account, 'DELETE');
  assert.equal(history.at(-1), 'delete-user');
});
test('per-run work is bounded and cannot prematurely remove Auth', async () => {
  const {account, api, history} = setup({neverEmpty:true});
  await assert.rejects(deleteSkyPulseAccount(api, account, 'DELETE'), /deletion\/continue/);
  assert.equal(history.filter(x => x.startsWith('remove:')).length, 48);
  assert.ok(!history.includes('delete-user'));
});
