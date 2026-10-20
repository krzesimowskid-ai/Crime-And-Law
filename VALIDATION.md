# Sprawdzenie 0.1.8

20 października 2026. Minecraft 1.21.1, NeoForge 21.1.248, JDK 21.

## Zakres

Widoczne Gang, Członkowie, Rejestr, Mapa i Ustawienia. Tylko Transport ma
ukrytą definicję: nie jest dodawany do nawigacji i jego identyfikator wraca
do widoku głównego. Nie rejestruje operacji sieciowych, pojazdów ani zadań w tle.
Mapa pozostaje dotychczasową podstawą kontroli terytorialnej.

Rejestr jest zapisywany na serwerze wraz ze światem: utworzenie gangu,
dołączenia/odejścia/usunięcia, przekazanie przywództwa, wspólny wygląd i logo.
Ostatnie 10 000 wpisów, strony po 100, najnowsze pierwsze. Zgłoszenia oraz
odrzucenia kandydatów pozostają prywatne i nie trafiają do publicznego rejestru.
Starsze zapisy bez historii otrzymują pustą listę; nie dopisuje się przeszłości.
Protokół 3 bez zmian. Nowy rozmiar stron i limit wymagają wersji 0.1.7 na serwerze.

## Testy automatyczne

35 testów JUnit przeszło (w tym cztery testy modelu przewijania): dotychczasowe 27 oraz trzy testy kategorii/rejestru
i jeden test importu różnych formatów/proporcji. Test obrazu przechodzi 12
kombinacji: PNG/JPEG/GIF/BMP oraz 256×256, 640×160 i 80×320. Każda jest odczytana,
kadrowana, kodowana jako poprawna miniatura 64×64 i dopasowana z zachowaniem
przezroczystych marginesów przy prostokątnym źródle. GIF czytany jest z klatki 0.

Sprawdzono jedyną wyłączoną kategorię (Transport), brak wejścia przez jej ID,
poprawną listę nawigacji, zapis/odczyt historii, migrację starego zapisu,
limit i strony historii oraz brak wpisów dla nieudanych działań i prywatnych
zgłoszeń. Wcześniejsze testy członkostwa, prywatności, dużych gangów,
bezpieczeństwa miniatur, kolorów i skalowania również przechodzą.

## Klient graficzny

Końcowy przebieg: CRIME_UI_SMOKE_PASS, 20 zrzutów 1920×1080, GUI 2 i 4,
OpenGL 4.5 Mesa llvmpipe. Obejrzano główny panel i Rejestr. Widoczna zakładka
Rejestru pokazuje stronę rzeczywistych wydarzeń z danych testowych; następna
strona jest dostępna. Transport nie ma przycisku, a wymuszenie jego ID wraca
do widoku głównego. Przechodzą dotychczasowe sprawdzenia kolorów, kadrowania,
klawiatury, stronicowania i ograniczeń członka. Nie jest to test dwóch graczy.

## Odtwarzanie

`./gradlew build` i `./gradlew -PuiSmoke runClient`.
Test UI używa prawdziwego renderera Minecrafta i danych przykładowych.
Wyniki oraz zrzuty są w build/ui-smoke. Klasa testowa jest wyłączona z wydania.

## Ograniczenia

Testy modelu i UI nie stanowią próby synchronizacji dwóch rzeczywistych klientów.
Fizyczne przeciąganie z Eksploratora i systemowe okno plików na komputerze
użytkownika pozostają poza automatyzacją. WebP, SVG i animowane logo nie są
obsługiwane; GIF daje statyczną pierwszą klatkę. Plik do 20 MB, bok do 16 384 px,
łącznie do 64 milionów pikseli, podgląd najwyżej 1024 px na dłuższym boku.

## Budowanie

Końcowy `build`: BUILD SUCCESSFUL. Sprawdzono wersję 0.1.8 w JAR-ze,
obecność definicji kategorii/rejestru oraz wykluczenie klasy testu graficznego.

Lokalny NeoForm Runtime używa fallbacku java.home/bin/java w dwóch miejscach,
gdzie środowisko nie podaje ProcessHandle.command. Ta zmiana narzędzia nie
trafia do kodu moda ani JAR-a. Usługi kont Minecraft są niedostępne w tym
środowisku; dane przykładowe nie wymagają logowania.

## Przewijanie Rejestru

Sprawdzono kółko myszy, przeciąganie suwaka, klawisze End i granice przewijania. Nagłówek i przyciski stron pozostają nieruchome. Zmiana strony zeruje przewijanie, lista mieści 100 wpisów na stronie. Zmiana skali GUI zachowuje poprawną pozycję. Wpisy są zawijane i przycinane do obszaru listy; najnowsze pozostają na górze.

## Zmiana 0.1.7
Numeracja i sąsiadujące strzałki znajdują się w prawym narożniku nagłówka. Lista zajmuje zwolnioną część dolną. Test limitu obejmuje ponad 10 000 wydarzeń, 100 stron po 100 wpisów, kolejność i ograniczenie numeru strony. Historia finansowa została zaplanowana osobno od Rejestru; nowe systemy nie są częścią wydania.

## Kategorie 0.1.8
Test kategorii obejmuje osiem widocznych zakładek nawigacji i wyłącznie ukryty Transport. Test klienta sprawdza widoczność, aktywność i kliknięcie czterech dodanych przycisków.
