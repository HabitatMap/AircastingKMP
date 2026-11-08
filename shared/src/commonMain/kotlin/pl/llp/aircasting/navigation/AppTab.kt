package pl.llp.aircasting.navigation

import pl.llp.aircasting.i18n.Strings

enum class AppTab { Home, Explore, Record, Active, MyData }

fun Strings.label(tab: AppTab): String = when (tab) {
  AppTab.Home -> tabHome
  AppTab.Explore -> tabExplore
  AppTab.Record -> tabRecord
  AppTab.Active -> tabActive
  AppTab.MyData -> tabMyData
}