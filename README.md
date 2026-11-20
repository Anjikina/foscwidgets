# foscwidgets

Nederlandstalige Android-app met volledig transparante startschermwidgets. Voor Samsung Galaxy S24 en Android 8 of hoger.

## Mogelijkheden

- Tot 12 snelkoppelingen per widget: apps, websites, bestanden en bewerkbare notities.
- Meerdere afzonderlijk instelbare widgets; 1 tot 4 kolommen.
- Tekstgrootte 10–28 sp, eigen tekstkleur, optionele pictogrammen.
- Een notitie openen door erop te tikken; wijzigingen worden bij afsluiten of verlaten opgeslagen.
- Blijvende leesrechten voor zelf gekozen bestanden via de Android-bestandskiezer.
- Geen account, abonnement, advertenties of internettoegang vanuit de app zelf. Websites openen in je browser.

## Installeren

Open `foscwidgets.apk` op je telefoon en installeer de app. Open daarna foscwidgets en tik op **Widget toevoegen**. Je kunt ook een lege plek op je startscherm ingedrukt houden en **Widgets → foscwidgets** kiezen.

Voeg snelkoppelingen toe, kies het uiterlijk en tik op **Opslaan**. Vergroot de widget op het startscherm wanneer tekst wordt afgekapt. De zichtbare titel in de app helpt je widgets uit elkaar houden; die titel staat niet boven de widget.

Zonder de link **Bewerken** kun je een widget nog steeds via de app aanpassen. Voor het bewerken van een app- of bestandssnelkoppeling kun je de naam wijzigen; om het doel te vervangen verwijder je de snelkoppeling en voeg je een nieuwe toe.

## Gegevens

Instellingen en notities blijven lokaal. Verwijderen van een widget verwijdert zijn instellingen en notities. Verwijderen van de app of wissen van appgegevens verwijdert alle instellingen en notities. Er is in deze eerste versie geen export- of synchronisatiefunctie.

Bestanden worden niet gekopieerd. Als een bestand verhuist, wordt verwijderd of toegang verliest, kies je het opnieuw. Een bestand opent alleen als er een geschikte app is geïnstalleerd.

## Bouwen

JDK 17, Android SDK 35 en Gradle 8.9 via de meegeleverde wrapper:

```
./gradlew assembleDebug lintDebug
./gradlew connectedDebugAndroidTest
```

De tweede opdracht vereist een aangesloten Android-testtoestel of emulator. De tests controleren opslag, onafhankelijke widgets, de transparante RemoteViews-indelingen, instellingen, annuleren, ongeldige kleuren en het bewerken van notities.

Voor een ondertekende release stel je `FOSC_KEYSTORE_FILE` en `FOSC_KEYSTORE_PASSWORD` in. De sleutelalias is `foscwidgets`. Daarna: `./gradlew assembleRelease`.

## GitHub Actions

De workflow staat in `.github/workflows/android.yml`. Plaats dit project in `Anjikina/foscwidgets` met `main` als standaardbranch. Zet in **Settings → Secrets and variables → Actions** twee repository secrets:

- `FOSC_KEYSTORE_BASE64`: de volledige inhoud van `FOSC_KEYSTORE_BASE64.txt` uit het afzonderlijke privé-ondertekeningspakket.
- `FOSC_KEYSTORE_PASSWORD`: de inhoud van `FOSC_KEYSTORE_PASSWORD.txt` uit datzelfde pakket.

Upload het privé-ondertekeningspakket nooit als broncode. Het bevat de sleutel waarmee toekomstige versies dezelfde app kunnen bijwerken. Bewaar een eigen veilige kopie.

Na een push naar `main` of een handmatige start onder **Actions → Android APK → Run workflow** verschijnt bij een geslaagde run het artifact **foscwidgets-apk**, met de installeerbare APK. De workflow controleert de code en compileert de praktijktests; de tests op een toestel zijn afzonderlijk lokaal uitgevoerd. Publicatie en een eerste geslaagde GitHub-run zijn nog niet bevestigd.

## Eerste versie

Versie 0.1.0. Getest op een Android 15-emulator; controle op een fysieke Samsung S24 blijft nodig. Zeer kleine widgets en lange notities kunnen tekst afkappen; de widget is in beide richtingen schaalbaar.
