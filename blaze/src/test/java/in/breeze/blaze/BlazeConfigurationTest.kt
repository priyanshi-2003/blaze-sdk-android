package `in`.breeze.blaze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BlazeConfigurationTest {

  private val valid = BlazeConfiguration(
    environment = BlazeEnvironment.BETA,
    contentOrigin = "https://shop.beta.breeze.in",
    tenant = "acme"
  )

  @Test
  fun contentOriginAcceptsAndNormalises() {
    val cases = mapOf(
      "https://shop.breeze.in" to "https://shop.breeze.in",
      "https://shop.breeze.in/" to "https://shop.breeze.in",
      "https://Shop.Breeze.in:8443" to "https://shop.breeze.in:8443",
      "HTTPS://shop.breeze.in" to "https://shop.breeze.in"
    )
    cases.forEach { (input, expected) ->
      assertEquals(input, expected, valid.copy(contentOrigin = input).validated().contentOrigin)
    }
  }

  @Test
  fun contentOriginRejects() {
    listOf(
      "",
      "shop.breeze.in",
      "/sdk",
      "http://shop.breeze.in",
      "http://localhost:5173",
      "ftp://shop.breeze.in",
      "https://",
      "https:///sdk",
      "https:shop.breeze.in",
      "https://shop.breeze.in/sdk",
      "https://shop.breeze.in/sdk/",
      "https://user@shop.breeze.in",
      "https://user:pass@shop.breeze.in",
      "https://shop.breeze.in?x=1",
      "https://shop.breeze.in?",
      "https://shop.breeze.in#top",
      "https://shop.breeze.in:0",
      "https://shop.breeze.in:70000",
      "https://shop breeze.in",
      " https://shop.breeze.in"
    ).forEach { assertInvalid("contentOrigin", valid.copy(contentOrigin = it)) }
  }

  @Test
  fun contentOriginPathErrorNamesFieldAndValue() {
    val e = expectInvalid(valid.copy(contentOrigin = "https://x/sdk"))
    assertEquals("contentOrigin", e.field)
    assertEquals("E_CONFIG_INVALID", e.code)
    assertEquals("E_CONFIG_INVALID: contentOrigin must not have a path: https://x/sdk", e.message)
  }

  @Test
  fun tenantAccepts() {
    listOf("ab", "acme", "a1", "0a", "acme-retail", "a-", "a" + "b".repeat(31))
      .forEach { assertEquals(it, valid.copy(tenant = it).validated().tenant) }
  }

  @Test
  fun tenantRejects() {
    listOf(
      "",
      "a",
      "-acme",
      "Acme",
      "ACME",
      "acme_retail",
      "acme.retail",
      " acme",
      "acme ",
      "a" + "b".repeat(32)
    ).forEach { assertInvalid("tenant", valid.copy(tenant = it)) }
  }

  @Test
  fun everyEnvironmentIsAccepted() {
    BlazeEnvironment.values().forEach {
      assertEquals(it, valid.copy(environment = it).validated().environment)
    }
  }

  @Test
  fun sameInputValidatesToEqualConfiguration() {
    val withSlash = valid.copy(contentOrigin = "https://shop.beta.breeze.in/")
    assertEquals(valid.validated(), withSlash.validated())
  }

  @Test
  fun frameUrlIsContentOriginSdk() {
    assertEquals("https://shop.beta.breeze.in/sdk", valid.validated().frameUrl())
  }

  @Test
  fun initiatePayloadMapsEveryEnvironment() {
    mapOf(
      BlazeEnvironment.BETA to "beta",
      BlazeEnvironment.PREPROD to "preprod",
      BlazeEnvironment.RELEASE to "release",
      BlazeEnvironment.MOCK to "mock"
    ).forEach { (environment, expected) ->
      val json = valid.copy(environment = environment).validated().toInitiatePayload("req-1")
      assertEquals("req-1", json.getString("requestId"))
      assertEquals("in.breeze.shop", json.getString("service"))
      val payload = json.getJSONObject("payload")
      assertEquals(expected, payload.getString("environment"))
      assertEquals("https://shop.beta.breeze.in", payload.getString("contentOrigin"))
      assertEquals("acme", payload.getString("publisherId"))
      assertEquals(3, payload.length())
    }
  }

  private fun assertInvalid(field: String, configuration: BlazeConfiguration) {
    val e = expectInvalid(configuration)
    assertEquals(configuration.toString(), field, e.field)
    assertEquals("E_CONFIG_INVALID", e.code)
    assertTrue(e.message!!, e.message!!.startsWith("E_CONFIG_INVALID: $field "))
  }

  private fun expectInvalid(configuration: BlazeConfiguration): BlazeConfigurationException {
    try {
      configuration.validated()
    } catch (e: BlazeConfigurationException) {
      return e
    }
    fail("expected E_CONFIG_INVALID for $configuration")
    throw AssertionError()
  }
}
