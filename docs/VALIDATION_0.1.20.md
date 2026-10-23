# Weryfikacja 0.1.20

Minecraft 1.21.1, NeoForge 21.1.248, Java 21, protokół 12.

## Poprawiony przypadek

Nagranie użytkownika pokazuje przesuwanie gotowego małego prostokąta i puste
pasy przy kolejnych zmianach kierunku. Weryfikacja obejmuje cały gest, także
z nadal wciśniętą myszą oraz odpowiedziami serwera w trakcie ruchu.

## Testy

- `build --offline`: **BUILD SUCCESSFUL**; **97 testów**, bez błędów,
  niepowodzeń i pominięć. Obejmują przeliczenie ruchu myszy na współrzędne
  świata, zmianę kierunku, granice świata, niezależne zaznaczenie i pamięć
  sąsiednich odpowiedzi. Pełna wiadomość 225 kafli mieści się w limicie kodeka.
- `map20a`: prawdziwy renderer Minecrafta, 1920 × 1080, skale GUI 2 i 4,
  78 zrzutów. Dodatkowy przypadek obejmuje trzy przybliżenia i **18 kroków
  przeciągania ze zmianami kierunku**. Opóźnione odpowiedzi dla sąsiednich
  widoków docierają przy wciśniętej myszy. Kontrola pikseli potwierdza brak
  tła i mgły na całym widocznym, wcześniej rozpoznanym terenie. Fokus,
  zaznaczenie chunka, powrót i puszczenie myszy pozostają poprawne.
- `terrain20b`: prawdziwy serwer dedykowany, pierwszy start i restart tego
  samego świata. Zachowano próby wioski, stałych cech, kolorów wszystkich
  kolumn, granic kafli, odświeżenia zmian i skrajnych współrzędnych.
  Sprawdzono kafel przy kamerze poza dawnym zasięgiem gracza oraz zimny odczyt
  rzeczywistego zapisu chunka FULL po restarcie. Paleta i heightmapa odtwarzają
  przygotowaną powierzchnię. Odczyt nie ładuje tego chunka do świata, nie
  zwiększa liczby załadowanych chunków i nie tworzy odległego pustego regionu.
- `net20b`: serwer dedykowany i **dwa oddzielne klienty przez TCP**. Na
  rzeczywistym terenie wykonano sześć zmian kierunku z trzymaną myszą;
  **29 odpowiedzi serwera** nie przerwało gestu, nie zmieniło zaznaczenia
  i nie zostawiło uciętych brzegów. Sprawdzenie obejmuje piksele całego
  znanego widoku, powrót kamery oraz puszczenie przycisku. Zachowano próby
  członkostwa, uprawnień, prywatności zgłoszeń i identycznych szczegółowych
  obrazów na obu klientach. Logo (19 196 396 znaków Base64) i baner
  (17 687 236 znaków) przeszły transmisję i porównanie skrótów; restart
  zachował obrazy, członków, rangi, uprawnienia, daty i wspólny wygląd.

Wyniki lokalne: `build/ui-smoke-map20a/result.txt`,
`build/server-smoke-terrain20b/result.txt` oraz trzy pliki `*.PASS` w
`build/network-smoke-net20b/`. Próby klienta i sieci uruchamiają prawdziwą
grę; nie zastępują długotrwałego pomiaru wydajności na komputerze gracza.

## Gotowy JAR

`crimeandlaw-0.1.20.jar`: 1 840 411 bajtów. Archiwum bez błędów CRC i
powtórzonych wpisów; 97 klas produkcyjnych zgodnych bajt w bajt z kompilacją,
15 bibliotek JarJar, poprawna wersja metadanych, bez klas automatyzacji prób.

SHA-256: `37d8ed9743c963d482431ac0b2568feb2ea0fc5bcde0b4e322777dc17437994e`.

Protokół 12 wymaga tej wersji na serwerze i u wszystkich graczy.

## Granice zachowania

Mapa używa prawdziwych danych: załadowanych bloków, gotowych kafli i istniejących
pełnych chunków NBT. Dane są zbierane stopniowo. Niewygenerowany teren i zapis
w nieobsługiwanym lub uszkodzonym formacie pozostają zasłonięte. Mod nie generuje
świata dla mapy, nie tworzy pustych regionów i nie utrzymuje odległych chunków
w pamięci świata. Cieniowanie przy granicy pojedynczego pliku korzysta z
lokalnej wysokości; analiza stałych cech terenu pozostaje oddzielna.

Wcześniejsze sprawdzenia: [0.1.19](VALIDATION_0.1.19.md),
[0.1.18](VALIDATION_0.1.18.md).
