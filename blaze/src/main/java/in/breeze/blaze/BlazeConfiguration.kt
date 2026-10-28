package `in`.breeze.blaze

import org.json.JSONObject
import java.net.URI
import java.net.URISyntaxException

enum class BlazeEnvironment { BETA, PREPROD, RELEASE, MOCK }

data class BlazeConfiguration(
  val environment: BlazeEnvironment,
  val contentOrigin: String,
  val tenant: String
) {

  internal fun validated(): BlazeConfiguration = copy(
    contentOrigin = validateContentOrigin(contentOrigin),
    tenant = validateTenant(tenant)
  )

  internal fun frameUrl(): String = "$contentOrigin/sdk"

  internal fun toInitiatePayload(requestId: String): JSONObject {
    val payload = JSONObject()
      .put("environment", environment.name.lowercase())
      .put("contentOrigin", contentOrigin)
      .put("publisherId", tenant)
    return JSONObject()
      .put("requestId", requestId)
      .put("service", SHOP_SERVICE)
      .put("payload", payload)
  }

  internal companion object {
    const val SHOP_SERVICE = "in.breeze.shop"
    private val TENANT_PATTERN = Regex("^[a-z0-9][a-z0-9-]{1,31}$")

    fun validateContentOrigin(value: String): String {
      val uri = parseHttpsUrl("contentOrigin", value)
      val path = uri.rawPath ?: ""
      if (path.isNotEmpty() && path != "/") {
        throw BlazeConfigurationException("contentOrigin", "contentOrigin must not have a path: $value")
      }
      return origin(uri)
    }

    fun validateTenant(value: String): String {
      if (!TENANT_PATTERN.matches(value)) {
        throw BlazeConfigurationException(
          "tenant",
          "tenant must match ${TENANT_PATTERN.pattern}: $value"
        )
      }
      return value
    }

    private fun parseHttpsUrl(field: String, value: String): URI {
      val uri = try {
        URI(value)
      } catch (e: URISyntaxException) {
        throw BlazeConfigurationException(field, "$field is not a valid URL: $value")
      }
      if (!"https".equals(uri.scheme, ignoreCase = true)) {
        throw BlazeConfigurationException(field, "$field must use https: $value")
      }
      if (uri.host.isNullOrEmpty()) {
        throw BlazeConfigurationException(field, "$field must have a host: $value")
      }
      if (uri.rawUserInfo != null) {
        throw BlazeConfigurationException(field, "$field must not have userinfo: $value")
      }
      if (uri.rawQuery != null) {
        throw BlazeConfigurationException(field, "$field must not have a query: $value")
      }
      if (uri.rawFragment != null) {
        throw BlazeConfigurationException(field, "$field must not have a fragment: $value")
      }
      if (uri.port == 0 || uri.port > 65535) {
        throw BlazeConfigurationException(field, "$field has an invalid port: $value")
      }
      return uri
    }

    private fun origin(uri: URI): String {
      val port = if (uri.port == -1) "" else ":${uri.port}"
      return "https://${uri.host.lowercase()}$port"
    }
  }
}
