# Weryfikacja 0.1.52 — nachylenie całych liści

Minecraft 1.21.1 / NeoForge 21.1.248 / Java 21 / protokół 28.
**Wersja do testów na kopii świata.** Wcześniejszy nieregularny problem
zapisu stanowiska pozostaje otwarty; kod zapisu nie jest zmieniany.

## Zakres

Użytkownik doprecyzował, że liście mają wyrastać od łodygi pod różnymi
kątami w pionie: część w górę, część niemal poziomo i część lekko w dół.
Zmiana dotyczy całego liścia od nasady. W 0.1.51 różnica wysokości końca
była bardzo mała, mimo zróżnicowanego azymutu i rozmiarów.

Generator nadal używa istniejącego zapisanego UUID. Pierwsze trzy
widoczne liście zawierają trzy rodzaje nachylenia; kolejne dopełniają układ.
Wstępny zakres to około 10–23° w górę/w dół i ±3° dla liści niemal
poziomych. Dolne liście mają ograniczenie opadania, aby zachować zapas nad rantem
doniczki, z uwzględnieniem całej grubości blaszki.
Płytka krzywizna pozostaje nałożona na główne nachylenie.

To parametry stylizowanego modelu gry. Opis i badania botaniczne potwierdzają
zmienność nachylenia konopi, lecz nie ustalają powyższych zakresów jako
uniwersalnej reguły dla każdego liścia:

- [Badanie geometrii i kątów liści, Remote Sensing 16(13), 2375](https://www.mdpi.com/2072-4292/16/13/2375).
- [Materiał identyfikacyjny UF/IFAS ze zdjęciami, ilustracja 1](https://ask.ifas.ufl.edu/publication/AG476/pdf#page=2).

## Brak przecinania

Same pionowe przedziały z 0.1.51 były zbyt ograniczające dla wyraźnie
nachylonych liści. Każdy kandydat ma teraz przestrzenną obwiednię w postaci
pochylonego graniastosłupa, obejmującą cały prostokąt blaszki, jej krzywiznę,
poprzeczny profil i grubość 0,013 bloku. SAT sprawdza osie ścian i iloczyny
wektorowe krawędzi, z marginesem 0,006 bloku.

Przy konflikcie generator dobiera inny azymut kandydata. Ograniczone
powtórzenie z łagodniejszym nachyleniem gwarantuje zakończenie także dla
nietypowo zatłoczonego ziarna. Cały wynik jest ustalany raz przez funkcję
ziarna, niezależnie od liczby pozostałych liści i kolejności cięcia.

## Testy

Niezależny test nie powtarza SAT. Dzieli rzeczywistą powierzchnię na sześć
płaskich płatów zgodnych z siatką renderera, przycina ich rzuty w XZ i
porównuje liniowe wysokości na każdym wierzchołku wspólnego wielokąta.
Uwzględnia obie powierzchnie bryły i dodatkowy odstęp.

Sprawdzono wszystkie pary ośmiu liści dla 2000 roślin, mieszankę trzech
nachyleń już przy trzech liściach, położenie ponad rantem doniczki i poniżej lampy,
trwałość UUID, różnorodność ziaren i zachowanie kształtu przy odtwarzaniu.
Pierwsze siedem testów generatora przeszło także oddzielnie. Po kontroli
rantu wzmocniono limit opuszczenia i test granicy; pełny zestaw końcowy
ponownie przeszedł.
Test braku ostrych załamań porównuje teraz krzywiznę względem nachylenia
całego liścia; kontrola nagłej zmiany lokalnego nachylenia pozostaje.

Pełny zestaw: **266 testów / 43 zestawy, 0 błędów, awarii i pominięć**.
Kod wykonawczy przeszedł kompilację na Java 21. Pierwsze próby odtwarzania
środowiska zatrzymały się na rozwiązywaniu zależności, a pierwsza pełna
kompilacja wykryła uszkodzoną kopię biblioteki Minecrafta w katalogu roboczym.
Przywrócono tę samą wersję z poprawnego pliku, sprawdzono CRC i zgodność
sumy kontrolnej; następna pełna kompilacja i testy przeszły. Nie zmieniano
z tego powodu wersji zależności ani kodu modyfikacji.

Próba `leaves-targeted` (`--harvest-only`): dwa rzeczywiste klienty TCP
oraz osobny serwer. Stan początkowy to dojrzałe rośliny z ustalonym UUID;
operacje zbioru wykonują rzeczywiste okno i pakiety produkcyjne. PASS:

- Identyfikator obserwowany przez drugiego klienta odpowiada temu samemu
  UUID, którego ziarno widzą świat i okno zbioru właściciela.
- Osiem wybieranych liści, osiem nagród i osiem punktów zużycia nożyc;
  ponowione pakiety zakończenia nie dodają nagród. Cięcia nie losują układu.
- Rzeczywisty odcięty obiekt jest przechwycony od pierwszej klatki, przed
  oczekiwaniem na aktualizację ekwipunku. Jego usunięcie spełnia warunek
  opuszczenia dolnej krawędzi przez całą bryłę.
- Obejrzano roślinę w świecie pod UV, okno zbioru, początek lotu oraz
  galerię czterech wariantów. Różnice nachylenia są widoczne.
- Logi stdout i `logs/latest.log` obu klientów nie zawierają błędów OpenGL.
- Pierwszy serwer zakończył pracę z kodem 0. Restart z dysku zachował sprzęt,
  zużytą ziemię oraz UUID, liczbę i maskę liści drugiej, niezbieranej rośliny.

Wcześniejsze, szersze próby nie są zaliczone jako pełny PASS cyklu:

- `inclinations-1`: start obrazu zatrzymany przez brak biblioteki xkbcomp;
  uzupełniono zależność środowiska przed uruchomieniem klientów.
- `inclinations-2`: przerwano podczas kontroli rantu; wzmocniono ograniczenie
  opadania i powtórzono pełne 266 testów.
- `inclinations-3`: przygotowanie i pierwszy zbiór doszły do skutku, lecz
  automat odczytywał listę lotu dopiero po aktualizacji ekwipunku i zapisie
  PNG; lista była już pusta (`NoSuchElementException`). Automat śledzi teraz
  obiekt od pierwszej klatki. Kod animacji produkcyjnej nie był zmieniany.
- `inclinations-4`: obaj klienci zostali rozłączeni przez timeout przed
  sadzeniem; serwer odnotował 32 sekundy opóźnienia. Ograniczono liczbę
  wątków i FPS wyłącznie w środowisku testowym.
- `inclinations-5`: szeroka próba zatrzymała się na sprawdzeniu pauzy worka
  (`Released bag pauses filling without spending material`). Nie zaliczamy
  jej ani nie deklarujemy poprawki tej części. Dla geometrii wyodrębniono
  próbę dojrzałej rośliny, która przeszła w `leaves-targeted`.

Celowy test nie zastępuje pełnej kontroli sadzenia, sklepu i finansów.
Ich dotychczasowe testy jednostkowe pozostają częścią zestawu 266 testów.
Udany restart nie jest naprawą wcześniejszego nieregularnego błędu zapisu.

## Granice

Ochrona przed przecięciem dotyczy liści tej samej stojącej rośliny.
Nakładanie w rzucie na ekran może wystąpić przy rozdzielonych bryłach.
Lot odciętych liści nadal jest sztywną animacją, bez nowych kolizji
z otoczeniem. Zachowano zapamiętany układ w świecie, przygotowaniu,
wybieraniu, cięciu i opadaniu.

Nie zmieniano UV, formatu pakietów ani zapisu, plonu, materiałów, finansów
i zasad nagród. Nie deklarujemy naprawy wcześniejszego nieregularnego
problemu zapisu ani braku wszystkich błędów całej modyfikacji.

## JAR

- `crimeandlaw-0.1.52.jar`, 3983520 bajtów.
- SHA-256 `ddc4e95caad368aeefdb66c128cc81f1ab2eb14b7309a2011ce9065e4c0daa2e`.
- CRC poprawne, brak duplikatów i klas Smoke.
- 265 klas produkcyjnych i 34 zasoby zgodne bajtowo z kompilacją/źródłami.
- Po końcowej próbie nie zmieniano kodu wykonawczego. Doprecyzowano tekst
  wyniku celowego automatu i dokumentację; JAR powstał bez klas automatyzacji.

Dane: [validation-0.1.52.json](validation-0.1.52.json).
