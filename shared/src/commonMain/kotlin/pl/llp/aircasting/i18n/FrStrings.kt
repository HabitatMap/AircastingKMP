package pl.llp.aircasting.i18n

/**
 * French copy. Mirrors [EnStrings] member for member — every [Strings] member being abstract means
 * the compiler refuses this file until every single value is supplied, so a locale can never be
 * half-translated.
 */
object FrStrings : Strings {
  override val newSessionConfirmTitle = "Prêt à commencer ?"
  override val summaryTypeLabel = "TYPE DE SESSION"
  override val summaryNameLabel = "NOM DE LA SESSION"
  override val summaryTagsLabel = "ÉTIQUETTES"
  override val summaryIntervalLabel = "INTERVALLE DE MESURE"
  override val startRecording = "Démarrer l'enregistrement"
  override val preparingLocating = "Recherche de votre position…"
  override val preparingConfiguring = "Configuration de l'AirBeam…"
  override val newSessionDetailsTitle = "Nommez votre session"
  override val newSessionDetailsBody =
    "Ajoutez des détails pour la retrouver et la partager plus tard."
  override val newSessionDeviceTitle = "Choisissez l'appareil avec lequel vous souhaitez mesurer."
  override val newSessionDeviceBody =
    "Choisissez la méthode adaptée à votre matériel et à votre objectif."
  override val newSessionDeviceAirBeamHeader = "AIRBEAM"
  override val sessionNameLabel = "Nom de la session"
  override val sessionNamePlaceholder = "ex. « Bruit de l'école »"
  override val sessionTagsLabel = "Étiquettes"
  override val sessionTagsPlaceholder = "Ajoutez des étiquettes, ex. « école », « trafic »"
  override val latestTagsHeader = "ÉTIQUETTES RÉCENTES"
  override fun addTagLabel(tag: String): String = "Ajouter l'étiquette $tag"
  override val samplingIntervalHeader = "INTERVALLE DE MESURE"
  override val samplingIntervalHint =
    "Un intervalle plus long permet d'économiser la batterie de votre appareil."

  override fun samplingSeconds(count: Int): String = "$count s"
  override fun samplingMinutes(count: Int): String = "$count min"
  override val methodMobileName = "AirBeam mobile"
  override val methodFixedName = "AirBeam fixe"
  override val methodMicName = "Microphone du téléphone"
  override val methodMobileConfirm =
    "Vérifiez les détails de la session avant de commencer. Gardez votre AirBeam " +
      "avec vous, puis touchez « Démarrer l'enregistrement »."
  override val methodFixedConfirm =
    "Vérifiez les détails de la session avant de commencer. Installez votre AirBeam " +
      "dans un endroit sûr, puis touchez « Démarrer l'enregistrement »."
  override val methodMicConfirm =
    "Vérifiez les détails de la session avant de commencer. Placez votre téléphone " +
      "là où il capte ce que vous voulez mesurer, puis touchez « Démarrer l'enregistrement »."
  override val recordStartTitle = "Démarrer un nouvel enregistrement"
  override val recordStartBody =
    "Mesurez la qualité de l'air et le niveau sonore avec l'AirBeam ou votre téléphone."
  override val recordNewSessionTitle = "Nouvelle session"
  override val recordNewSessionBody =
    "Mesurez la qualité de l'air et le niveau sonore avec l'AirBeam ou votre téléphone."
  override val recordRecoverTitle = "Récupérer et synchroniser"
  override val recordRecoverBody =
    "Importez les sessions stockées sur votre AirBeam 3 ou AirBeam Mini."
  override val newSessionMethodTitle = "Comment souhaitez-vous mesurer ?"
  override val newSessionMethodBody =
    "Choisissez la méthode adaptée à votre matériel et à votre objectif."
  override val methodMobileTag = "AIRBEAM MOBILE"
  override val methodMobileTitle = "Mesure de l'air en déplacement"
  override val methodMobileBody =
    "Déplacez-vous et mesurez la pollution de l'air en temps réel avec un AirBeam connecté en Bluetooth."
  override val methodFixedTag = "AIRBEAM FIXE"
  override val methodFixedTitle = "Mesure de l'air en un seul lieu"
  override val methodFixedBody =
    "Surveillez un lieu en continu : maison, école ou travail. Connexion en Wi-Fi."
  override val methodMicTag = "MICROPHONE DU TÉLÉPHONE"
  override val methodMicTitle = "Mesure du bruit avec votre téléphone"
  override val methodMicBody =
    "Mesurez le niveau sonore partout avec le microphone intégré de votre téléphone."
  override val next = "Suivant"
  override val bluetoothPermissionNeeded =
    "Autorisez l'accès Bluetooth pour trouver les AirBeam à proximité."
  override val bluetoothPermissionTitle = "Autoriser les appareils à proximité"
  override val bluetoothPermissionBody =
    "AirCasting a besoin de cette autorisation pour trouver votre AirBeam et s'y connecter."
  override val bluetoothPermissionAllow = "Autoriser"
  override val bluetoothOffTitle = "Activez le Bluetooth"
  override val bluetoothOffBody =
    "Activez le Bluetooth sur votre téléphone pour vous connecter à l'AirBeam."
  override val noDevicesTitle = "Aucun AirBeam trouvé"
  override val noDevicesBody =
    "Nous n'avons détecté aucun AirBeam à proximité. Essayez ces étapes pour vous connecter."
  override val noDevicesStepPowerOn = "Vérifiez que votre AirBeam est allumé"
  override val noDevicesStepMoveCloser = "Rapprochez-vous de votre AirBeam"
  override val noDevicesStepBluetoothOn = "Vérifiez que le Bluetooth est activé"
  override val tryAgain = "Réessayer"
  override val refresh = "Actualiser"

  // TODO(design): placeholder copy, verbatim from Figma. Only the two buttons are real UI, so
  // only they are translated.
  override val onboardingMeasureTitle = "Placeholder heading"
  override val onboardingMeasureBody = OnboardingPlaceholderBody
  override val onboardingMapTitle = "Placeholder heading"
  override val onboardingMapBody = OnboardingPlaceholderBody
  override val onboardingShareTitle = "Placeholder heading"
  override val onboardingShareBody = OnboardingPlaceholderBody
  override val onboardingNext = "Étape suivante"
  override val onboardingSkip = "Passer"
  override val authWelcomeTitle = "Bienvenue sur AirCasting !"
  override val authWelcomeSubtitle =
    "Connectez-vous ou créez un compte ci-dessous pour surveiller la qualité de l'air."
  override val authSignInSheetSubtitle =
    "Connectez-vous pour continuer à surveiller la qualité de l'air."
  override val authSignUpSheetSubtitle =
    "Surveillez et enregistrez la qualité de l'air autour de vous"
  override val authTabSignIn = "Se connecter"
  override val authTabSignUp = "Créer un compte"
  override val authSignInAction = "Se connecter"
  override val authSignUpAction = "S'inscrire"
  override val authNoAccountPrompt = "Vous n'avez pas de compte ?"
  override val authHaveAccountPrompt = "Vous avez déjà un compte ?"
  override val authLoginLabel = "Saisissez l'e-mail ou le nom de profil"
  override val authEmailLabel = "Saisissez l'e-mail"
  override val authUsernameLabel = "Saisissez le nom de profil"
  override val authPasswordLabel = "Saisissez le mot de passe"
  override val authNewPasswordLabel = "Créez un mot de passe"
  override val authInvalidCredentials = "E-mail, nom de profil ou mot de passe incorrect."
  override val authUnexpectedError = "Une erreur s'est produite. Veuillez réessayer."
  override val authShowPassword = "Afficher le mot de passe"
  override val authHidePassword = "Masquer le mot de passe"
  override val authClearField = "Effacer"
  override val authForgotPassword = "Mot de passe oublié ?"
  override val forgotPasswordTitle = "Mot de passe oublié ?"
  override val forgotPasswordBody =
    "Saisissez l'e-mail associé à votre compte et nous vous enverrons un lien de " +
      "réinitialisation."
  override val forgotPasswordEmailLabel = "E-mail"
  override val forgotPasswordSubmit = "Envoyer le lien"
  override val forgotPasswordBackToSignIn = "Retour à la connexion"
  override val forgotPasswordSent = "Lien envoyé. Consultez votre boîte de réception."
  override val forgotPasswordFailed = "Impossible d'envoyer le lien. Veuillez réessayer."
  override val customServerTitle = "Utiliser un serveur de données personnalisé"
  override val customServerIntroBody =
    "Si vous hébergez votre propre serveur AirCasting (auto-hébergé ou " +
      "d'organisation), indiquez-le ici. Vos mesures existantes restent sur le serveur actuel — " +
      "seules les nouvelles données seront synchronisées vers le nouveau."
  override val customServerStep1 = "Saisissez l'URL et le port de votre serveur"
  override val customServerStep2 = "Vous serez déconnecté pour appliquer le changement"
  override val customServerStep3 =
    "Reconnectez-vous. Les nouvelles mesures se synchronisent automatiquement"
  override val customServerFormBody =
    "Saisissez l'adresse de votre serveur personnalisé ou auto-hébergé. " +
      "Nous testerons la connexion avant d'enregistrer quoi que ce soit."
  override val customServerUrlLabel = "URL"
  override val customServerUrlHint = "Saisissez une URL valide, p. ex. https://aircasting.org"
  override val customServerPortLabel = "Port"
  override val customServerPortHint = "Le port doit être un nombre entre 1 et 65535"
  override val customServerUseOfficial = "Utiliser le serveur officiel AirCasting"
  override val customServerNext = "Suivant"
  override val customServerTestingTitle = "Test de la connexion..."
  override val customServerTestingBody =
    "Nous vérifions que votre serveur répond correctement. Cela peut prendre un moment."
  override val customServerTooLong = "Cela prend trop de temps ? Annuler !"
  override val customServerVerifiedTitle = "Connexion vérifiée"
  override fun customServerVerifiedBody(server: String): String =
    "$server a répondu correctement. Vos mesures y seront synchronisées désormais. " +
      "Les données existantes resteront sur le serveur actuel."

  override val customServerFailedTitle = "Échec de la connexion"
  override fun customServerFailedBody(server: String): String =
    "Impossible de joindre $server. Vérifiez l'URL et le port, puis réessayez."

  override val customServerSave = "Enregistrer et se déconnecter"
  override val customServerEdit = "Revenir et modifier"
  override val customServerClearField = "Effacer"
  override fun settingsVersion(version: String): String = "AirCasting v$version"
  override val settingsTagline = "HabitatMap · Qualité de l'air en open source"
  override val settingsTitle = "Réglages"
  override val settingsAccountTitle = "Réglages du compte"
  override val settingsAccountSubtitle = "Modifier le mot de passe, supprimer le compte"
  override val settingsAirBeamsTitle = "Gestion des AirBeam"
  override val settingsAirBeamsSubtitle = "Nom, batterie, stockage, connexion"
  override val settingsAppTitle = "Réglages de l'application"
  override val settingsAppSubtitle = "Langue, notifications, confidentialité"
  override val settingsHelpTitle = "Aide"
  override val settingsHelpSubtitle = "FAQ, ressources, version de l'application"
  override val helpSectionActions = "Actions AirCasting"
  override val contentLocale = "fr"

  override val settingsAccountSectionHeader = "COMPTE"
  override val accountChangeEmail = "Modifier l'adresse e-mail"
  override val accountChangeUsername = "Modifier le nom d'utilisateur"
  override val accountChangePassword = "Modifier le mot de passe"
  override val accountCurrentPasswordLabel = "Mot de passe actuel"
  override val accountNewPasswordLabel = "Nouveau mot de passe"
  override val accountConfirmPasswordLabel = "Confirmez le nouveau mot de passe"
  override val accountSignOut = "Se déconnecter"
  override val accountDeleteAccount = "Supprimer le compte"
  override val accountSave = "Enregistrer"
  override val accountUpdateFailed =
    "Impossible d'enregistrer. La valeur est peut-être déjà utilisée ou invalide."
  override val fieldErrorTaken = "Déjà utilisé."
  override val fieldErrorBlank = "Ne peut pas être vide."
  override val fieldErrorTooShort: (min: String) -> String = { min -> "Doit contenir au moins $min caractères." }
  override val fieldErrorTooLong: (max: String) -> String = { max -> "Ne doit pas dépasser $max caractères." }
  override val fieldErrorPasswordMismatch = "Les mots de passe ne correspondent pas."
  override val fieldErrorWrongPassword = "Mot de passe incorrect."
  override val fieldErrorInvalid = "Valeur non valide."
  override val accountUpdateRefused = "Impossible d'enregistrer ces modifications."
  override val cancel = "Annuler"
  override val deleteAccountConfirmTitle = "Supprimer le compte ?"
  override val deleteAccountConfirmBody =
    "Cette action supprime définitivement votre compte et toutes les sessions que vous avez " +
      "enregistrées. Nous vous enverrons un code par e-mail pour confirmer."
  override val deleteAccountSendCode = "Envoyez-moi un code"
  override val deleteAccountCodeTitle = "Confirmer la suppression"
  override fun deleteAccountCodeBody(email: String): String = "Saisissez le code à 4 chiffres envoyé à $email. Il expire dans 30 minutes."
  override val deleteAccountCodeLabel = "Code à 4 chiffres"
  override val deleteAccountCodeInvalid = "Ce code est incorrect ou a expiré."
  override val deleteAccountResend = "Envoyer un nouveau code"
  override val appSettingsCommunityHeader = "COMMUNAUTÉ"
  override val appSettingsUnitsRegionHeader = "UNITÉS ET RÉGION"
  override val appSettingsDisplayHeader = "AFFICHAGE"
  override val appSettingsSensorsHeader = "CAPTEURS"
  override val appSettingsNotificationsHeader = "NOTIFICATIONS"
  override val appSettingsSyncHeader = "SYNCHRONISATION"
  override val appSettingsBackendHeader = "SERVEUR"
  override val appSettingCrowdMap = "Contribuer à la carte collaborative"
  override val appSettingCrowdMapSubtitle = "Partager les mesures avec la communauté"
  override val appSettingDisableMapping = "Désactiver la cartographie"
  override val appSettingDisableMappingSubtitle = "Ne plus enregistrer les données de localisation"
  override val appSettingTemperatureUnits = "Unités de température"
  override val appSettingRegionalFormats = "Formats régionaux"
  override val appSettingRegionalFormatsSubtitle = "Date, heure, distance et unités"
  override val appSettingLanguage = "Langue"
  override val appSettingMapType = "Type de carte"
  override val appSettingDarkMode = "Mode sombre"
  override val appSettingMicrophoneCalibration = "Calibrage du microphone"
  override val appSettingMicrophoneCalibrationSubtitle = "Calibrer le micro intégré"
  override val appSettingPushNotifications = "Notifications push"
  override val appSettingPushNotificationsSubtitle = "Alertes de qualité de l'air et rappels"
  override val appSettingWifiOnlySync = "Synchroniser uniquement en WiFi"
  override val appSettingWifiOnlySyncSubtitle =
    "Évite d'utiliser les données mobiles pendant la synchronisation"
  override val appSettingCustomDataServer = "Serveur de données personnalisé"
  override val appSettingCustomDataServerSubtitle = "Choisir où les données sont stockées"

  override val appSettingValueFahrenheit = "°F"
  override val appSettingValueCelsius = "°C"
  override val appSettingValueRegionUs = "US"
  override val done = "Terminé"
  override val appSettingValueRegionUk = "Format britannique"
  override val appSettingValueRegionCentralEuropean = "Europe centrale"
  override val appSettingValueRegionNordic = "Nordique"
  override val appSettingValueRegionEastAsian = "Asie de l'Est"
  override val appSettingFahrenheit = "Fahrenheit"
  override val appSettingFahrenheitDetail = "°F • ex. 72 °F"
  override val appSettingCelsius = "Celsius"
  override val appSettingCelsiusDetail = "°C • ex. 22 °C"
  override val unitMiles = "Milles"
  override val unitKilometers = "Kilomètres"
  override val appSettingValueMapDefault = "Par défaut"
  override val appSettingValueMapSatellite = "Satellite"
  override val languageName = "Français"

  override val micLiveReading = "LECTURE EN DIRECT"
  override val micDecibels = "db"
  override val micCalibrationHint =
    "Plage typique : 80–100. Ajustez la valeur ci-dessous jusqu'à ce que la lecture en direct " +
      "y corresponde."
  override val micCalibrationOffset = "DÉCALAGE DE CALIBRAGE"
  override fun micResetOffset(offset: Int): String = "Rétablir le décalage par défaut ($offset)"
  override val micDecreaseOffset = "Diminuer le décalage"
  override val micIncreaseOffset = "Augmenter le décalage"
  override val micPermissionNeeded =
    "Autorisez l'accès au microphone pour voir une lecture en direct."

  override val openSettings = "Ouvrir les réglages"
  override val back = "Retour"
  override val appLogo = "AirCasting"
  override val errorTitle = "Une erreur s'est produite."
  override val errorRetry = "Réessayer"

  override val airQualityLabel = "QUALITÉ DE L'AIR"
  override val noReadingsTitle = "Aucune donnée de qualité de l'air disponible"
  override val noReadingsBody =
    "Nous n'avons pas trouvé de mesures récentes pour ce lieu.\nRéessayez plus tard."
  override val noLocationTitle = "Aucune station à proximité"
  override val noLocationBody =
    "Nous n'avons pas pu associer votre position à une station de mesure.\n" +
      "Essayez d'activer la localisation précise."
  override val turnOnLocation = "Activer les services de localisation"

  override val aqGoodLabel = "Bonne"
  override val aqGoodDescription =
    "L'air extérieur est sain. Le moment idéal pour des activités en plein air."
  override val aqModerateLabel = "Modérée"
  override val aqModerateDescription =
    "La qualité de l'air est acceptable pour la plupart des personnes."
  override val aqUnhealthySensitiveLabel = "Mauvaise pour les groupes sensibles"
  override val aqUnhealthySensitiveDescription =
    "Les groupes sensibles devraient limiter les efforts en plein air."
  override val aqUnhealthyLabel = "Mauvaise"
  override val aqUnhealthyDescription =
    "Tout le monde peut commencer à en ressentir les effets. Limitez le temps passé dehors."
  override val aqHazardousLabel = "Dangereuse"
  override val aqHazardousDescription = "Alerte sanitaire. Évitez toute activité en plein air."

  override val pollutantPm25 = "PM 2,5"
  override val pollutantNo2 = "NO₂"
  override val pollutantOzone = "Ozone"

  override val nearbyStationsTitle = "Stations à proximité"
  override val viewMap = "Voir la carte"
  override val govMonitor = "STATION OFFICIELLE"

  override fun distanceAway(miles: Double): String = "à $miles mi"
  override val ageJustNow = "à l'instant"
  override fun ageMinutesAgo(minutes: Long): String = "il y a $minutes min"
  override fun ageHoursAgo(hours: Long): String = "il y a $hours h"
  override fun ageDaysAgo(days: Long): String = "il y a $days j"
  override val tabHome = "Accueil"
  override val tabExplore = "Explorer"
  override val tabRecord = "Enregistrer"
  override val tabFavorites = "Favoris"
  override val tabMyData = "Mes données"
  override val exploreSearchHint = "Rechercher par nom ou lieu"
  override val exploreFilters = "Filtres"
  override val exploreFilterSource = "SOURCE"
  override val exploreFilterParameter = "PARAMÈTRE"
  override val exploreShowAll = "Tout afficher"
  override val exploreOnlyAirBeam = "AirBeam fixes uniquement"
  override val exploreOnlyGovernment = "Gouvernement uniquement"
  override val exploreSourceAirBeam = "AirBeam fixe"
  override val exploreSourceGovernment = "Gouvernement"
  override val exploreParticulateMatter = "Particules fines"
  override val exploreNitrogenDioxide = "Dioxyde d'azote (NO₂)"
  override val exploreOzone = "Ozone"
  override val exploreTemperature = "Température"
  override val exploreHumidity = "Humidité"
  override val exploreClearAll = "Tout effacer"
  override fun exploreShowResults(n: Int?): String =
    when (n) {
      null -> "Voir les résultats"
      0, 1 -> "Voir $n résultat"
      else -> "Voir $n résultats"
    }

  override fun exploreRemoveFilter(name: String): String = "Retirer le filtre : $name"
}
