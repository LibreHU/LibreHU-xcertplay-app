# CarPlay LibreHU

Fork de [xcertplay](https://github.com/shilapi/xcertplay) (recepteur CarPlay en appli Android, filaire et sans fil),
pour les autoradios LibreHU (AC8257 / UJC201). Licence inchangee : **GPL-3.0** (`LICENSE`).

## Filaire ou sans fil
xcertplay fait les deux, **un seul a la fois** : interrupteur « Wireless CarPlay » de ses reglages (ouverts par le
bouton en bas a droite ou en glissant trois doigts vers le bas), puis « Save & Reconnect ».
- **Active** (valeur par defaut de xcertplay) : sans fil. L'iPhone doit etre appaire en Bluetooth ; l'appli ouvre le
  point d'acces Wi-Fi puis le canal iAP2 en Bluetooth. **La prise USB est ignoree** dans ce mode.
- **Desactive** : filaire. L'appli attend l'iPhone sur l'USB (iAP2 + USBMUX + NCM) ; c'est le seul mode ou
  LibreHU-service recoit les details de la batterie (cycles, temperature, capacites : Lockdown passe par l'USB).

## Ce que change le fork
- **Video basse latence sur Android 9** : le decodeur MediaTek recoit `vdec-lowlatency` (la cle officielle
  `KEY_LOW_LATENCY` n'existe qu'a partir d'Android 11), comme le fait Moonlight ; refusee, le decodeur est reconfigure
  sans. Interrupteur dans LibreHU-service (Projection → CarPlay, colonne `carplay_video_low_latency` du fournisseur
  vehicule), actif par defaut (`shared/.../media/AndroidMediaSink.kt`, `common/.../librehu/LibreHuVehicle.kt`).
- **Paquet `org.librehu.carplay`** (mobile et automotive) : s'installe a cote de xcertplay. L'identifiant de
  protocole iAP2 (`com.shilapi.xcertplay`) ne change pas.
- **Signe avec la cle LibreHU** (`keystore/librehu.jks`, ou `ANDROID_KEYSTORE_PATH` et les autres variables de
  xcertplay).
- **Nom et icone** : affiche « CarPlay » sous l'icone. Icone originale (voiture Material, Apache-2.0, sur fond bleu)
  avec un glyphe blanc (`org.librehu.themed_icon`) que LibreHU Launcher colore avec l'accent du theme. Le logo CarPlay
  d'Apple n'est pas repris (voir « Marques »). Autre image sur son autoradio : appui long sur l'appli dans LibreHU
  Launcher → « Changer l'icone ».
- **Bus I2C trouve tout seul** : le reglage « I2C device » vaut `auto` par defaut (et la cible MFi par defaut est
  l'I2C natif) : chaque `/dev/i2c-N` que l'appli peut ouvrir est sonde (adresses 0x10 puis 0x11, 1 s par bus, comme
  le scanner de xcertplay) et le premier ou la puce repond est garde pour la session (journal : `mfi Linux I2C
  coprocessor found path=...`). Un chemin `/dev/i2c-N` explicite marche toujours. Le diagnostic I2C accepte `auto`.
- **Vehicule depuis LibreHU-service** (`librehu/LibreHuVehicle.kt`, permission signature
  `org.librehu.permission.VEHICLE`) : nom de la voiture (affiche sur l'iPhone), marque, modele, cote conducteur et
  vraies adresses Bluetooth et Wi-Fi de l'autoradio (Android les cache aux applis : xcertplay envoyait
  `02:00:00:00:00:01`, ce qui gene l'appairage sans fil). Le nom, la marque et le modele vont aussi dans
  l'identification iAP2 (nom affiche dans les reglages CarPlay de l'iPhone ; xcertplay envoyait « xcertplay »).
  Lu au demarrage (attendu jusqu'a 1,5 s avant de preparer la session : lu en tache de fond, la premiere session
  apres avoir nomme la voiture partait encore avec « xcertplay ») et a chaque changement ; la derniere fiche lue est
  gardee et servie des le demarrage. L'iPhone garde le nom d'une voiture deja connue : « Oublier cette voiture »
  dans ses reglages CarPlay pour voir le nouveau. Une valeur vide garde le reglage de xcertplay. Si le
  nom reste « xcertplay » : reinstaller CarPlay apres LibreHU-service (Android 9 n'accorde une permission signature
  qu'a l'installation, si l'appli qui la declare est deja la). En sans fil, l'identifiant reste le BSSID du point d'acces quand il est connu.
- **Theme et nuit LibreHU** (`librehu/LibreHuCarPlay.kt`) : les ecrans de l'appli prennent le mode clair / sombre
  et la couleur d'accent de LibreHU Launcher (`content://org.librehu.launcher.theme/theme`, diffusion
  `org.librehu.action.THEME_CHANGED`). Le jour / nuit envoye a l'iPhone (`setNightMode` d'AirPlay) suit, dans
  l'ordre : LibreHU-service (phares, `org.librehu.carplay.action.SET_NIGHT_MODE`, accepte seulement des applis
  signees LibreHU : permission signature `org.librehu.carplay.permission.CONFIGURE`), sinon le mode sombre du
  launcher, sinon celui d'Android.
- **Etat de la session** diffuse pour LibreHU-service (`org.librehu.carplay.SESSION_STATE`, `state` = connecting /
  active / ended / failed, `detail`) : onglet Projection → CarPlay du service.
- **Numero de version** : `versionCode` = celui d'amont (1400) + nombre de commits du depot. LibreHU Apps propose une
  mise a jour quand le `versionCode` augmente ; celui d'amont ne bougeait pas d'un build du fork a l'autre, la mise a
  jour n'apparaissait donc pas. Le `versionName` reste celui d'amont suivi de `-<branche>` ; le commit est sur sa propre ligne de `release.txt`.
- **CI** (`.github/workflows/librehu.yml`) : APK `mobile` en release, pre-release continue `latest-<branche>` avec
  `release.txt` (LibreHU Apps) ; le workflow de xcertplay (qui exige ses secrets de signature) est retire.

Les Actions GitHub d'un fork sont desactivees au depart : les activer dans l'onglet Actions du depot pour que la
CI publie l'APK.

## Fichiers modifies par rapport a l'amont

Base : commit amont `551895a` (xcertplay « 1.4.0 », 11/10/2026), branche locale `librehu` publiee sur `master`.
`git diff 551895a HEAD --stat` donne la liste complete ; les modifications du code sont marquees `LibreHU` en
commentaire.

| Fichier(s) | Modification |
|---|---|
| `mobile/build.gradle.kts`, `automotive/build.gradle.kts` | paquet `org.librehu.carplay`, signature par la cle LibreHU, `versionCode` = 1400 + nombre de commits (`providers.exec`, compatible cache de configuration) |
| `common/build.gradle.kts` | `kotlinx-coroutines-android` (theme du launcher et nuit de LibreHU-service en `StateFlow`) |
| `mobile/src/main/AndroidManifest.xml`, `automotive/src/main/AndroidManifest.xml` | icone thémable (`org.librehu.themed_icon`) |
| `common/src/main/AndroidManifest.xml` | permission signature `org.librehu.carplay.permission.CONFIGURE` declaree ; `org.librehu.permission.VEHICLE` demandee ; visibilite des fournisseurs LibreHU (theme du launcher, fiche voiture) |
| `mobile/src/main/AndroidManifest.xml`, `automotive/src/main/AndroidManifest.xml` | pas de sauvegarde adb (`allowBackup` false) ; `org.librehu.permission.VEHICLE` declaree aussi dans `mobile` (LibreHU-service ne prend `PHONE_INFO` que d'une appli qui l'a ; declaree ici, elle existe quel que soit l'ordre d'installation) |
| `shared/src/main/AndroidManifest.xml` | `usesCleartextTraffic` garde, explique : seul le serveur MFi distant optionnel peut etre en `http://` (serveur du reseau local) ; CarPlay lui-meme passe par des sockets |
| `automotive/build.gradle.kts`, `automotive/src/main/keepRules/librehu.keep` | R8 aussi pour le module automotive (non publie), memes regles que mobile |
| `common/.../librehu/LibreHuVehicle.kt` (nouveau) | fiche voiture de LibreHU-service (nom, marque, modele, cote conducteur, vraies adresses Bluetooth / Wi-Fi), derniere fiche gardee et servie des le demarrage |
| `common/.../librehu/LibreHuCarPlay.kt` (nouveau) | theme et accent du launcher, nuit de CarPlay commandee par LibreHU-service (phares), etat de la session diffuse, infos de l'iPhone envoyees a LibreHU-service seul (`org.librehu.carplay.PHONE_INFO`, paquet `org.librehu.service`) |
| `common/.../CarPlayHostActivity.kt` | nom, marque et modele de la voiture dans la configuration AirPlay **et** dans l'identification iAP2 (nom affiche sur l'iPhone) ; adresses reelles ; mode nuit ; rapports de session ; demarrage de `LibreHuVehicle` / `LibreHuCarPlay` |
| `shared/.../transport/LockdownDeviceInfoClient.kt` (nouveau) | infos de l'iPhone par la session Lockdown deja appairee pour CarPlay filaire, comme libimobiledevice / pymobiledevice3 : `GetValue` (nom, modele `ProductType`, iOS, adresse Bluetooth), domaine `com.apple.mobile.battery` (niveau, charge, alimentation), service `com.apple.mobile.diagnostics_relay` requete `IORegistry` classe `IOPMPowerSource` (cycles, temperature, capacites, tension, courant) ; relu toutes les 60 s |
| `shared/.../transport/LockdownPlistChannel.kt` | plist : `<array>`, `<real>`, `<date>` lus (et ecrits), necessaires aux reponses de diagnostic |
| `shared/.../librehu/IphoneInfoBus.kt` (nouveau), `shared/.../orchestration/CarPlayController.kt` | iAP2 `DeviceInformationUpdate` (0x4E09, nom), `PowerUpdate` (0xAE01 : chargeur externe, etat de charge, niveau), `DeviceTransportIdentifier` (0x4E0E, adresse Bluetooth) fusionnes avec les infos Lockdown ; nom et modele de l'iPhone par AirPlay (filaire et sans fil) ; en sans fil, nom et adresse de l'iPhone connecte en Bluetooth ; `wired` (USB ou sans fil) pour LibreHU-service ; effaces a la fin de la session. Ni l'identification iAP2 ni les messages envoyes ne changent : seuls des messages deja recus sont lus |
| `common/.../ui/theme/Theme.kt` | ecrans Compose au clair / sombre et a l'accent du launcher |
| `common/.../AirPlayPersistence.kt` | cible MFi par defaut = I2C, peripherique I2C par defaut = `auto` |
| `common/.../MainActivity.kt` | auto-test MFi sur le bus trouve automatiquement |
| `shared/.../orchestration/MfiRuntime.kt`, `CarPlayController.kt` | `auto` : chaque `/dev/i2c-N` accessible est sonde aux adresses 0x10 / 0x11 pour trouver la puce MFi |
| `shared/.../orchestration/CarPlayController.kt` | **iPhone rebranché** : à la fin d'une session filaire (iPhone débranché, CarPlay quitté sur le téléphone, échec de la mise en route), USBMUX, iAP2, lecture des infos et VPN/NCM sont fermés et le contrôleur repasse en attente d'iPhone (2 s, puis jusqu'à 30 s si les essais échouent) ; un iPhone rebranché pendant la fermeture relance tout de suite. Avant, le contrôleur restait en phase de contrôle et ignorait le nouvel appareil USB jusqu'au redémarrage de l'appli |
| `common/src/main/res/values/strings.xml` | nom « CarPlay » sous l'icone |
| `shared/src/main/res/drawable/librehu_launcher_*.xml`, `shared/src/main/res/mipmap-anydpi-v26/ic_launcher*.xml` | icone originale et glyphe thémable |
| `keystore/librehu.jks` | cle de signature commune des applis LibreHU |
| `.github/workflows/librehu.yml` (nouveau), `android.yml` (retire) | CI LibreHU (APK signe, `latest-master`, `release.txt`) a la place de celle d'amont |
| `README.md`, `README_LIBREHU.md` | renvoi vers ce fichier ; ce fichier |

## Puce MFi de l'UJC201 (a valider sur l'unite)
- La puce d'authentification Apple est sur un bus I2C du SoC ; la ROM stock (`iapd`, `libcoprocessor.so`) lit le bus
  et l'adresse dans la **metazone** et ouvre `/dev/i2c-N` (voir `docs/projection_stock.md` du depot device). Choisir
  ce `/dev/i2c-N` dans les reglages (I2C natif), ou laisser `auto`.
- xcertplay ne contourne pas les droits (`LinuxI2cTransport`) : il faut que l'appli puisse ouvrir le noeud
  (droits du fichier et SELinux), ce qui demande root. Et `iapd` doit etre arrete pendant ce temps
  (`setprop vendor.iapd.start 0; stop iapd`), sinon deux clients parlent a la puce.
- A faire : une action de LibreHU-service (root) qui prepare la puce pour cette appli (arret d'`iapd`, droits du
  noeud, regle SELinux) et la rend a CPLink ensuite ; theme LibreHU ; integration au launcher comme Android Auto.

## Marques
CarPlay et Apple sont des marques d'Apple Inc. Ce fork n'est ni affilie a Apple ni certifie MFi par Apple ; le nom
« CarPlay » sous l'icone decrit seulement le protocole que l'appli recoit. Apple n'autorise pas ses marques et logos
dans le nom ou l'icone d'une appli tierce sans licence
([Guidelines for Using Apple Trademarks](https://www.apple.com/legal/intellectual-property/guidelinesfor3rdparties.html)) :
le logo n'est donc pas inclus dans ce depot public.

## Suivre xcertplay
```
git fetch upstream && git merge upstream/master
```

## Optimisation (R8)

L'APK publié par la CI (`assembleRelease`) est **optimisé par R8** : code et ressources réduits et optimisés, ce qui
rend Jetpack Compose nettement plus fluide ([Compose performance](https://developer.android.com/develop/ui/compose/performance)),
**sans renommage** (`-dontobfuscate` dans `mobile/src/main/keepRules/librehu.keep`) : les traces des journaux restent lisibles. Les
règles gardent aussi ce qui n'est appelé que par son nom (méthodes natives, exception levée par le JNI I2C). Si
une fonction plante avec « class / method not found », regarder `usage.txt` dans l'artefact *R8 reports* de la CI (ce
que R8 a retiré) et ajouter une règle `-keep`.


**Style rétro et police de LibreHU Launcher** : quand le launcher est en style rétro (les couleurs d'un afficheur
d'autoradio tirées de sa couleur d'accent, couleur RGB comprise), cette appli prend sa palette (jour ou nuit) ; quand
la **police d'afficheur à segments** y est choisie (dans tous les styles), cette appli la prend aussi, en majuscules
sans accents (« LibreHU Seg14 », tirée de DSEG14 © keshikan, SIL Open Font License 1.1, voir
`third_party/DSEG-LICENSE.txt`). Le launcher publie le style, la police et la palette par
`content://org.librehu.launcher.theme/theme` (colonnes `style`, `font`, `background`, `surface`, `surface_high`,
`on_accent`, `text`, `text_dim`) et par la diffusion `org.librehu.action.THEME_CHANGED` (mêmes noms en extras).
