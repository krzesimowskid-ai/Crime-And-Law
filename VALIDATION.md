# Sprawdzenie 0.1.9

21 października 2026. Minecraft 1.21.1, NeoForge 21.1.248, JDK 21.

## Zmiana

Importer logo rozpoznaje format po zawartości, a nie rozszerzeniu. Obsługiwane:
PNG, JPEG, GIF, BMP, WebP, SVG, TIFF, ICO, ICNS, PSD, TGA, PCX i PNM.
Czytniki TwelveMonkeys 3.15.3 i renderer JSVG 2.0.0 są dołączone do JAR-a
razem z zależnościami. NeoForge 1.21.1 otrzymuje też biblioteki na classpath
uruchomień deweloperskich. Rejestracja czytników nie polega wyłącznie na
kontekstowym classloaderze wątku asynchronicznego.

Limit pliku rastrowego 64 MB, boku 100 000 px i miliarda pikseli. Podgląd jest
odczytywany z subsamplingiem do maksymalnie 1024 px na dłuższym boku. SVG do
8 MB jest renderowany do podglądu o tym samym limicie, z przezroczystością.
Odwołania SVG do innych plików i obrazów są pomijane. Kadrowanie i dopasowanie
proporcji kończą się PNG 64×64 do 20 KB. Protokół 3 pozostaje zgodny z 0.1.8.

## Testy automatyczne

42 testy JUnit, zero błędów. Zachowano dotychczasowe 35 testów, dodano siedem:

- Dziewięć rzeczywistych plików: WebP lossless/lossy, TIFF, ICO (DIB), ICNS,
  PSD, TGA, PCX i PPM. Każdy ma mylące rozszerzenie PNG; sprawdzane są wymiary,
  kanał alfa oraz kolor z tolerancją dla stratnego WebP.
- SVG pod nazwą PNG, tylko viewBox 6000×3000, podgląd 1024×512,
  kolor i przezroczystość.
- PNG zawierający tekst SVG w metadanych nadal jest poprawnie czytany jako raster.
- SVG nie odczytuje zewnętrznego pliku.
- WebP z kontekstowym classloaderem bez klas aplikacji.
- Panorama PNG 20 000×4 zostaje zredukowana do podglądu bez zerowego wymiaru.
- Pusty/uszkodzony plik i przekroczenie limitu mają czytelne błędy.

Dotychczasowy test obrazu zachowuje 12 kombinacji PNG/JPEG/GIF/BMP z rozmiarami
256×256, 640×160 i 80×320, kadrowaniem, dopasowaniem oraz poprawnym PNG 64×64.

## Klient graficzny

CRIME_UI_SMOKE_PASS: prawdziwy renderer Minecrafta, 20 zrzutów 1920×1080,
GUI 2 i 4, Mesa llvmpipe. W klasach załadowanego moda sprawdzono asynchroniczny
odczyt dziewięciu plików testowych oraz normalizację do PNG. SVG pod nazwą PNG
przeszedł onFilesDrop, ekran kadrowania, zatwierdzenie i podgląd tekstury.
Dotychczasowe testy kategorii, kolorów, rejestru, klawiatury i uprawnień również
przeszły. Tylko Transport pozostaje ukryty.

## Budowanie i ograniczenia

`./gradlew clean build` oraz `./gradlew -PuiSmoke runClient`.
Testy UI używają danych przykładowych, nie dwóch graczy na serwerze.
Fizyczne okno wyboru pliku w Windows i przeciąganie z Eksploratora nie były
automatyzowane. Oryginalnego problematycznego pliku logo nie otrzymano;
konkretnej przyczyny jego odrzucenia nie potwierdzono na tych samych bajtach.
HEIC/AVIF/PDF i animowane logo nie są obsługiwane. GIF używa pierwszej klatki.
Nie oznacza to obsługi każdego możliwego wariantu każdego formatu.

W narzędziu NeoForm Runtime lokalnie użyto fallbacku java.home/bin/java,
ponieważ środowisko nie udostępnia ProcessHandle.Info.command. Ta zmiana nie
trafia do moda ani repozytorium. Usługi kont Minecraft są niedostępne w tym
środowisku; testy z danymi przykładowymi nie wymagają logowania.
