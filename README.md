# foscwidgets

Nederlandstalige Android-app met volledig transparante startschermwidgets. Voor Samsung Galaxy S24 en Android 8 of hoger.

## Mogelijkheden

- Tot 12 snelkoppelingen per widget: apps, websites, bestanden en bewerkbare notities.
- Meerdere afzonderlijk instelbare widgets; 1 tot 4 kolommen.
- Formaatkeuze bij toevoegen: 1×1, 2×1, 2×2, 3×2 of 4×2. Alle formaten kunnen op het startscherm tot 1×1 worden verkleind.
- Samsung Notes: kies een bestaande notitie via de eigen notitiekiezer van Samsung Notes en open die rechtstreeks vanuit de widget.
- Tekstgrootte 10–28 sp, eigen tekstkleur, optionele pictogrammen.
- Een notitie openen door erop te tikken; wijzigingen worden bij afsluiten of verlaten opgeslagen.
- Blijvende leesrechten voor zelf gekozen bestanden via de Android-bestandskiezer.
- Geen account, abonnement, advertenties of internettoegang vanuit de app zelf. Websites openen in je browser.

## Installeren

Open `foscwidgets.apk` op je telefoon en installeer de app als update over de vorige versie. Open daarna foscwidgets, tik op **Widget toevoegen** en kies een formaat, bijvoorbeeld **1×1**. Je kunt ook een lege plek op je startscherm ingedrukt houden en **Widgets → foscwidgets** kiezen.

Voeg snelkoppelingen toe, kies het uiterlijk en tik op **Opslaan**. Vergroot de widget op het startscherm wanneer tekst wordt afgekapt. De zichtbare titel in de app helpt je widgets uit elkaar houden; die titel staat niet boven de widget.

Houd de widget op het startscherm ingedrukt en sleep de randen om de grootte te wijzigen. Android bepaalt de uiteindelijke plaatsing op het startschermraster. Een compact formaat toont alleen de eerste snelkoppelingen die passen; overige items verschijnen na vergroten. Op kleine widgets worden de bewerklink, de notitie-inhoud en pictogrammen die niet passen automatisch verborgen. De titel van de snelkoppeling blijft zichtbaar.

Zonder de link **Bewerken** kun je een widget nog steeds via de app aanpassen. Voor het bewerken van een app- of bestandssnelkoppeling kun je de naam wijzigen; om het doel te vervangen verwijder je de snelkoppeling en voeg je een nieuwe toe.

## Bestaande notities uit Samsung Notes

Kies **Snelkoppeling toevoegen → Samsung Notes · bestaande notities**. Samsung Notes opent zijn eigen kiezer voor bestaande notities. Selecteer een notitie en geef de snelkoppeling daarna een herkenbare naam. De notitie blijft in Samsung Notes en wordt daar geopend en bewerkt; foscwidgets bewaart alleen de koppeling.

De koppeling gebruikt `ACTION_PICK` met `application/sdocuuid` en de door Samsung teruggegeven notitie-ID. De beschikbare kiezer en de openbare activiteit voor het openen van een bestaande notitie zijn op de aangesloten S24 met Samsung Notes 4.4.48.15 gecontroleerd. Er wordt geen Samsung-notitie gekopieerd of naar GitHub verstuurd; foscwidgets leest de afgeschermde Samsung Notes-database niet uit. Vergrendelde notities blijven onder de beveiliging van Samsung Notes vallen.

Bij andere Samsung Notes-versies probeert de app een aangeboden Android-snelkoppelingenkiezer. Ontbreekt ook die, dan biedt de app **Notitielijst koppelen** aan; die reserveoptie opent de lijst en niet rechtstreeks één specifieke notitie.

**Nieuwe eigen notitie** blijft beschikbaar voor notities die je binnen foscwidgets wilt bijhouden.

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

Na een push naar `main` of een handmatige start onder **Actions → Android APK → Run workflow** verschijnt bij een geslaagde run het artifact **foscwidgets-apk**, met de installeerbare APK. De workflow controleert de code en compileert de praktijktests; de tests op een toestel zijn afzonderlijk lokaal uitgevoerd. De broncode is gepubliceerd op [Anjikina/foscwidgets](https://github.com/Anjikina/foscwidgets) en de twee ondertekeningssecrets zijn ingesteld. Bekijk de actuele bouwresultaten onder **Actions**.

## Eerste versie

Versie 0.3.3. De startschermformaten en compacte weergave zijn op een Android 15-emulator gecontroleerd. Zeer kleine widgets en lange notities kunnen tekst afkappen; de widget is in beide richtingen schaalbaar.

Namen zijn optioneel. Laat de naam van een snelkoppeling leeg om de zichtbare titel te verbergen. Pictogrammen blijven naar keuze zichtbaar en de snelkoppeling blijft aanklikbaar. Ook de widgetnaam in de app mag leeg blijven; alleen de beheerlijst gebruikt dan een nummer om widgets uit elkaar te houden.

Nieuwe widgets stellen standaard 1 kolom voor. Een eerder opgeslagen kolomkeuze blijft behouden.

## Widget beveiligen

Open de widgetinstellingen, ga naar **Beveiliging** en kies:

- **Geen vergrendeling**.
- **Pincode / schermvergrendeling**: Android vraagt de bestaande ontgrendeling van je telefoon.
- **Vingerafdruk of pincode**: Android biedt sterke biometrie, zoals de vingerafdruk op de S24, en de bestaande telefoonontgrendeling als alternatief.

Er wordt geen nieuwe pincode aangemaakt en foscwidgets ontvangt geen pincode of vingerafdrukgegevens. Voor inschakelen moet een veilige telefoonvergrendeling ingesteld zijn. Op oudere Android-versies gebruikt de app de telefoonontgrendeling.

Elke beveiligde widget vraagt bij het openen om ontgrendeling. Ook de widgetinstellingen zijn beschermd, zodat je de beveiliging niet zonder ontgrendelen kunt uitschakelen. Een beveiligde eigen notitie toont geen inhoud op het startscherm. Bij verlaten van de app verdwijnt de beveiligde inhoud en is opnieuw ontgrendelen nodig; systeemschermopnamen van beveiligde schermen worden geblokkeerd.

Dit beveiligt toegang via foscwidgets. Een gekoppelde app, website of bestand kan daarnaast via zijn eigen app worden geopend en houdt zijn eigen beveiliging. Het is geen algemene appvergrendeling of versleutelde bestandskluis.

## Tijdelijk openingsscherm

Het scherm dat een widgetsnelkoppeling opent, staat niet in **Recente apps**. Bij een externe bestemming sluit dit scherm automatisch zodra de bestemming is geopend, zowel zonder beveiliging als na een geslaagde pincode- of biometrische controle. De bestemming krijgt haar eigen appvenster en blijft open. Annuleren sluit het tijdelijke scherm ook.

Na een appupdate worden widgets automatisch ververst en oude tijdelijke openingsvensters opgeruimd of verborgen. De gewone foscwidgets-instellingen en de eigen notitie-editor blijven beschikbaar wanneer je die bewust opent.

Bij het openen van een beveiligde widgetsnelkoppeling blijft de achtergrond zichtbaar en verschijnt meteen de systeemvraag voor vingerafdruk of telefoonontgrendeling, zonder eigen vergrendelpagina. Samsung/Android bepaalt het formaat van de pincodevraag; de app kan deze niet tot een eigen kleine popup verkleinen. De notitie-editor verschijnt pas na ontgrendelen.
