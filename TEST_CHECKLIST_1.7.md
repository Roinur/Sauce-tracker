# Sauce Tracker 1.7 - testchecklista

APK: `dist/Sauce-Tracker-1.7-release.apk`

Releaseversionen uppdaterar en kompatibelt signerad Sauce Tracker-installation. Ta en backup innan installation och kontrollera att Android accepterar signaturen.

## 1. Grundläggande smoke test

- [ ] Appen i launchern heter **Sauce Tracker**.
- [ ] 1.7 startar utan krasch och visar importerade entries.
- [ ] Tema, accent, dashboard och navigation behåller det etablerade utseendet.
- [ ] App-lock fungerar efter att appen lämnas längre än grace-perioden.
- [ ] Incognito i Library döljer känsligt innehåll och stoppar Desktop Bridge enligt tidigare beteende.
- [ ] Incognito i Browser behåller sitt separata sessions-/rensningsbeteende.

## 2. Library efter Browser och appbyte

- [ ] Öppna **Entries** och bekräfta att galleri/cards syns.
- [ ] Öppna en importerad entry i Browser och gå tillbaka direkt: Library ska fortfarande synas.
- [ ] Upprepa, byt till en annan app medan Browser är öppen, återvänd till Sauce Tracker och stäng Browser.
- [ ] Entries ska visas direkt utan att ändra tags, filter eller sortering.
- [ ] Testa både gallery layout och normal entry layout.
- [ ] Slå på Legacy home UI och kontrollera att dess Expand/Collapse fortfarande fungerar.

## 3. Nätverks- och webbplatsfel

- [ ] Med fungerande nät: hämta en giltig kod.
- [ ] Testa en kod som inte finns: 404 ska inte loopa eller retryas länge.
- [ ] Stäng av nätet och hämta en kod: texten ska säga nätverksproblem, inte bara API-fel.
- [ ] Slå på nätet igen och prova samma kod.
- [ ] Kontrollera Browser search och subscription refresh efter nätåterkomst.

## 4. Entry Heatmap med stort bibliotek

- [ ] Öppna den redan precalculerade **Entries heatmap** med hela biblioteket.
- [ ] Den ska öppna på **10%** utan krasch.
- [ ] Alla entries ska finnas som punkter även utanför thumbnailområdet.
- [ ] Under drag och zoom ska entries renderas som lätta punkter.
- [ ] När rörelsen stannar ska thumbnails laddas i området kring skärmens mitt.
- [ ] Byt i ordning 10% -> 25% -> 50% -> 100%; appen ska inte krascha.
- [ ] 100% ska ge thumbnails över den synliga canvasen, inte försöka ladda hela grafen utanför viewporten.
- [ ] Läsmarkerade entries ska få thumbnail före olästa när ett nytt område laddas.
- [ ] Panorera långt bort och tillbaka; gamla zonbilder ska kunna evikteras/laddas om utan krasch.
- [ ] Tryck på både en thumbnail och en punkt och kontrollera entry-valet.
- [ ] Tag heatmap, family outline, Reset View och den precalculerade layoutens öar ska vara oförändrade.

## 5. Prenumerationsnotis

- [ ] Tillåt notifications och skapa/behåll minst en subscription.
- [ ] När en ny subscription-event skapar en notis: tryck på notisen.
- [ ] Sauce Tracker ska öppnas direkt på **Subscriptions**.
- [ ] Om app-lock är aktivt ska låsskärmen visas först och navigationen ske efter unlock.
- [ ] Markera/öppna events och kontrollera att badge/notis uppdateras enligt seen/unseen.
- [ ] Testa tillfälligt nätfel under bakgrundsrefresh; kontrollen ska kunna retryas senare utan att unseen-koder förbrukas.

## 6. Rullande backuphistorik

- [ ] Välj procedural backupmapp och kör Backup Now.
- [ ] Kontrollera att `procedural_backup.txt` finns och går att importera.
- [ ] Ändra något, kör backup igen och kontrollera `procedural_backup_previous_1.txt`.
- [ ] Kör en tredje gång och kontrollera `procedural_backup_previous_2.txt`.
- [ ] Kör en fjärde gång: endast Current, Previous 1 och Previous 2 ska vara den rullande datahistoriken.
- [ ] Thumbnail-arkivet ska fortfarande vara en gemensam mapp och inte kopieras tre gånger.
- [ ] Manuell Export ska skriva den valda exportfilen utan att rotera procedural backuphistorik.
- [ ] Avbryt/framkalla ett misslyckat backupskrivförsök om praktiskt möjligt; senaste giltiga Current ska finnas kvar.

## 7. Selected Entry och relaterade entries

- [ ] Öppna en entry som har **Parts** och kontrollera att endast föregående och nästa part visas.
- [ ] Kontrollera att Parts använder samma individuella thumbnailkort som **More like this** och **Same artist**.
- [ ] Kontrollera att ingen stor bakgrundslåda omsluter hela relationssegmentet.
- [ ] Aktivera ett sökord som döljer nästa part och tryck sedan på **Next part**: målposten och dess detaljpanel ska visas medan söktexten ligger kvar.
- [ ] Upprepa med **Show Read**, **Show Unread**, **Show Downloaded** och ett taggfilter som normalt döljer målposten.
- [ ] Gå till en annan yta, exempelvis Artists/Groups, öppna en relaterad entry och kontrollera att appen går till Entries och visar målet.
- [ ] Tryck föregående/nästa flera gånger och kontrollera att samma målpost inte dupliceras i listan.

## 8. Regressionsrunda

- [ ] Importera en ny kod, ändra read/rating/pin och starta om appen.
- [ ] Browser list/detail, duplicate hints och snabbåtgärder fungerar.
- [ ] Slideshow behåller sida, läge, inertia, middle-tap och hold-anywhere.
- [ ] Lokala downloads kan öppnas och tas bort.
- [ ] Tags, creators/groups, suggestions, subscriptions, history och search/filter/sort fungerar.
- [ ] Desktop Bridge kan startas normalt men inte i Library-incognito.
- [ ] Rotation, appbyte och återgång från recents ger ingen tom vy eller krasch.

Rapportera gärna exakt checkbox, vad som visades och om appen stängdes eller bara visade feltext. För en krasch är tidpunkten och den senast tryckta kontrollen viktigast.
