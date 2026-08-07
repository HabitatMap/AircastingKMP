package pl.llp.aircasting.i18n

/**
 * French copy. Mirrors [EnStrings] field for field — [Strings] being a data class means the
 * compiler refuses this file until every single value is supplied, so a locale can never be
 * half-translated.
 */
val FrStrings = Strings(
  customServerTitle = "Utiliser un serveur de données personnalisé",
  customServerIntroBody = "Si vous hébergez votre propre serveur AirCasting (auto-hébergé ou " +
    "d'organisation), indiquez-le ici. Vos mesures existantes restent sur le serveur actuel — " +
    "seules les nouvelles données seront synchronisées vers le nouveau.",
  customServerStep1 = "Saisissez l'URL et le port de votre serveur",
  customServerStep2 = "Vous serez déconnecté pour appliquer le changement",
  customServerStep3 = "Reconnectez-vous. Les nouvelles mesures se synchronisent automatiquement",
  customServerFormBody = "Saisissez l'adresse de votre serveur personnalisé ou auto-hébergé. " +
    "Nous testerons la connexion avant d'enregistrer quoi que ce soit.",
  customServerUrlLabel = "URL",
  customServerUrlHint = "Saisissez une URL valide, p. ex. https://aircasting.org",
  customServerPortLabel = "Port",
  customServerPortHint = "Le port doit être un nombre entre 1 et 65535",
  customServerUseOfficial = "Utiliser le serveur officiel AirCasting",
  customServerNext = "Suivant",
  customServerTestingTitle = "Test de la connexion...",
  customServerTestingBody = "Nous vérifions que votre serveur répond correctement. Cela peut prendre un moment.",
  customServerTooLong = "Cela prend trop de temps ? Annuler !",
  customServerVerifiedTitle = "Connexion vérifiée",
  customServerVerifiedBody = { server ->
    "$server a répondu correctement. Vos mesures y seront synchronisées désormais. " +
      "Les données existantes resteront sur le serveur actuel."
  },
  customServerFailedTitle = "Échec de la connexion",
  customServerFailedBody = { server -> "Impossible de joindre $server. Vérifiez l'URL et le port, puis réessayez." },
  customServerSave = "Enregistrer et se déconnecter",
  customServerEdit = "Revenir et modifier",
  customServerClearField = "Effacer",
  settingsVersion = { version -> "AirCasting v$version" },
  settingsTagline = "HabitatMap · Qualité de l'air en open source",
  settingsTitle = "Réglages",
  settingsAccountTitle = "Réglages du compte",
  settingsAccountSubtitle = "Modifier le mot de passe, supprimer le compte",
  settingsAirBeamsTitle = "Gestion des AirBeam",
  settingsAirBeamsSubtitle = "Nom, batterie, stockage, connexion",
  settingsAppTitle = "Réglages de l'application",
  settingsAppSubtitle = "Langue, notifications, confidentialité",
  settingsHelpTitle = "Aide",
  settingsHelpSubtitle = "FAQ, ressources, version de l'application",

  settingsAccountSectionHeader = "COMPTE",
  accountChangeEmail = "Modifier l'adresse e-mail",
  accountChangeUsername = "Modifier le nom d'utilisateur",
  accountResetPassword = "Réinitialiser le mot de passe",
  accountSignOut = "Se déconnecter",
  accountDeleteAccount = "Supprimer le compte",
  cancel = "Annuler",
  deleteAccountConfirmTitle = "Supprimer le compte ?",
  deleteAccountConfirmBody =
    "Cette action supprime définitivement votre compte et toutes les sessions que vous avez " +
      "enregistrées. Nous vous enverrons un code par e-mail pour confirmer.",
  deleteAccountSendCode = "Envoyez-moi un code",
  deleteAccountCodeTitle = "Confirmer la suppression",
  deleteAccountCodeBody = { email ->
    "Saisissez le code à 4 chiffres envoyé à $email. Il expire dans 30 minutes."
  },
  deleteAccountCodeLabel = "Code à 4 chiffres",
  deleteAccountCodeInvalid = "Ce code est incorrect ou a expiré.",
  deleteAccountResend = "Envoyer un nouveau code",
  appSettingsCommunityHeader = "COMMUNAUTÉ",
  appSettingsUnitsRegionHeader = "UNITÉS ET RÉGION",
  appSettingsDisplayHeader = "AFFICHAGE",
  appSettingsSensorsHeader = "CAPTEURS",
  appSettingsNotificationsHeader = "NOTIFICATIONS",
  appSettingsSyncHeader = "SYNCHRONISATION",
  appSettingsBackendHeader = "SERVEUR",
  appSettingCrowdMap = "Contribuer à la carte collaborative",
  appSettingCrowdMapSubtitle = "Partager les mesures avec la communauté",
  appSettingDisableMapping = "Désactiver la cartographie",
  appSettingDisableMappingSubtitle = "Ne plus enregistrer les données de localisation",
  appSettingTemperatureUnits = "Unités de température",
  appSettingRegionalFormats = "Formats régionaux",
  appSettingRegionalFormatsSubtitle = "Date, heure, distance et unités",
  appSettingLanguage = "Langue",
  appSettingMapType = "Type de carte",
  appSettingDarkMode = "Mode sombre",
  appSettingMicrophoneCalibration = "Calibrage du microphone",
  appSettingMicrophoneCalibrationSubtitle = "Calibrer le micro intégré",
  appSettingPushNotifications = "Notifications push",
  appSettingPushNotificationsSubtitle = "Alertes de qualité de l'air et rappels",
  appSettingWifiOnlySync = "Synchroniser uniquement en WiFi",
  appSettingWifiOnlySyncSubtitle = "Évite d'utiliser les données mobiles pendant la synchronisation",
  appSettingCustomDataServer = "Serveur de données personnalisé",
  appSettingCustomDataServerSubtitle = "Choisir où les données sont stockées",

  appSettingValueFahrenheit = "°F",
  appSettingValueCelsius = "°C",
  appSettingValueRegionUs = "US",
  done = "Terminé",
  appSettingValueRegionUk = "Format britannique",
  appSettingValueRegionCentralEuropean = "Europe centrale",
  appSettingValueRegionNordic = "Nordique",
  appSettingValueRegionEastAsian = "Asie de l'Est",
  appSettingFahrenheit = "Fahrenheit",
  appSettingFahrenheitDetail = "°F • ex. 72 °F",
  appSettingCelsius = "Celsius",
  appSettingCelsiusDetail = "°C • ex. 22 °C",
  unitMiles = "Milles",
  unitKilometers = "Kilomètres",
  appSettingValueMapDefault = "Par défaut",
  appSettingValueMapSatellite = "Satellite",
  languageName = "Français",

  micLiveReading = "LECTURE EN DIRECT",
  micDecibels = "db",
  micCalibrationHint =
    "Plage typique : 80–100. Ajustez la valeur ci-dessous jusqu'à ce que la lecture en direct " +
      "y corresponde.",
  micCalibrationOffset = "DÉCALAGE DE CALIBRAGE",
  micResetOffset = { offset -> "Rétablir le décalage par défaut ($offset)" },
  micDecreaseOffset = "Diminuer le décalage",
  micIncreaseOffset = "Augmenter le décalage",
  micPermissionNeeded = "Autorisez l'accès au microphone pour voir une lecture en direct.",

  openSettings = "Ouvrir les réglages",
  back = "Retour",
  appLogo = "AirCasting",
  errorTitle = "Une erreur s'est produite.",
  errorRetry = "Réessayer",

  airQualityLabel = "QUALITÉ DE L'AIR",
  noReadingsTitle = "Aucune donnée de qualité de l'air disponible",
  noReadingsBody = "Nous n'avons pas trouvé de mesures récentes pour ce lieu.\nRéessayez plus tard.",
  noLocationTitle = "Aucune station à proximité",
  noLocationBody =
    "Nous n'avons pas pu associer votre position à une station de mesure.\n" +
      "Essayez d'activer la localisation précise.",
  turnOnLocation = "Activer les services de localisation",

  aqGoodLabel = "Bonne",
  aqGoodDescription = "L'air extérieur est sain. Le moment idéal pour des activités en plein air.",
  aqModerateLabel = "Modérée",
  aqModerateDescription = "La qualité de l'air est acceptable pour la plupart des personnes.",
  aqUnhealthySensitiveLabel = "Mauvaise pour les groupes sensibles",
  aqUnhealthySensitiveDescription =
    "Les groupes sensibles devraient limiter les efforts en plein air.",
  aqUnhealthyLabel = "Mauvaise",
  aqUnhealthyDescription =
    "Tout le monde peut commencer à en ressentir les effets. Limitez le temps passé dehors.",
  aqHazardousLabel = "Dangereuse",
  aqHazardousDescription = "Alerte sanitaire. Évitez toute activité en plein air.",

  pollutantPm25 = "PM 2,5",
  pollutantNo2 = "NO₂",
  pollutantOzone = "Ozone",

  nearbyStationsTitle = "Stations à proximité",
  viewMap = "Voir la carte",
  govMonitor = "STATION OFFICIELLE",

  distanceAway = { miles -> "à $miles mi" },
  ageJustNow = "à l'instant",
  ageMinutesAgo = { minutes -> "il y a $minutes min" },
  ageHoursAgo = { hours -> "il y a $hours h" },
  ageDaysAgo = { days -> "il y a $days j" },
  tabHome = "Accueil",
  tabExplore = "Explorer",
  tabRecord = "Enregistrer",
  tabFavorites = "Favoris",
  tabMyData = "Mes données",
)
