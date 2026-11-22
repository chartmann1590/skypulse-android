// Browser-independent orchestration. Firebase rules remain the security boundary.
export const accountCollections = Object.freeze(['saved_aircraft', 'saved_airports', 'saved_areas']);
export const dataGroups = Object.freeze([...accountCollections, 'shared_flight_refs', 'legacy_shared_flights']);

export async function deleteSkyPulseAccount(api, account, confirmation) {
  if (!account?.uid || account.isAnonymous || confirmation !== 'DELETE') {
    throw new Error('deletion/not-confirmed');
  }
  function check() {
    if (!api.isCurrentAccount(account)) throw new Error('deletion/account-changed');
  }
  check();
  await api.reauthenticate(account);
  check();
  await api.beginDeletion(account.uid);
  check();
  // A server-confirmed empty page for every group is required before removing Auth.
  // The rules block uploads from other signed-in sessions after beginDeletion.
  // Bounded per click; the account stays intact and the same flow can resume.
  for (const group of dataGroups) {
    let pages = 0;
    for (;;) {
      check();
      const page = await api.readServerPage(account.uid, group);
      check();
      if (page.length === 0) break;
      if (pages++ === 48) throw new Error('deletion/continue');
      await api.removePage(account.uid, group, page);
    }
  }
  check();
  await api.removeAccount(account);
}
