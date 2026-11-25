package pl.llp.aircasting.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import pl.llp.aircasting.data.network.FixedSessionsSync
import pl.llp.aircasting.mydata.PastSession
import pl.llp.aircasting.mydata.PastSessionsRepository

/**
 * Past Sessions from the DB plus one walk of every fixed-sessions page per subscription: sessions finished
 * on the server arrive through the same DB flow, page by page. Mobile rows are local; nothing to sync.
 */
class SyncingPastSessionsRepository(
  private val local: PastSessionsRepository,
  private val sync: FixedSessionsSync,
) : PastSessionsRepository {
  override fun observe(): Flow<List<PastSession>> = merge(local.observe(), flow<Nothing> { sync.syncFinished() })
}
