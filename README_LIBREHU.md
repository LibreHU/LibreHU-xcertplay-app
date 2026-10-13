# CarPlay LibreHU

Fork de [xcertplay](https://github.com/shilapi/xcertplay) (recepteur CarPlay en appli Android, filaire et sans fil),
pour les autoradios LibreHU (AC8257 / UJC201). Licence inchangee : **GPL-3.0** (`LICENSE`).

## Ce que change le fork (pour l'instant)
- **Paquet `org.librehu.carplay`** (mobile et automotive) : s'installe a cote de xcertplay. L'identifiant de
  protocole iAP2 (`com.shilapi.xcertplay`) ne change pas.
- **Signe avec la cle LibreHU** (`keystore/librehu.jks`, ou `ANDROID_KEYSTORE_PATH` et les autres variables de
  xcertplay).
- **CI** (`.github/workflows/librehu.yml`) : APK `mobile` en release, pre-release continue `latest-<branche>` avec
  `release.txt` (LibreHU Apps) ; le workflow de xcertplay (qui exige ses secrets de signature) est retire.

## Puce MFi de l'UJC201 (a valider sur l'unite)
- La puce d'authentification Apple est sur un bus I2C du SoC ; la ROM stock (`iapd`, `libcoprocessor.so`) lit le bus
  et l'adresse dans la **metazone** et ouvre `/dev/i2c-N` (voir `docs/projection_stock.md` du depot device). Choisir
  ce `/dev/i2c-N` dans les reglages de xcertplay (I2C natif).
- xcertplay ne contourne pas les droits (`LinuxI2cTransport`) : il faut que l'appli puisse ouvrir le noeud
  (droits du fichier et SELinux), ce qui demande root. Et `iapd` doit etre arrete pendant ce temps
  (`setprop vendor.iapd.start 0; stop iapd`), sinon deux clients parlent a la puce.
- A faire : une action de LibreHU-service (root) qui prepare la puce pour cette appli (arret d'`iapd`, droits du
  noeud, regle SELinux) et la rend a CPLink ensuite ; theme LibreHU ; integration au launcher comme Android Auto.

## Suivre xcertplay
```
git fetch upstream && git merge upstream/master
```
