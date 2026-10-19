# Sprawdzenie 0.1.0

Wynik: Gradle build i wrapper zakończyły się BUILD SUCCESSFUL.
Wszystkie 12 testów JUnit przeszło (10 modelu gangów, 2 walidacji PNG).
Osobny serwer developerski NeoForge załadował Crime And Law 0.1.0,
utworzył świat i doszedł do komunikatu Done / gotowości.
Test startu korzystał z developerskiego trybu offline. Nie przetestowano
logowania kont ani uwierzytelnienia; środowisko zgłaszało niedostępność
api.minecraftservices.com, co nie zatrzymało startu serwera.


Testy obejmują pojedyncze członkostwo, zgłoszenia i ich akceptowanie,
blokadę nieuprawnionych działań, przekazywanie przywództwa, uprawnienia
własnych rang, niezależność światów, zapis/odczyt JSON, walidację nazw,
parametry motywu oraz przyjmowanie i odrzucanie logo PNG.

Kompilacja i testy nie zastępują sprawdzenia klienta Minecraft oraz dwóch
graczy na serwerze. Nie potwierdzono jeszcze wyglądu na rzeczywistym ekranie
Minecrafta ani całego przepływu między dwoma uruchomionymi klientami.

## Informacja o środowisku budowania

W ograniczonym środowisku Work NeoForm Runtime 2.0.31 nie otrzymywał ścieżki
procesu z ProcessHandle.current().info().command(). Dwie lokalne klasy narzędzia
budującego (NeoFormEngine i ExternalJavaToolAction) otrzymały fallback na
java.home/bin/java zamiast bezwarunkowego Optional.orElseThrow().
Nie zmieniano logiki transformowania Minecrafta. Ta korekta narzędzia nie jest
częścią JAR-a moda ani kodu źródłowego projektu; zwykły komputer korzysta
z oryginalnej zależności pobieranej przez Gradle.
