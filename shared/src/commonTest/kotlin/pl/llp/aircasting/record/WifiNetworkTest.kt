package pl.llp.aircasting.record

import kotlin.test.Test
import kotlin.test.assertEquals

/** What the network-name field offers (legacy FixedSessionDetailsController). */
class WifiNetworksTest {

  @Test
  fun only_2_4_GHz_networks_are_offered() {
    val scan = listOf(
      ScannedNetwork("Home", frequencyMhz = 2437, rssi = -50),
      ScannedNetwork("Home-5G", frequencyMhz = 5180, rssi = -40),
      ScannedNetwork("Office-6G", frequencyMhz = 5955, rssi = -45),
    )

    assertEquals(listOf("Home"), scan.twoPointFourGhzNames())
  }

  @Test
  fun hidden_networks_are_not_offered() {
    val scan = listOf(ScannedNetwork("", 2412, -50), ScannedNetwork(" ", 2412, -50))

    assertEquals(emptyList(), scan.twoPointFourGhzNames())
  }

  /** A mesh or several access points show the same name once per radio. */
  @Test
  fun each_name_is_offered_once_strongest_first() {
    val scan = listOf(
      ScannedNetwork("Cafe", 2412, rssi = -80),
      ScannedNetwork("Home", 2437, rssi = -70),
      ScannedNetwork("Cafe", 2462, rssi = -40),
    )

    assertEquals(listOf("Cafe", "Home"), scan.twoPointFourGhzNames())
  }

  /** One scan can miss a network that's there; once seen, it stays pickable while Details is open. */
  @Test
  fun a_network_missing_from_a_later_scan_stays_listed() {
    val listed = listOf("Home", "Cafe").withNewlySeen(listOf("Home"))

    assertEquals(listOf("Home", "Cafe"), listed)
  }

  /** Rows never move under the user's finger: newcomers join at the end. */
  @Test
  fun new_networks_join_at_the_end_without_reordering() {
    val listed = listOf("Home", "Cafe").withNewlySeen(listOf("Office", "Cafe", "Home"))

    assertEquals(listOf("Home", "Cafe", "Office"), listed)
  }
}