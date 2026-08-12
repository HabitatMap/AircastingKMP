package pl.llp.aircasting.navigation

enum class AppEntry { Onboarding, Auth, Shell }

fun appEntry(onboarded: Boolean, signedIn: Boolean): AppEntry = when {
  signedIn -> AppEntry.Shell
  onboarded -> AppEntry.Auth
  else -> AppEntry.Onboarding
}