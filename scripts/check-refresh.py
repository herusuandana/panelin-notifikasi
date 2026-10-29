#!/usr/bin/env python3
"""Source contracts for refresh; not a gesture/runtime acceptance test."""
from pathlib import Path

ui = (Path(__file__).resolve().parents[1] / 'app/src/main/java/id/panelin/notifikasi/MainActivity.kt').read_text()
# An instant local read can coalesce true -> false before Material3 sees true.
# Launch the work only after the refreshing composition has been applied.
assert 'LaunchedEffect(refreshing)' in ui, 'Refresh work starts before refreshing UI is committed'
effect = ui.split('LaunchedEffect(refreshing)', 1)[1].split('val config', 1)[0]
assert 'if(refreshing) refresh(tab)' in effect
refresh = ui.split('private suspend fun refresh(page: Int)', 1)[1].split('private fun mutate', 1)[0]
assert 'reload()' in refresh and 'if(page==2) loadApps()' in refresh
assert 'finally { refreshing = false }' in refresh, 'Refresh must clear on failure/cancellation'
assert 'if(initialLoading || refreshing)' in ui, 'Manual refresh has no skeleton'
assert 'if(!initialLoading && !refreshing)' in ui, 'Stale content shown instead of loading skeleton'
assert 'rememberInfiniteTransition(label="skeleton")' in ui, 'Skeleton has no loading animation'
assert 'testTag("refresh-indicator-$page")' in ui
assert 'this[RefreshDistance]=pullState.distanceFraction' in ui
print('Refresh source contracts OK (runtime gesture verification still required).')
