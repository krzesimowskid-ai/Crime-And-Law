# Weryfikacja 0.1.24 — zapis postępu

Minecraft 1.21.1, NeoForge 21.1.248, Java 21, protokół 14.

Zakres: pobieranie 21×21 przy widocznych 11×11, wyprzedzenie do trzech chunków,
8192 kafle z ochroną okolicy postaci, natychmiastowy Home, łączenie żądań
i ograniczona kolejka odczytu. Baner nadal 2K.

- Pierwszy `build --offline`: BUILD SUCCESSFUL; 104 testy JUnit bez błędów.
- `fast24a`: PASS w prawdziwym rendererze, GUI 2/4 i trzy przybliżenia.
  Identyczne piksele po puszczeniu, siatce i spóźnionych odpowiedziach.
  24 odległe odpowiedzi przekraczają pojemność pamięci; Home od razu
  odzyskuje chroniony obszar bez połączenia. 80 skoków do granic świata
  i nieprawidłowe współrzędne nie powodują wyjątku. 15 zrzutów.
- Test serwera ujawnił porzucanie wyników trwającego odczytu po zmianie
  aktualnego obszaru. Wyniki są teraz zapisywane również po porzuceniu
  widoku, przy zachowaniu ośmiu rzeczywistych slotów I/O.

Ponowny test serwera (`fast24b`), dwóch klientów oraz końcowa kompilacja
i kontrola JAR-a są w toku. Ten zapis postępu nie jest gotowym wydaniem.

Poprzedni raport: [0.1.23](docs/VALIDATION_0.1.23.md).
