# Sprawdzenie 0.1.1

19 października 2026. Wersja 0.1.1 poprawia ekran tworzenia/wyszukiwania
według szkicu użytkownika oraz renderowanie i skalowanie całego panelu.

## Wyniki

- Gradle build i generowanie wrappera: BUILD SUCCESSFUL, JDK 21.
- 14 testów JUnit bez błędów: 10 modelu gangów, 2 walidacji PNG,
  2 proporcji panelu i przeliczania współrzędnych.
- Uruchomiono rzeczywistego klienta Minecraft 1.21.1 / NeoForge 21.1.248
  z Crime And Law 0.1.1 w rozdzielczości 1920×1080. Wirtualny ekran,
  OpenGL 4.5 Mesa llvmpipe, bez logowania konta i bez serwera.
- Obejrzano zrzuty siedmiu widoków z przykładowymi danymi: tworzenie przy
  skali GUI 2 i 4, wyszukiwanie, gang, członkowie, rangi i ustawienia.
- Sprawdzono kliknięcie pola po przeliczeniu skali, wpisywanie tekstu,
  kliknięcie zakładki wyszukiwania, równe szerokości obu górnych zakładek
  i widoczność przycisku utworzenia bez przewijania.
- Dodatkowy test obrazu wykrywa znikające tło po zmianie skali/zakładki.
  Poprawiono stan bufora głębi; ponowny przebieg zakończył się
  CRIME_UI_SMOKE_PASS. Napisy na końcowych obrazach są ostre.
- Produkcyjny JAR jest pakowany z wyłączeniem klasy testu graficznego.

Test graficzny jest odtwarzalny przez `./gradlew -PuiSmoke runClient`.
Korzysta z danych testowych, a nie z dwóch połączonych graczy. Nie potwierdza
całego przepływu sieciowego, rzeczywistych kont, renderowania świata w tle
ani poprawności na każdym GPU i każdej rozdzielczości. Przepływ między dwoma
rzeczywistymi klientami pozostaje do sprawdzenia.

## Dotychczasowe sprawdzenie serwera

Poprzednia wersja 0.1.0 uruchomiła osobny serwer developerski NeoForge,
utworzyła świat i doszła do komunikatu Done. Nie powtarzano tego testu
w 0.1.1: zmiany dotyczą klienta i układu menu, a model gangów i protokół
serwerowy pozostały takie same. Testy modelu nadal przechodzą.

## Środowisko budowania

W środowisku Work NeoForm Runtime 2.0.31 nie otrzymywał ścieżki procesu
z ProcessHandle.current().info().command(). Dwie lokalne klasy narzędzia
budującego (NeoFormEngine i ExternalJavaToolAction) otrzymały fallback na
java.home/bin/java zamiast bezwarunkowego Optional.orElseThrow().
Nie zmieniano logiki transformowania Minecrafta. Korekta narzędzia nie jest
częścią JAR-a moda ani kodu projektu; zwykły komputer korzysta z oryginalnej
zależności pobieranej przez Gradle.

W kliencie developerskim usługi kont Minecraft zgłaszały niedostępność DNS.
Nie blokowało to uruchomienia renderera i testu menu z przykładowymi danymi.
