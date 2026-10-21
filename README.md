# Crime And Law

Autorski mod gangsterski: panel gangów, członkostwo i własny wygląd menu.
Wersja **0.1.11**.

## Uruchomienie

- Minecraft **1.21.1**, Java **21**, NeoForge **21.1.248**.
- Włóż `crimeandlaw-0.1.11.jar` do `mods`, zastępując poprzedni JAR tego moda.
- W rozgrywce sieciowej mod musi być **u graczy i na serwerze**.
  Zaktualizuj serwer i wszystkich klientów do 0.1.11: większe logo używa protokołu 4.
- W świecie jednoosobowym część serwerowa działa razem ze światem.
- **G** otwiera panel; skrót można zmienić w sterowaniu. Esc lub X zamyka okno.

## Panel gangu

Bez gangu: dwie równe zakładki u góry — „Stwórz gang” i „Znajdź gang”.
Możesz ustawić nazwę i opcjonalne logo albo wysłać zgłoszenie do istniejącego gangu.
Zgłoszenie przyjmuje jego lider.

Po dołączeniu: kompaktowy nagłówek, zakładki „Gang”, „Członkowie”, „Rejestr” i „Mapa”.
Główna zakładka ma wyśrodkowane logo i nazwę nad linią obejmującą całą szerokość
obszaru treści. Poniżej „Informacje o gangu” i trzy równomierne kolumny:
liczba członków, lider oraz miejsce na poziom gangu. Członkowie nie mają limitu.
Pole poziomu pokazuje „— / Wkrótce”; system zdobywania poziomów jest do ustalenia.
„Ustawienia” znajdują się na dole nawigacji, nad „Opuść gang”.
Mapa pozostaje podstawą przyszłej kontroli terytorialnej.
Rejestr pokazuje rzeczywiste wydarzenia: utworzenie gangu, dołączenia, odejścia,
usunięcia członków, przekazanie przywództwa oraz zmiany wspólnego motywu/logo.
Wpisy powstają na serwerze i zapisują się ze światem; ostatnie 10 000 wydarzeń,
po 100 na stronę, od najnowszego. Wpisy na bieżącej stronie przewija się
pionowo kółkiem lub paskiem po prawej; nagłówek i numer/przyciski stron pozostają
nieruchome. Pasek obsługuje strzałki, Page Up/Down i Home/End po zaznaczeniu.
Zmiana strony ustawia listę na początku. Krótka lub pusta strona nie przewija się.
Długie wpisy zawijają się, a zmiana skali GUI zachowuje pozycję w granicach listy. Nie ujawnia nazw osób z prywatnych zgłoszeń.
Starsze zapisy zaczynają z pustym rejestrem; wydarzenia nie są odtwarzane wstecz.

Transport ma wyłącznie ukrytą definicję kategorii w kodzie. Nie ma przycisku,
widoku, zadań działających w tle ani mechanik transportu. Próba wybrania jego
nieaktywnego identyfikatora wraca do głównej zakładki. Pozostałych kategorii
ze zdjęcia (saldo, premie, czarna lista i cele) nie dodano.

System własnych rang został usunięty na życzenie użytkownika. Pozostają lider
oraz członkowie. Lista członków służy przeglądaniu; czynności administracyjne
znajdują się w Ustawienia → Zarządzanie, widocznym wyłącznie dla lidera:
zgłoszenia, przyjmowanie/odrzucanie, usuwanie członków, przekazanie przywództwa
oraz odświeżanie danych. Serwer sprawdza uprawnienia niezależnie od przycisków.
Zgłoszenia nie są wysyłane do zwykłych członków.

Lider przed odejściem przekazuje przywództwo, jeśli ma innych członków.
Odejście ostatniego członka usuwa pusty gang po potwierdzeniu.

## Kolory i wygląd

Ustawienia mają kategorie „Wygląd”, „Logo” oraz, dla lidera, „Zarządzanie”.
„Zmień kolor…” otwiera koło barw, pola HEX/RGB i pionowe suwaki z gradientami:

- **Jasność:** wybrany kolor u góry, czerń na dole.
- **Nasycenie:** nasycony kolor u góry, odcień neutralny na dole.
- **Krycie tła:** pełne krycie u góry, przezroczystość na szachownicy na dole.

Suwaki obsługują kliknięcie, przeciąganie i klawisze strzałek; Home ustawia 100%,
End 0%. Po „Zastosuj” widzisz podgląd menu. Każdy członek może zapisać wygląd
osobisty albo używać wyglądu gangu. Wspólny motyw i logo zapisuje lider.
Osobisty wygląd nie zmienia ustawień u pozostałych graczy.

## Własne logo

Importer nie analizuje symboliki, treści ani nazw obrazów i nie ma listy
zakazanych flag. Własne grafiki i flagi przechodzą tę samą ścieżkę odczytu,
kadrowania oraz zapisu. Sprawdzane są poprawność pliku i limity zasobów.

Wybierz obraz z systemowego okna, wklej lokalną ścieżkę albo przeciągnij jeden
plik z folderu na otwarty panel gry. PNG/JPG/JPEG/GIF/BMP oraz WebP, SVG, TIFF, ICO, ICNS, PSD, TGA, PCX i PNM. Plik rastrowy do 64 MB; SVG do 8 MB. W razie potrzeby użyj F11,
aby widzieć grę i folder jednocześnie.

Obrazy 256×256, poziome, pionowe i inne proporcje są przyjmowane. GIF używa
pierwszej klatki. Limit wymiarów rastra: 100 000 px na bok i miliard pikseli;
podgląd jest pomniejszany do maksymalnie 1024 px na dłuższym boku.

W oknie „Dopasuj logo” przesuwaj obraz, przybliżaj kółkiem lub suwakiem.
Wybierz kwadratowy kadr albo cały obraz z zachowaniem proporcji i przezroczystymi
marginesami. „Anuluj”/X/Esc zachowuje poprzednie logo. „Zastosuj logo” przygotowuje
podgląd; zapisz go przez „Utwórz gang” lub Ustawienia → Logo → „Zapisz logo gangu”.

Logo jest zapisywane jako bezstratny PNG **512×512**, do 1,1 MB. Nagłówek,
podgląd i główna zakładka wyświetlają cały obraz z wygładzaniem skalowania.
Dla wyraźnego logo przygotuj źródło 512×512 lub większe; przy mocnym kadrowaniu
warto mieć większy oryginał. Mniejszy obraz jest przyjmowany, ale powiększanie
nie odzyska brakujących szczegółów. Nie ma wyboru stylu ani przerabiania rysunku
na inny rodzaj grafiki.

Dotychczasowe logo 64×64 nadal się wyświetla. Aby odzyskać detale utracone
podczas wcześniejszego importu, wczytaj ponownie oryginalny plik i zapisz logo.
Pozostali gracze nie potrzebują pliku źródłowego. Import z linku nie jest jeszcze
obsługiwany.

## Zapis i aktualizacja

Gangi: `data/crimeandlaw_gangs.dat` w danych świata Overworld.
Osobisty motyw: `config/crimeandlaw-appearance.json`.
Starsze zapisy 0.1.0–0.1.2 zachowują gangi, właściciela, członków, zgłoszenia,
logo i wspólny motyw. Dawne rangi oraz przypisane im uprawnienia są pomijane.

Nie ma limitu członków ani zgłoszeń na gang. Listy są wysyłane i wyświetlane
stronami po sześć pozycji; licznik pokazuje pełną liczbę członków. Duży gang
nie powoduje przesłania całej listy w jednym pakiecie. Jeden gang na gracza,
nazwy 3–32 znaki, roboczy limit 512 gangów na świat.

## Budowanie i sprawdzanie

Z JDK 21:

```text
Windows: gradlew.bat build
Linux/macOS: ./gradlew build
```

Wynik: `build/libs/crimeandlaw-0.1.11.jar`.
Gradle Wrapper 9.2.1, ModDevGradle 2.0.148, NeoForge 21.1.248.
`./gradlew test` sprawdza model, migrację zapisów, duże listy i prywatność
zgłoszeń, walidację/edycję logo, kolory oraz skalowanie menu.

`./gradlew -PuiSmoke runClient` uruchamia prawdziwy klient z przykładowymi danymi,
sprawdza menu i edytory oraz zapisuje obrazy i raport w `build/ui-smoke`.
Wymaga ekranu/OpenGL. Kod z `src/uiTest` jest wyłączony z wydania JAR.
Test graficzny nie łączy dwóch graczy na serwerze.

## Dalszy zakres

Mapa, własność chunków, oznaczenia, NPC, produkcja, handel i rozbudowane
animacje pozostają dalszymi etapami. Ta wersja dotyczy panelu gangów.

Definicje kategorii: `GangCategory.java`. Tylko `TRANSPORT` ma umiejscowienie `HIDDEN`; aktywację i mechaniki pozostawiono do dalszej pracy.

Rejestr: numer strony i dwie sąsiadujące strzałki w prawym narożniku nagłówka; powiększona lista. Maksymalnie 100 stron po sto wydarzeń.

W 0.1.8 dodano przyciski Premie, Zarządzanie saldem, Czarna lista i Cele. Otwierają panele informacyjne; funkcje tych systemów są do rozwijania. Transport nadal jest ukryty.

## Import logo 0.1.9
Format jest rozpoznawany po danych, a nie rozszerzeniu. Czytniki są dołączone do JAR-a. Przykładowo WebP lub SVG zapisany pod nazwą PNG może zostać odczytany bez ręcznej konwersji. Podgląd ma najwyżej 1024 px na dłuższym boku, a po kadrowaniu do serwera trafia PNG 64×64. SVG renderuje lokalne kształty wektorowe; odwołania do obrazów i plików zewnętrznych są pomijane. Formatów HEIC/AVIF, PDF i animowanego logo nie obsługuje to wydanie.

## Odczyt logo 0.1.10
Jeśli pierwszy czytnik rozpoznaje format, lecz nie potrafi odczytać danego wariantu,
importer próbuje kolejnych dostępnych czytników. Każda próba zaczyna od osobnego
strumienia danych, a nie od pozycji pozostawionej przez poprzedni czytnik.
Błędy odczytu mają komunikat po polsku; szczegóły czytnika trafiają na poziom DEBUG.
Obsługiwane formaty oraz limity plików pozostają takie jak w 0.1.9.

## Szczegółowość logo 0.1.11
Zapis zwiększono z 64×64 do 512×512. Podniesiono limity walidacji, zapisu świata
i obu kierunków przesyłania; protokół sieciowy 4 wymaga aktualizacji klienta
i serwera. Tekstura jest odczytywana ze strumienia i ładowana tylko po zmianie
logo. Każde miejsce wyświetlania używa pełnego zakresu obrazu oraz filtra liniowego.
Wygładzany jest również podgląd kadrowania. Zachowana obsługa dawnych logo 64×64.
