# CarPlay LibreHU

Fork de [xcertplay](https://github.com/shilapi/xcertplay) (recepteur CarPlay en appli Android, filaire et sans fil),
pour les autoradios LibreHU (AC8257 / UJC201). Licence inchangee : **GPL-3.0** (`LICENSE`).

## Ce que change le fork
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
  `02:00:00:00:00:01`, ce qui gene l'appairage sans fil). Lu au demarrage et a chaque changement ; une valeur vide
  garde le reglage de xcertplay. En sans fil, l'identifiant reste le BSSID du point d'acces quand il est connu.
- **CI** (`.github/workflows/librehu.yml`) : APK `mobile` en release, pre-release continue `latest-<branche>` avec
  `release.txt` (LibreHU Apps) ; le workflow de xcertplay (qui exige ses secrets de signature) est retire.

Les Actions GitHub d'un fork sont desactivees au depart : les activer dans l'onglet Actions du depot pour que la
CI publie l'APK.

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
