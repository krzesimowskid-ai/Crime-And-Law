# Sprawdzenie 0.1.13

21 października 2026. Minecraft 1.21.1, NeoForge 21.1.248, JDK 21.

## Zmiana

„+ Nowa ranga” otwiera osobne okno z nazwą, czterema uprawnieniami,
Anuluj oraz Zapisz rangę. Pusta i jednoliterowa nazwa nie pozwala zapisać;
reguły nazwy i uprawnienia nadal zatwierdza serwer. Szkic przetrwa zmianę
skali GUI. Anuluj, X i Esc nie wysyłają operacji utworzenia rangi.
Edycja istniejących rang i przeciąganie kolejności pozostają dostępne.

Przycisk zamykania ma 20×20 jednostek, wyśrodkowany X z dwiema równymi
przekątnymi; jest wspólny dla panelu i okien dialogowych. Usunięto instrukcje
o przeciąganiu/kolejności rang oraz o podglądzie i wyborze odbiorcy wyglądu.
Opis wspólnej zmiany logo pozostał. Ustawienia wyglądu zajmują mniej miejsca.

Rejestr nie tworzy wpisów po zmianach motywu, logo, definicji rang i ich
kolejności. Zachowano zdarzenia członkostwa, przywództwa i przypisania rang.
Stare wpisy ze świata są zachowane. Pełna polska data zawiera rok i godzinę,
np. „21 października 2026 o godzinie 18:15”, według lokalnej strefy gracza,
w osobnym wierszu nad opisem. Nadal 100 wpisów na stronę, maksymalnie 100 stron,
przewijanie pionowe i stały nagłówek. Magazyn i pojazdy nie są implementowane
w tej zmianie; transport nadal pozostaje ukrytą definicją.

## Testy automatyczne

55 testów JUnit, zero niepowodzeń, błędów i pominięć.

- Zmiany konfiguracji nie zwiększają rejestru, natomiast dołączenie,
  przypisanie rangi i usunięcie członka dodają właściwe zdarzenia.
- Limit, zapis i strony rejestru sprawdzono na rzeczywistych operacjach
  członkostwa, zamiast na zmianach koloru.
- Zachowano testy serwerowych uprawnień, chronionego Lidera, przypisań,
  kolejności rang, migracji zapisów i prywatności zgłoszeń.
- Nadal przechodzą testy importu i detali logo 512×512, kodeków sieciowych,
  limitów, członkostwa bez limitu, kolorów i układu.

## Klient graficzny

CRIME_UI_SMOKE_PASS: rzeczywisty renderer Minecrafta, 27 zrzutów 1920×1080,
GUI 2 i 4, Mesa llvmpipe. Zrzuty nowej rangi, rejestru i wyglądu obejrzano.

Sprawdzono otwarcie osobnego okna, wpisanie nazwy, wybór uprawnień,
przycisk zapisu i powrót do rang. Sprawdzono zachowanie szkicu po zmianie
skali, anulowanie przez Esc/X/Anuluj, pełną polską datę, brak wskazanych
instrukcji i geometrię X w panelu oraz oknach koloru i rangi. Sprawdzono
przeciąganie rang, zwykłego członka, delegowane uprawnienia, paginację,
przewijanie rejestru, kolory, import logo i kadrowanie.

Potwierdzenia operacji rang w tym teście pochodzą z danych przykładowych
przez rzeczywiste metody modelu; nie jest to gra dwóch klientów na serwerze.

## Budowanie i ograniczenia

`./gradlew build --rerun-tasks` oraz `./gradlew -PuiSmoke runClient`.
Protokół pozostaje 5. Aby część serwerowa nie zapisywała zmian konfiguracji
w rejestrze, należy zaktualizować również serwer do 0.1.13.
Kod testu graficznego nie jest dołączany do produkcyjnego JAR-a.
Test kodeków nie jest testem sieci TCP. Fizyczne okno wyboru pliku w Windows
ani przeciąganie z Eksploratora nie były automatyzowane.

Źródła logo: PNG, JPEG, GIF, BMP, WebP, SVG, TIFF, ICO, ICNS, PSD, TGA, PCX i PNM.
Limit źródła rastrowego 64 MB, 100 000 px na bok i miliard pikseli, podgląd
do 1024 px; zapis PNG 512×512 do 1,1 MB, filtrowanie liniowe.
SVG do 8 MB bez zewnętrznych zasobów. HEIC/AVIF/PDF i animowane logo
pozostają nieobsługiwane. GIF używa pierwszej klatki.

Lokalny fallback java.home/bin/java w NeoForm Runtime pozostaje narzędziem
środowiska; nie trafia do JAR-a ani repozytorium. Niedostępność usług kont
Minecraft nie blokuje testu z danymi przykładowymi.
