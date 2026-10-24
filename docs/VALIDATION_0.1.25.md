# Walidacja 0.1.25 — 24 października 2026

Wynik: **PASS**. Minecraft 1.21.1, NeoForge 21.1.248, Java 21.0.2, Gradle 9.2.1.

## Automatyczne sprawdzenia

- `clean build`: sukces; 120 testów JUnit, zero błędów i niepowodzeń.
- Nowe testy obejmują obecność, pauzę/anulowanie, przejście do PvP, pełne
  przygotowanie obrońcy, śmierć i zakaz powrotu, trzy gangi, remis, minimum
  startowe i ocalałych, zmiany gangu/wymiaru, ochronę, cooldown, restart,
  błędny zapis, strefy czasowe, zmianę czasu letniego i pełną dobę słońca.
- Oznaczenia: oddzielenie prywatnych i gangowych, brak przesyłania obcym,
  niezależność wymiaru, zakres widoku, limit i ponowne kolorowanie, zapis.
- Własność: sąsiedztwo bokiem, pierwszy teren w osobnym wymiarze i zwalnianie
  własności po rozwiązaniu gangu, z zachowaniem pierwotnego opisu terenu.

## Rzeczywisty serwer i klienci

Uruchomiono odizolowany dedykowany serwer. Część serwerowa sprawdziła
komendy konsoli i odmowę nieuprawnionemu graczowi, rzeczywisty zegar
świata oraz przywrócenie gamerule po wyłączeniu synchronizacji.
Następnie przeprowadzono neutralne przejęcie, PvP, eliminację i ochronę.

Identyczne itemy leżące obok oznaczonego łupu pozostały nietknięte;
sprawdzono rzeczywiste ticki łączenia stosów i działający mixin.
Pełny ekwipunek, także creative, zachowuje depozyt. Jeden wolny element
pojemności pozwolił odebrać jedną sztukę; reszta pozostała do odbioru.
Sprawdzono zapis stosów w rzeczywistym NBT.

Dwa osobne klienty Minecrafta połączyły się przez TCP z serwerem:

- Właściciel dodał prywatny znacznik przez rzeczywiste koło kolorów,
  aktywował kontrolę i rozpoczął przejęcie przyciskiem z potwierdzeniem.
- Drugi gang nie otrzymał prywatnego znacznika. Aktywował własną kontrolę
  i dołączył do wydarzenia przyciskiem z mapy. Serwer przekazał listę uczestników.
- Sprawdzono widok przygotowania, HUD walki oraz kolorowe oznaczenie rywala.
  Rywal umarł w rzeczywistej grze, odrodził się i pozostał wyeliminowany.
- Zwycięzca zobaczył odświeżoną własność chunka. Jedenaście diamentów
  z faktycznej śmierci rywala trafiło do depozytu. Komenda odbioru przekazała
  dokładnie 11 do ekwipunku, pozostawiając pusty depozyt.

Po zatrzymaniu i ponownym uruchomieniu tego samego serwera sprawdzono
zapis na dysku: własność, ochronę, cooldown, siedem pozostawionych łupów,
prywatne/gangowe oznaczenia i strefę czasu. Niedokończony capt został
anulowany bez przyznania terenu; obowiązywał cooldown po przerwaniu.

Fixture testowe znajdują się w `src/captureTest/java`; włącza je
`-PcaptureSmoke`. Zadania: `runCaptureServer`, `runCaptureClient`
(z `-PcaptureRole=owner` lub `rival`) oraz `-PcaptureRestart=true` dla
ponownego uruchomienia. Serwer testowy korzysta z lokalnego portu 25596,
trybu offline i własnego katalogu `run-capture-server`; te ustawienia
służą wyłącznie próbie. Raporty powstają w `build/capture-smoke`.

Render sprawdzono na Linux/Mesa/Xvfb w rozdzielczości 1600×900.
Narzędzie budowania NeoFormRuntime wymagało w tym kontenerze lokalnego
wskazania ścieżki Javy; ta zmiana narzędzia nie jest częścią moda.

## Pakiet wydania

- Plik: `crimeandlaw-0.1.25.jar`, 1914690 bajtów.
- SHA-256: `819e8e4f60aa147b2b30f5654249014eb711d2b877c0ed740c556dd2cf6f69d3`.
- 121 klas moda i 15 dołączonych bibliotek; CRC wszystkich archiwów poprawne.
- Metadane 0.1.25 i konfiguracja mixinu obecne; klasy automatyzacji smoke nie są pakowane.

Aktywacja kontroli jest obecnie bezpłatna. Łupy korzystają z depozytu
odbieranego komendą, do czasu podłączenia fizycznego magazynu. Dolary,
poziomy gangów i bonusy produkcyjne pozostają do wdrożenia.
