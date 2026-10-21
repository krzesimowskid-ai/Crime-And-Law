# Sprawdzenie 0.1.11

21 października 2026. Minecraft 1.21.1, NeoForge 21.1.248, JDK 21.

## Zmiana

Nowe logo kończy import jako bezstratny PNG 512×512 zamiast 64×64. Limit 1,1 MB
obejmuje również szczegółowy obraz RGBA o słabej kompresji. Wspólna stała określa
maksymalny wymiar, rozmiar binarny i długość Base64. Zapisy świata akceptują
większe logo oraz dotychczasowe 64×64. Zapisany wcześniej obraz nie odzyskuje
sam utraconych szczegółów; trzeba ponownie wczytać plik źródłowy.

Protokół 4: limity akcji i stanu zawierają pełne logo oraz zapas na metadane,
strony członkostwa i rejestru. Wersje protokołu 3 wymagają aktualizacji
klienta i serwera. Odczyt NativeImage korzysta ze strumienia, aby szczegółowy
PNG nie był kopiowany na mały stos LWJGL.

LogoTexture przechowuje rzeczywiste wymiary tekstury. Nagłówek, podgląd i główna
zakładka używają pełnego zakresu UV oraz liniowego filtra powiększania i
pomniejszania. Podgląd kadrowania również używa filtra liniowego. Tekstura
jest ładowana po zmianie danych, a nie przy każdej klatce. Układ i rozmiary
okien pozostają zachowane; nie dodano trybów przerysowywania grafiki.

## Testy automatyczne

47 testów JUnit, zero błędów i pominiętych testów. Włączono obsługę testów
Minecrafta przez ModDevGradle, dzięki czemu test kodeków używa prawdziwych
RegistryFriendlyByteBuf oraz kodeków moda.

Nowe i zaktualizowane sprawdzenia:

- Drobna czarno-biała szachownica 512×512 zachowuje każdy piksel po zapisie.
- Import PNG/JPEG/GIF/BMP przy różnych proporcjach kończy się 512×512;
  kadrowanie i przezroczyste marginesy pozostają poprawne.
- Losowe RGBA 512×512 mieszczą się w limicie binarnym i limicie akcji.
- Szczegółowe logo przekracza dawny limit akcji, przechodzi rzeczywiste kodeki
  Action i State, walidację oraz zapis/odtworzenie danych gangu. Sprawdzane są
  rozdzielczość i niezmieniony kolor po tej ścieżce.
- Kodeki nadal odrzucają ciągi ponad nowy limit. Maksymalne logo razem ze
  stroną członków i 100 wpisami rejestru mieści się w limicie stanu.
- Walidator akceptuje 64×64, 256×256 i 512×512; odrzuca przekroczenie boku
  oraz rozmiaru danych. Zapis dotychczasowych gangów nadal jest odczytywany.

Zachowane testy wieloformatowego importu, alternatywnych czytników po błędzie,
modeli kolorów PNG, SVG, nazw Unicode, limitów źródła, kolorów, rejestru,
paginacji, uprawnień i członkostwa.

## Klient graficzny

CRIME_UI_SMOKE_PASS: prawdziwy renderer Minecrafta, 20 zrzutów 1920×1080,
GUI 2 i 4, Mesa llvmpipe. Asynchroniczny import dziewięciu formatów i SVG
przechodzi do kadrowania, zatwierdzenia i podglądu. Sprawdzono rzeczywiste
tekstury 512×512, szczegółowy PNG większy od dawnego limitu oraz dawną
teksturę 64×64. Parametry OpenGL MIN_FILTER i MAG_FILTER mają GL_LINEAR,
także w podglądzie kadrowania. Logo zapisano w danych przykładowego gangu
i wyświetlono również na jego głównej zakładce.

## Budowanie i ograniczenia

`./gradlew build` oraz `./gradlew -PuiSmoke runClient`.
Test kodeków nie jest testem sieci TCP ani gry dwóch klientów na serwerze.
Testy UI używają przykładowych danych. Fizyczne okno wyboru pliku w Windows
i przeciąganie z Eksploratora nie były automatyzowane. Oryginalnego
problematycznego pliku logo użytkownika nie otrzymano.

Źródła: PNG, JPEG, GIF, BMP, WebP, SVG, TIFF, ICO, ICNS, PSD, TGA, PCX i PNM.
Limit źródła rastrowego 64 MB, 100 000 px na bok i miliard pikseli. Podgląd
do 1024 px. SVG do 8 MB, bez odczytu zewnętrznych zasobów. HEIC/AVIF/PDF
oraz animowane logo pozostają nieobsługiwane. GIF używa pierwszej klatki.

Lokalny fallback java.home/bin/java w NeoForm Runtime pozostaje wyłącznie
narzędziem środowiska; nie trafia do JAR-a ani repozytorium. Niedostępność
usług kont Minecraft nie blokuje testu z danymi przykładowymi.
