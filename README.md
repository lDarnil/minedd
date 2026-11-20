# Random Events (Fabric, Minecraft 26.1.x)

La fiecare 5 minute (configurabil) **fiecare jucător** primește propriul eveniment aleatoriu, pozitiv (50%) sau negativ (50%), din 30 de evenimente. Evenimentele nu sunt sincronizate între jucători.

Țintă: **Minecraft 26.1.2** (ultimul patch 26.1; `fabric.mod.json` cere `~26.1.2`), Fabric Loader ≥ 0.20.1, Fabric API 0.155.3+26.1.2, Java 25.

> ⚠️ **STARE: necompilat și netestat.** Codul a fost scris și verificat contra surselor Fabric API 26.1.2, dar NU a fost compilat și NU a rulat în joc (vezi `VERIFICARE.md`). Primul pas obligatoriu este `./gradlew build` pe calculatorul tău; dacă apar erori, trimite-mi-le și le repar.

## 1. Instalare și compilare (pe calculatorul tău)

1. Instalează **JDK 25** (ex. Eclipse Temurin 25, https://adoptium.net). Verifică: `java -version` → 25.
2. **Nu trebuie să instalezi Gradle**: proiectul include Gradle Wrapper (Gradle 9.8.2, descărcat automat).
3. În folderul proiectului:
   - Windows: `gradlew.bat build`
   - Linux/macOS: `./gradlew build`
   Prima rulare descarcă Minecraft, Fabric Loom și Fabric API (necesită internet, câteva minute).
4. Rezultat: `build/libs/random_events-1.0.0.jar` (nu folosi `-sources.jar`).
5. Pentru test rapid în dezvoltare: `./gradlew runClient` / `./gradlew runServer`.

## 2. Instalare în joc (singleplayer / client)

1. Instalează Minecraft **26.1.2** și **Fabric Loader** (https://fabricmc.net/use/installer/).
2. Pune în `.minecraft/mods`: **Fabric API 0.155.3+26.1.2** și `random_events-1.0.0.jar`.
3. Pornește profilul Fabric.

## 3. Server multiplayer

1. Server Fabric 26.1.2 + JDK 25 pe server.
2. În `mods/` pune Fabric API și `random_events-1.0.0.jar`.
3. Jucătorii au nevoie de mod și de Fabric API pe client pentru HUD. Cine nu are modul poate intra pe server, dar nu primește HUD-ul (evenimentele funcționează oricum, mesajele din chat de asemenea). Dacă un client refuză conectarea fără mod, instalează-l.
4. Config: `config/random_events.json` (creat la prima pornire). Comenzile administrative cer operator (gamemaster) sau nodul de permisiune `random_events.admin` (ex. LuckPerms).

## 4. Comenzi

| Comandă | Efect | Permisiune |
|---|---|---|
| `/event start` | pornește sistemul | admin |
| `/event stop` | oprește sistemul și termină evenimentele active | admin |
| `/event next` | declanșează un eveniment aleatoriu pentru tine | admin |
| `/event positive` | declanșează un eveniment pozitiv | admin |
| `/event negative` | declanșează un eveniment negativ | admin |
| `/event reload` | reîncarcă configurația | admin |
| `/event history` | ultimele evenimente + cele active (ale tale) | toți jucătorii |

## 5. Configurare (`config/random_events.json`)

`intervalSeconds` (300), `positiveChancePercent` (50), `autoStart`, `hudEnabled`, `messagesEnabled`, `maxHistoryEntries` (10), și pentru fiecare eveniment `enabled` + `durationSeconds`. Durata 0 = eveniment instant.

## 6. Cele 30 de evenimente

Durata implicită între paranteze. Toate sunt implementate prin cod, nu doar mesaje.

### Pozitive
1. **Lucky Day** (instant): 4 tipuri diferite de obiecte valoroase aleatorii (diamant, smarald, aur, fier, măr auriu, ender pearl, XP bottle, carne).
2. **Regeneration** (45s): Regeneration II.
3. **Super Speed** (60s): Speed III.
4. **Jump Boost** (60s): Jump Boost III.
5. **Double XP** (90s): orice câștig de XP este dublat (detectat prin creșterea XP-ului total, la fiecare 2 ticks).
6. **Treasure Rain** (10s): ~10 obiecte (pâine, carne, fier, aur, săgeți, torțe, cărbune, XP bottle) cad lângă tine, unul la 0,5s.
7. **Full Health** (instant): viață și foame la maxim.
8. **Haste** (60s): Haste III.
9. **Night Vision** (120s).
10. **Diamond Gift** (instant): 3-6 diamante.
11. **Resistance** (60s): Resistance II.
12. **Hero Power** (90s): Speed II, Jump II, Strength II, Regeneration II, Haste II, Resistance I, Absorption II.
13. **Friendly Animals** (instant): 5 animale pasive (vacă, oaie, porc, găină) la 3-7 blocuri.
14. **Golden Touch** (instant): 8-16 lingouri de aur.
15. **Peaceful Moment** (30s): monștrii ostili la 48 de blocuri dispar, apoi zona de 32 de blocuri este curățată în fiecare secundă.

### Negative
1. **Darkness** (60s).
2. **Slowness** (45s): Slowness II.
3. **Weakness** (60s): Weakness II.
4. **Hunger** (30s): −8 sațietate + efect Hunger II.
5. **Thunderstorm** (75s): `/weather thunder` pentru durata dată. ⚠️ Vremea este **globală** în Minecraft: furtuna se vede de toți jucătorii (limitare a jocului).
6. **Blindness** (30s).
7. **Mining Fatigue** (60s): Mining Fatigue II.
8. **Mob Invasion** (instant): 6 monștri (zombie, păianjen, creeper, schelet cu arc) la 6-11 blocuri, care te țintesc.
9. **Poison** (30s): nu poate omorî.
10. **Gravity Trouble** (45s): săritura scade de la 0,42 la 0,20 (nu mai poți urca un bloc sărind). Modificator de atribut, eliminat la final.
11. **Lost Items** (45s): inventarul nu poate fi modificat (click-uri, aruncare, schimb mână). Nimic nu se șterge sau pierde. Implementat cu mixin (vezi `VERIFICARE.md`).
12. **Cursed Night** (90s): monștrii ostili la 40 de blocuri primesc Speed II + Strength II (reînnoit la 2s).
13. **Firestorm** (30s): particule de foc în jur; jucătorul arde 2s la fiecare 8s. Nu se plasează blocuri de foc, deci construcțiile nu ard.
14. **Bad Luck** (30s): 3 efecte negative diferite, aleatorii (din 7 posibile).
15. **Wither Curse** (30s): Wither I. ⚠️ Poate fi letal; scade durata în config dacă e prea dur.

## 7. Reguli de sistem (cum sunt implementate)

- **Cronometru individual** per jucător, pornit la prima intrare, continuă la schimbarea dimensiunii și la moarte (starea e legată de UUID, nu de obiectul jucător).
- **Deconectare:** cronometrul și evenimentele active (cu timpul rămas) se salvează în datele jucătorului (Fabric Data Attachment API) și se restaurează la reconectare. Se salvează și la autosave-ul lumii.
- **Moarte:** evenimentele active se termină (ca poțiunile vanilla), cronometrul NU se resetează.
- **Fără repetare consecutivă** pentru același jucător. Șansa fiecărui eveniment este egală în categoria lui; categoriile sunt 50/50 (configurabil).
- **La oprirea serverului:** totul se salvează, efectele tranzitorii se anulează, starea statică se șterge (fără scurgeri între lumi).
- **Server autoritar:** clientul doar afișează ce primește (un snapshot pe secundă + un pachet la declanșare).

## 8. Structură

`RandomEventsMod` (init) · `event/` EventManager, EventScheduler, EventRegistry, PositiveEvents, NegativeEvents, PlayerEventData, EventHistory, EventBuilder, Fx · `config/EventConfig` · `command/EventCommands` · `network/NetworkHandler` + payload-uri · `client/EventHUD` (+ `ClientEventState`, `RandomEventsClient`) · `mixin/ServerGamePacketListenerImplMixin`.

**Eveniment nou:** adaugă un `EventRegistry.register(EventBuilder.positive(...)...build())` în `PositiveEvents` sau `NegativeEvents`. Apare automat în config, istoric, HUD și selecție.

## 9. Compilare online (fără să instalezi JDK)

1. Creează un repository gol pe GitHub și urcă tot conținutul proiectului (inclusiv folderul `.github`).
2. Deschide tab-ul **Actions** → rulează „Build Random Events" (pornește singur la fiecare push).
3. La final, descarcă artefactul `random_events-jar` (conține `random_events-1.0.0.jar`).
4. Dacă build-ul eșuează, deschide jurnalul, copiază erorile de la pasul „Build" și trimite-le mai departe pentru corectare.
