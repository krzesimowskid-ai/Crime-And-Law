# Sprawdzenie 0.1.10

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

W 0.1.10 błędy jednego czytnika nie kończą importu: kolejne rozpoznające format
czytniki próbują odczytu, każdy z osobnego strumienia. Importer nie analizuje
symboliki ani treści obrazu i nie ma listy zakazanych flag lub nazw plików.

## Testy automatyczne

44 testy JUnit, zero błędów i pominiętych testów. Zachowano 42 testy z 0.1.9,
dodano dwa scenariusze:

- Sześć modeli PNG: RGB, ARGB, 8- i 16-bitowa szarość, jednobitowy i paletowy.
  Różne nazwy, znaki Unicode, brak rozszerzenia i niezgodne rozszerzenie nie
  blokują odczytu; pliki przechodzą również końcowe kodowanie logo.
- Czytnik rozpoznający PNG, ale zawodzący przy odczycie: zarówno IOException,
  jak i RuntimeException. Następny czytnik poprawnie ładuje obraz, nawet gdy
  poprzedni przesunął i opróżnił swój strumień. Każdy czytnik jest zwalniany.

Zachowane sprawdzenia rozszerzonego importu z 0.1.9:

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
