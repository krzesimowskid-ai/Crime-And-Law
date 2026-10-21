# Sprawdzenie 0.1.15

21 października 2026. Minecraft 1.21.1, NeoForge 21.1.248, JDK 21.

## Zmiana

Zastąpiono jednolite kolory chunków ciągłą mapą aktualnej powierzchni.
Każda kolumna bloków ma osobny piksel w palecie map Minecrafta; wysokość
sąsiednich kolumn i głębokość wody wpływają na cieniowanie. Widok 11×11
chunków, przybliżanie do 5×5, opcjonalna siatka, pozycja/kierunek gracza,
północ, podziałka oraz wybór chunka z przewijanym opisem.

Rysunek aktualizuje się niezależnie od stałych profili generowania, jest
zapisywany ze światem i może pokazywać budowle graczy bez przyznawania
im cech wygenerowanych budynków. Brakujące dane są zasłonięte.
Usunięto automatyczne opisy potencjalnych korzyści przy każdej cesze;
API opisuje cechy posiadanego terenu. Nie ma automatycznych boosterów.
Konkretne rzadkie premie, ich krytyczne warunki i wartości są do ustalenia.

## Testy jednostkowe

69 testów JUnit, bez błędów, niepowodzeń i pominięć.
Nowe sprawdzenia: 256 pikseli na chunk, niezmienność danych przez tablice,
odrzucenie niepełnych/uszkodzonych kafli, mapa niezależna od profilu cech,
dokładna pozycja gracza, zapis i odczyt map dla wymiaru/chunka, odświeżenie
rysunku bez zmiany cech, migracja zapisów 0.1.14 bez rastrów.

Pełna mapa 121 chunków, z rastrami, wszystkimi opisowymi cechami i właścicielami,
przechodzi osobny kodek sieciowy w założonym budżecie, bez przesyłania logo.
Zachowano wcześniejsze testy rang, członkostwa, rejestru, kolorów i obrazów.

## Rzeczywisty serwer dedykowany

CRIME_TERRAIN_SERVER_PASS: FIRST_RUN i RESTART tego samego izolowanego świata
`terrain15a`, ziarno 123456789, warsztat w chunku (-17, 12), Overworld.

- Rzeczywista wygenerowana wioska i szablon warsztatu rozpoznane.
- Pierwotny profil i zapis źródłowy zachowane mimo zmian bloków.
- Rekonstrukcja starego chunka wyklucza wysokie konstrukcje gracza.
- Odczyt powierzchni objął co najmniej 49 chunków prawdziwego świata.
- Poszczególne kolory bloków, także poza dawną siatką 16 próbek, zachowane.
- Oba brzegi granicy chunków zachowują właściwe piksele.
- Zamiana bloku zmienia raster przy odświeżeniu, bez zmiany cech naturalnych.
- Raster, zaktualizowany piksel, profile i dane źródłowe przetrwały prawdziwy
  restart; zapis mapy sprawdzono przed ponownym skanowaniem.
- Zapis SavedData NBT zachowuje pierwotny profil i lokalne przypisanie własności.

Test utrzymuje okolice wsi w pamięci biletami wyłącznie w świecie testowym.
Produkcja nie dodaje biletów i nie generuje odległych chunków dla mapy.
W świecie testowym stoją bloki kontrolne użyte do sprawdzenia kolorów.

## Rzeczywisty renderer Minecrafta

CRIME_UI_SMOKE_PASS, 33 zrzuty 1920×1080, skale GUI 2 i 4.
Sprawdzono skalowany wybór chunków, niezależne przewijanie opisu,
nieznane obszary, zmianę przybliżenia, przełącznik siatki i skalowanie GUI.

Dwa ostatnie zrzuty pokazują dane powierzchni odczytane z rzeczywistego
świata powyższego serwera; wcześniejsze próby interakcji korzystają też
z syntetycznego testowego przebiegu rzeki. Każdy nieprzezroczysty piksel
rastrów świata porównano z gotową teksturą klienta: pozycja, orientacja,
kanały barw i kolor są zachowane. Wizualnie sprawdzono mapę wsi/rzeki
oraz powiększony widok z siatką. Zachowano próby rang, dat, rejestru,
kolorów oraz importu/kadrowania logo.

To test renderera z zapisanymi danymi serwera, a nie rozgrywka dwóch klientów
połączonych przez TCP. Nie twierdzić, że odbyła się taka rozgrywka.

## Budowanie

`./gradlew build --rerun-tasks`
`./gradlew -PserverSmoke -PserverSmokeId=terrain15a runServer` — dwa starty.
`./gradlew -PuiSmoke runClient` — po teście serwera, używa jego view.json.

Test serwera wymaga zaakceptowania EULA w izolowanym katalogu
`run-server-smoke-terrain15a`. JAR produkcyjny nie zawiera klas automatyzacji
serwera/klienta. Zawiera 15 dotychczasowych bibliotek importu logo.
Wersja 0.1.15 i protokół 7: aktualizacja klienta oraz serwera wymagana.

## Granice sprawdzenia

Mapa jest lokalnym widokiem 176×176 bloków, nie przeglądem całego świata.
Brak przesuwania i osobistej mgły odkrycia. Dane kartograficzne zapisują
się wspólnie na serwerze, oddzielnie od naturalnych cech i od własności.
Paleta map pokazuje barwy materiałów; nie renderuje tekstur bloków ani
wnętrza podziemnych budynków. Przetestowano powierzchnię Overworld, nie
czytelność mapy pod sufitem Netheru.

Rysowanie: najwyżej 128 kolumn na tick, porcje po 32 i miękki budżet 2 ms.
Odświeżenie chunka najwcześniej po 100 tickach. Kolejka najwyżej 256 zadań;
nieodświeżane zadania wygasają po 200 tickach. Praca odbywa się tylko na
chunkach już dostępnych jako FULL. Klient używa jednej tekstury, zmienianej
po aktualizacji pikseli. Nie wykonano benchmarku dużego serwera.

Analiza stałych cech nadal używa 16 próbek i nazw szablonów vanilla.
To przybliżony opis, nie szczegółowy silnik kwalifikacji rzadkich premii.
Nie dodano przejmowania, receptur, przedmiotów ani aktywnych mnożników.
Dotychczasowe ograniczenia importu logo pozostają jak w README.
Lokalna poprawka narzędzia NeoForm Runtime nie trafia do źródeł ani JAR-a.
