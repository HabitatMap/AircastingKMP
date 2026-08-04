package pl.llp.aircasting.data.auth

interface AuthTokenStore {
  fun token(): String?
  fun save(token: String)
  fun clear()
}

class InMemoryAuthTokenStore(initial: String? = null) : AuthTokenStore {
  private var token: String? = initial
  override fun token(): String? = token
  override fun save(token: String) { this.token = token }
  override fun clear() { token = null }
}