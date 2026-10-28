package `in`.breeze.blaze

class BlazeConfigurationException(
  val field: String,
  message: String
) : IllegalArgumentException("$E_CONFIG_INVALID: $message") {

  val code: String = E_CONFIG_INVALID

  companion object {
    const val E_CONFIG_INVALID = "E_CONFIG_INVALID"
  }
}
