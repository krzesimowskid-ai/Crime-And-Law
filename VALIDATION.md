# Weryfikacja 0.1.22

Minecraft 1.21.1, NeoForge 21.1.248, Java 21. Protokół 13.

## Zakres

Swobodne położenie kamery mapy między chunkami oraz szczegółowy baner
8192×1901 (limit 8192×2048 / 68 MB). Logo bez zmian: 2048×2048 / 17 MB.
Zachowany kadr, przybliżenie i oś nagłówka z 0.1.21.

## Potwierdzone wyniki

- `build --offline`: BUILD SUCCESSFUL.
- 98 testów JUnit: 0 niepowodzeń, 0 błędów, 0 pominięć.
- Import i zapis 16 777 216 pikseli źródła 8192×2048, porównanie wszystkich
  wierszy obrazu. Zapis, ponowny odczyt i kodeki stanu z dużym logo i banerem.
- Osobna walidacja banera oraz zachowana obsługa dawnych 4096×1024.
- Powtarzane gesty od ułamkowych współrzędnych przy każdym przybliżeniu.

Trwa odtwarzanie i wykonywanie próby renderera oraz dwóch klientów TCP.
Wyniki tych prób i kontrola końcowego JAR-a będą uzupełnione przed wydaniem.
Wynik wcześniejszej sesji `free22b` był pozytywny, ale bieżący kod jest
sprawdzany ponownie po odtworzeniu środowiska; nie zastępuje to nowego raportu.

Poprzednie sprawdzenia: [0.1.21](docs/VALIDATION_0.1.21.md),
[0.1.20](docs/VALIDATION_0.1.20.md).
