# Weryfikacja 0.1.45 — UV, sterowanie i modele przygotowania

26 października 2026. Minecraft **1.21.1**, NeoForge **21.1.248**, Java **21**.
Protokół **25** — jednakowy JAR na serwerze i u graczy.

**Status: wersja do testów na osobnej kopii świata.** Poprawki wizualne
oraz sterowanie przeszły opisane poniżej sprawdzenia. Otwarta pozostaje
sporadyczna obserwacja utraty zmian chunka podczas zapisu.

## Zmiany

- Fiolet UV obejmuje podłogę, ściany, sufit, modele i trzymany przedmiot.
  Kolor jest obliczany z pozycji powierzchni w świecie. Pełne bloki
  zatrzymują jego zasięg; HUD zachowuje swoje kolory. Własne programy
  renderujące są wbudowane w mod, bez dodatkowej paczki shaderów.
- A/D przesuwa narzędzie w lewo/prawo względem kamery, W/S w głąb/do siebie.
  LPM chwyta i uruchamia automatyczny przechył do sypania lub podlewania.
  Q/E pozwala korygować przechył. Puszczone narzędzie pozostaje w miejscu.
- Drobniejszy materiał ziemi zastępuje duże kontrastowe pola i wystające
  brązowe kostki. Nasiono ma owalną geometrię, szew i własny materiał.
  Obręcz oraz pionowy wskaźnik ułatwiają celowanie, a nasiono opada do dołka.
  Rękawica ma palce i neutralny kolor.
- Dziewięć strużek powstaje w rzeczywistych otworach sitka konewki.
  Serwer uwzględnia udział trafiających strumieni w postępie podlewania.
- Każdy odcięty liść ma własny kierunek i obrót wokół swojego środka,
  grawitację i kołysanie. Efekt jest kosmetyczny; nagroda nadal powstaje raz.

## Testy jednostkowe

**226 testów w 38 zestawach**, zero niepowodzeń, błędów i pominięć.
Dotychczasowe testy finansów, menu, upraw, map i zbioru pozostają w zestawie.
Nowe sprawdzają:

- Dziewięć różnych otworów sitka i transformowanie ich wraz z konewką.
- Pełne oraz częściowe trafienie w doniczkę, brak postępu po puszczeniu.
- Kierunki klawiszy przy różnych obrotach kamery, stałą wysokość,
  ograniczenie zasięgu i brak przyspieszenia ruchu po przekątnej.
- Zasięg UV na wysokości ścian i sufitu, blokowanie przez ścianę,
  przywrócenie światła po jej usunięciu i skończone wartości pola.
- Interpolację ośmiu sąsiednich komórek oraz odrzucanie nieprawidłowych
  współrzędnych próbkowania.
- Różne kierunki lotu liści, przewagę grawitacji nad początkowym ruchem
  i powtarzalność danych kosmetycznych dla ustalonego ziarna.

## Rzeczywisty klient i dwa połączenia TCP

Serwer dedykowany i dwa osobne klienty wykonały zakupy, montaż ramy,
przygotowanie, podlewanie oraz zbiór przez zdarzenia okien i pakiety sieciowe.
Sprawdzono rzeczywiste naciśnięcia A/D/W/S. Po każdym ruchu porównywano
rzut pozycji narzędzia na ekran z oczekiwanym kierunkiem.

Zrzuty i pomiar koloru fragmentu ściany potwierdzają fiolet w **Fancy**
i **Fabulous**, jego zniknięcie po zdjęciu lampy oraz zachowanie po
przeładowaniu zasobów. Obejrzano także kolor trzymanej konewki i HUD.
Fabulous zachowuje głębię świata w osobnych buforach warstw; renderer
uwzględnia te dane przy obliczaniu UV. Kontrola pikseli ściany wykryła
wcześniejszy wariant, który barwił tylko przedmiot w ręce.

W przebiegu sprawdzono również:

- Zakupy **2000 → 746**, jeden wydatek po ponowieniu pakietu.
- Duży worek **3 → 2** po napełnieniu; pauzę po puszczeniu i ponowny chwyt.
- Otwarcie woreczka, nietrafione nasiono i ponowną próbę bez kosztu,
  prawidłowe opadnięcie oraz ręczne zasypanie. Pozostały cztery nasiona.
- Konewkę **400 → 300** i ruch wszystkich emiterów z sitkiem.
- Osiem cięć w kolejności **7, 2, 5, 0, 6, 1, 4, 3**, osiem nagród
  i osiem punktów zużycia. Obejrzano klatki animacji po cięciach.
- Rezerwację stanowiska, odzyskanie zgłoszenia po trzech sekundach,
  podmianę źródła, powtórzenia FINISH, pomijanie etapów, stosy worków,
  pełny ekwipunek, ochronę miejsca, ścianę i zasięg.
- Zakupy gotówkowe bez pobierania banku oraz niszczenie i eksplozję ramy
  z jednokrotnym oddaniem wyposażenia.

Okna przygotowania sprawdzono w **1920×1080 / GUI 2 i 4**, **855×481 / GUI 2**
oraz **640×480 / GUI 2**. Galeria sprzętu przeszła rzeczywisty render klienta.
Materiały ziemi, rękawicy i nasiona są własnymi paletami, bez tekstur bloków.

Czas dojrzewania w scenariuszu sieciowym przesunięto do końca; pełen cykl
wzrostu i ekspozycję obejmują testy jednostkowe.

## Zapis świata — otwarta obserwacja

Jedna próba (`final-tcp`) nie odtworzyła stanowisk po restarcie. W migawce
świata wykonanej po zatrzymaniu pierwszego procesu, przed ponownym
uruchomieniem, chunk **0,0** miał starszy stan (`LastUpdate=412`) bez
BlockEntity doniczek. Podobną nierozstrzygniętą obserwację odnotowano w 0.1.44.

Dodano diagnostykę w opcjonalnym scenariuszu: zdarzenia zapisu, osobne
migawki NBT stanu w pamięci i obiektu używanego przez ChunkHolder,
odczyt przez pracownika magazynu oraz kod zakończenia procesu.

**Dwie kolejne próby z diagnostyką przeszły** (`persistence-diagnostic`
i `persistence-repeat`). Proces serwera kończył się kodem **0**. W obu
przypadkach stan w pamięci, dane przekazane do magazynu i późniejszy odczyt
z dysku zawierały trzy BlockEntity. Zachowały się:

- Pierwszy stelaż, doniczka i UV, bez zużytego podłoża i rośliny.
- Druga roślina: plon 6, maska 47 i ten sam UUID.
- Ekwipunek: osiem liści, cztery nasiona, 300 wody, osiem zużycia nożyc,
  brak dużego worka; gotówka 746.

**Przyczyna pozostaje nieustalona.** Obserwacja pozostaje otwarta pomimo
udanych kolejnych prób. Produkcyjny kod zapisu upraw nie otrzymał
spekulacyjnej poprawki. Do testowania tego wydania używaj kopii świata.

## Zakres i ograniczenia

UV ma zasięg około **6 bloków** i budżet **8 najbliższych lamp**. Pole
przesłaniania ma 13³ komórek na źródło i uwzględnia pełne bloki kolizji;
częściowe kształty nie rzucają precyzyjnych cieni. Wygaszanie i mieszanie
koloru są efektem renderowania; logiczny kanał światła Minecrafta pozostaje
biały. Nie wykonano pomiaru FPS dużej farmy ani porównania z zewnętrznymi
paczkami shaderów. Testowano zwykłe zamknięcie procesu i restart, bez
symulowanego odcięcia zasilania między zapisami plików świata.

## Archiwum i odtworzenie

Dane archiwum, SHA-256, rozmiar, kontrola CRC, porównanie klas i zasobów
oraz wyniki scenariuszy: [validation-0.1.45.json](validation-0.1.45.json).

`./gradlew -PgardenSmoke compileJava processResources prepareClientRun prepareServerRun`
kompiluje opcjonalne scenariusze. `tools/garden_smoke.py --output nowa-nazwa`
uruchamia pełny przebieg w świeżym katalogu. Java 21, lokalne Xvfb i biblioteki
wskazuje `CRIMEANDLAW_BUILD_TOOLS`. `--visual` uruchamia galerię.
Klasy scenariuszy są wyłączone z JAR wydania.
