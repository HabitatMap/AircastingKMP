package pl.llp.aircasting.active

import kotlinx.coroutines.flow.Flow
import pl.llp.aircasting.domain.session.Session

data class ActiveMobileSession(val session: Session, val readings: List<LatestReading>)

interface ActiveSessionsRepository {
  /** Mobile sessions not finished yet, newest first, each with every stream's newest reading. */
  fun observeMobile(): Flow<List<ActiveMobileSession>>
}
