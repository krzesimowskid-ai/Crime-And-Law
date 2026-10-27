# Weryfikacja 0.1.51 — widoczność UV i trwałe warianty roślin

Minecraft 1.21.1 / NeoForge 21.1.248 / Java 21 / protokół 28.
**Wersja testowa na kopię świata.** Znany nieregularny problem zapisu
stanowiska pozostaje otwarty; w tej wersji nie zmieniano implementacji zapisu.

## Zgłoszenie i zmiana

Obejrzano lokalnie dostarczone nagranie 15-13-50 (36,654934 s): odległa
kamera pokazuje zwykłe oświetlenie, podczas gdy UV pojawia się dopiero blisko
stanowiska. Dotychczasowy renderer wybierał osiem lamp z obszaru 3×3 chunków
wokół kamery i odrzucał źródła odległe o ponad około 20,5 bloku.

Nowa wspólna mapa światła obejmuje wszystkie lampy w chunkach załadowanych
przez klienta. Nie wybiera źródeł według kamery ani liczby ośmiu lamp.
Próbkowanie w shaderze korzysta z indeksu rzadkich sekcji 16³ z marginesami
do interpolacji. Pola lamp są przechowywane i przeliczane przy zmianie źródeł,
chunków lub nieprzezroczystości pobliskich bloków. Zmiana samego położenia
kamery nie przebudowuje pola ani nie wygasza jego części. Modele stanowisk
mają odległość renderowania zgodną z ustawieniem terenu.

Zachowano fiolet, kierunek w dół, górną płaszczyznę odcięcia, miękki brzeg
wiązki, zasłanianie przez pełne bloki i próbkę małych modeli przy powierzchni.
Kolor trafia do oświetlenia materiałów przed teksturą i mgłą, bez białej
emisji blokowej i bez nakładki na obraz. Przy wyłączeniu światła shader
zeruje przypięte tekstury, aby nie odwoływać się do zwolnionego atlasu.

Układ ośmiu możliwych liści wynika z dotychczasowego, zapisanego UUID
rośliny. Mieszanie ziarna i losowanie kątów, wysokości, długości oraz
szerokości daje różne, trwałe rośliny. Liście są niemal proste, z płytkim
wygięciem. Rozdzielone pionowe obwiednie utrzymują ich bryły osobno.
Świat i obie minigry używają tego samego wariantu; cięcie nie przestawia
pozostałych liści, a odcięty liść zachowuje właściwą geometrię.
Pakiety przesyłają ziarno kształtu; wymagany protokół 28 po obu stronach.
Plon, koszty, nagrody i format zapisu roślin nie są zmienione.

## Testy jednostkowe

264 testy / 43 zestawy, 0 błędów, awarii i pominięć. Nowe przypadki:

- Wspólne pole dla więcej niż ośmiu rozproszonych źródeł; ujemne współrzędne
  i wysokości; granice sekcji; stabilna kolejność; puste i odległe próbki.
  Niezależna emulacja odczytu spakowanego indeksu i atlasu sprawdza dane GPU.
- Powtarzalność kształtu po odtworzeniu stanu, różnice wymiarów i kątów,
  rozdzielenie brył wszystkich par liści dla 2000 roślin, rozmieszczenie
  pierwszych trzech liści i rozkład kierunków dla kolejnych ziaren.
- Regresja górnej powierzchni liścia pod lampą oraz dotychczasowe testy
  kolizji, sztywnego opadania, przygotowania, nagród i zabezpieczeń.

Pierwsze uruchomienie wykryło brak idempotencji modyfikacji shadera po
zmianie deklaracji uniformów (263 testy, jedna awaria). Poprawiono znacznik
wykrywający już zmodyfikowane źródło; następne pełne testy przeszły.

## Przebiegi rzeczywistej gry

Automat uruchamia dwa klienty TCP i osobny serwer; nie zastępuje ich
jednostkową atrapą. Freecam jest oddzielną encją kamery, a gracz pozostaje
przy stanowisku, aby nie przesuwać obszaru ładowania chunków.

- `persistent-1` i `far-debug-1`: dotychczasowe bliskie próby UV przeszły,
  nowy pomiar odległości zatrzymał się przy czterech blokach. Diagnostyka
  potwierdziła dodatnie światło w polu CPU, ale kamera automatu patrzyła
  w przeciwną stronę: dla LivingEntity wymagany był również obrót głowy.
  Poprawiono wyłącznie ustawienie kamery w automatyzacji.
- `far-debug-2`: pominięto już sprawdzoną długą sekwencję bliskiego UV;
  13 lamp, kamera 4/18/22/40/64, pełne przygotowanie, osiem cięć, kontrole
  serwera, zabezpieczeń i restart z dysku przeszły. Galeria małych ziaren
  ujawniła zbyt podobne początkowe kierunki; dodano mieszanie ziarna i
  mocniejsze zróżnicowanie kąta. Usunięto też podwójny zapis tych samych
  zdjęć freecam, który w tym środowisku dawał puste pliki PNG.
- `persistent-2`: pełna sekwencja bliskiego UV Fancy/Fabulous, odległa kamera,
  13 źródeł i aktualizacja zasłaniania przy kamerze 64 bloki dalej przeszły.
  Przygotowanie, zbiór, kolizje i zabezpieczenia przeszły. Restart z dysku
  **FAIL**: `Reusable equipment and exhausted soil persist`, mimo normalnego
  zakończenia pierwszego serwera (exit 0). Ponowne wystąpienie znanego błędu
  zapisu. Oględziny logu ujawniły odwołania do zwolnionych tekstur przy GUI;
  poprawiono zerowanie samplerów w produkcyjnym rendererze.

`persistent-3`: końcowy, świeży przebieg po poprawce wiązania tekstur.
Wszystkie 15 kontroli PASS: UV Fancy/Fabulous, sufit, usunięcie, ponowne
włożenie i przeładowanie zasobów; freecam 4/18/22/40/64 przy 13 lampach;
zasłonięcie i odsłonięcie bloku przy kamerze 64 bloki dalej; cały cykl,
kolizje, lot, dwie sesje TCP i kontrole nadużyć. Oba logi klientów bez
komunikatów błędów OpenGL. Fiolet na próbce pozostaje widoczny przy 64
blokach (RGB około 34,0 / 27,2 / 57,1); niewielkie zmiany wraz z odległością
wynikają z renderowania terenu i mgły, bez wcześniejszego nagłego zaniku.
Pierwszy serwer exit 0; restart z dysku PASS w tym przebiegu. Wynik ten
nie unieważnia nieudanego restartu `persistent-2`.

Oględziny końcowych zrzutów obejmują odległe stanowisko z widocznym
fioletem, dojrzałą roślinę w świecie oraz cztery wyraźnie różne warianty
liści. Identyfikator kształtu jest sprawdzany pomiędzy światem,
przygotowaniem i zbiorem, także po każdym z ośmiu cięć.

## Granice

Widoczność zależy od załadowania i renderowania terenu oraz mgły Minecrafta.
Nie wymuszamy ładowania chunków dla freecam. Testy obejmują odległości do
64 bloków, a kod pola nie ma odcięcia według odległości kamery. Pamięć i
rozmiar tekstur karty graficznej pozostają praktycznymi granicami skali.
Nie deklarujemy zgodności z każdą zewnętrzną paczką shaderów.

To kolorowa mapa oświetlenia materiałów. Nie zmienia waniliowego poziomu
LightLayer.BLOCK, zachowań innych modów zależnych od tego poziomu ani
rozchodzenia się światła za rogi. Cienie częściowych bloków i nakładanie
wiązek na różnych wysokościach są przybliżone.

Test rozdzielenia dotyczy liści tej samej stojącej rośliny, nie kolizji
odciętych części z otoczeniem. Geometria może nakładać się w rzucie na ekran
bez fizycznego przecięcia brył. Nie jest to symulacja botaniki.

Nieregularny błąd zapisu pozostaje otwarty. Pojedynczy udany restart nie
jest jego naprawą; nie deklarujemy braku wszystkich błędów całego moda.

## JAR

- `crimeandlaw-0.1.51.jar`, 3979457 bajtów.
- SHA-256 `7779dabe7795a8d3ab49a17a8b05e53274c3f5f640a19bddff8b2560e2f36213`.
- CRC poprawne, brak duplikatów i klas automatyzacji Smoke.
- 263 klas produkcyjnych i 34 zasoby zgodne bajtowo z kompilacją/źródłami.
- Końcowe pakowanie bez źródeł automatyzacji. Po `persistent-3` nie zmieniano
  kodu wykonawczego; uzupełniono dokumentację i raport.

Dane: [validation-0.1.51.json](validation-0.1.51.json).
