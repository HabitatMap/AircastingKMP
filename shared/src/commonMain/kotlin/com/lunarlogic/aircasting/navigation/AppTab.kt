package com.lunarlogic.aircasting.navigation

import com.lunarlogic.aircasting.i18n.Strings

enum class AppTab { Home, Explore, Record, Favorites, MyData }

fun Strings.label(tab: AppTab): String = when (tab) {
  AppTab.Home -> tabHome
  AppTab.Explore -> tabExplore
  AppTab.Record -> tabRecord
  AppTab.Favorites -> tabFavorites
  AppTab.MyData -> tabMyData
}