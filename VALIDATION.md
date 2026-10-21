# Sprawdzenie 0.1.14

21 października 2026. Minecraft 1.21.1, NeoForge 21.1.248, JDK 21.

## Zakres

Fundament stałych cech terenów: dane źródłowe nowych chunków, rekonstrukcja
starszych, wygenerowane budynki wiosek, trwałe profile oraz lokalne API
własności/możliwości. Mapa 7×7 wokół gracza, wybór chunka i przewijany opis.
Nie dodano zakupu/przejmowania, aktywnych mnożników, receptur ani przedmiotów.

Cechy terenu pochodzą z 16 próbek i biomów; obiekty z danych wygenerowanych
struktur oraz nazw szablonów vanilla. Każdy profil zostaje niezmienny.
Starsze chunki bez źródła mają jawnie oznaczoną rekonstrukcję z generatora:
nie odtwarza ona dokładnie wszystkich historycznych dekoracji powierzchni.

## Testy JUnit

64 testy, bez błędów, niepowodzeń i pominięć.
Nowe sprawdzenia obejmują teren i struktury jako niezależne źródła cech,
wpływ rzeźby/wody, stałość profilu po zmianach i zapisie, własność w dokładnym
chunku i wymiarze, ujemne współrzędne, fragmenty wsi na granicy chunków,
mapę przy granicy świata, nieprawidłowe zapisy i niemodyfikowalne kolekcje.
Osobny kodek przesyła ograniczone podsumowanie bez logo i odrzuca za duże dane.
Wcześniejsze testy rang, członkostwa, rejestru, kolorów oraz importu logo przechodzą.

## Prawdziwy serwer dedykowany

CRIME_TERRAIN_SERVER_PASS, świeży świat i ponowny start tego samego świata.
Ziarno testowe 123456789, wygenerowany warsztat w chunku (-17, 12) Overworld.

- Odczyt prawdziwej wioski i szablonu warsztatu, nie danych przykładowych.
- Zapis źródłowy pomiarów powstaje przy generowaniu chunka.
- Postawienie bloków przed analizą nie zmienia pierwotnych pomiarów.
- Chunk bez zapisu źródłowego otrzymuje rekonstrukcję generatora; wysokie
  konstrukcje postawione przez gracza nie zmieniają odtworzonych wysokości.
- Zapis źródłowy i profil przetrwały zamknięcie oraz ponowny start serwera.
- Własność udostępnia możliwości lokalnie; sąsiedni chunk ich nie otrzymuje.
- Rzeczywisty zapis SavedData do NBT i ponowny odczyt zachowują profil.

Test działa bez graczy w izolowanym świecie; utrzymuje wczytaną wioskę
biletami testowymi. Bilety nie są dodawane przez produkcyjną analizę mapy.
Nie jest to test rozgrywki dwóch klientów. Test odtwarza stary chunk przez
usunięcie wyłącznie testowego zapisu źródłowego w świeżym świecie.

## Interfejs Minecrafta

CRIME_UI_SMOKE_PASS: rzeczywisty renderer, 30 zrzutów 1920×1080, GUI 2 i 4.
Mapa używa danych przykładowych w tym teście. Sprawdzono skalowany wybór
chunka, przewijanie długiego opisu, zachowanie wyboru po zmianie skali
oraz wybór pola bez danych. Zachowano sprawdzenia rang, kolorów, rejestru
oraz logo. To nie są zrzuty mapy z wymienionego wyżej świata serwera.

## Wydanie i ograniczenia

`./gradlew build --rerun-tasks`, `./gradlew -PuiSmoke runClient`,
`./gradlew -PserverSmoke -PserverSmokeId=terrain14a runServer` (dwa starty).
Produkcja wyklucza klasy obu testów graficznych/serwera, zawiera 15 bibliotek
odczytu logo i ma protokół 6. Należy zaktualizować klienta oraz serwer.

Analiza mapy nie generuje odległych chunków. Czeka na dostępność początku
struktury zamiast utrwalać niepełną listę budynków. Maksymalnie dwa kroki
na tick i miękki limit 2 ms; pojedynczy krok generatora nie jest przerywany.
Brak benchmarku FPS/TPS dla dużego serwera. Reguły rozpoznawania są prototypem;
nie stanowią ustalonego balansu przyszłych bonusów.

Dotychczasowe ograniczenia importu logo pozostają: wspierane formaty opisano
w README, HEIC/AVIF/PDF i animowane logo nie są obsługiwane. Zapis PNG 512×512
do 1,1 MB i filtr liniowy pozostają bez zmian.
Lokalny fallback narzędzia NeoForm Runtime nie trafia do repozytorium ani JAR-a.
