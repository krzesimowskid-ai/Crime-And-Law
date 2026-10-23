# Weryfikacja 0.1.19

Minecraft 1.21.1, NeoForge 21.1.248, JDK 21.

## Zakres

- Logo 2048×2048, baner do 4096×1024 (pas 4096×951), PNG do 17 MB.
- Oddzielny obraz roboczy do 8192 px / 16 777 216 pikseli i podgląd do 2048 px.
- Pełne mipmapy i filtrowanie trójliniowe dla logo, baneru i podglądu.
- Przygotowanie końcowego PNG w tle; blokada podwójnego zatwierdzania
  i odrzucanie wyniku po anulowaniu okna.
- Protokół 11 oraz dotychczasowy przesył w kompletnych logicznych wiadomościach
  podzielonych na pakiety do 24 KiB; większy limit całego obrazu.

## Wyniki

93 testy JUnit: 0 niepowodzeń, 0 błędów, 0 pominiętych.
Końcowe `--offline build`: BUILD SUCCESSFUL (45 sekund).

Sprawdzono końcowy JAR: 92 klasy produkcyjne zgodne z kompilacją,
15 dołączonych bibliotek, brak klas automatyzacji i powtórzonych wpisów ZIP.
SHA-256 JAR: `dc9a8d04426f329d801a9a88b5a35ae81446383c56d23104b7b65b63a5eb2cb8`.

### Zachowanie szczegółów

- Test porównuje każdy piksel źródła 4096×1024 po odczycie z pliku i zapisie
  baneru; naprzemienny wzór o szerokości 1 px pozostaje identyczny.
- Logo zachowuje pikselowy wzór w natywnej rozdzielczości 2048×2048.
- Roboczy poziomy SVG 8192×1024 pozostaje w tej rozdzielczości, a kwadrat
  8192×8192 mieści się w budżecie jako 4096×4096.
- Mniejszy podgląd 2048 px nie zastępuje szczegółowego źródła przy zapisie.

### Prawdziwy renderer Minecrafta

Próba `res19c`: 1920×1080, GUI 2 i 4, 60 zrzutów 00–59.
Raport `build/ui-smoke-res19c/result.txt`: PASS.

- Rzeczywiste tekstury logo 2048×2048 i baneru 4096×951, starsze PNG 64×64.
- Odczyt parametrów OpenGL potwierdza filtrowanie trójliniowe przy pomniejszaniu
  i liniowe przy powiększaniu. Sprawdzono rozmiary każdego poziomu mipmap,
  także niesymetrycznych i niebędących potęgą dwójki wymiarów baneru.
- Drobna czarno-biała kratka źródłowa po wyświetleniu w mniejszym polu daje
  stabilną szarość: sprawdzono rzeczywiste piksele z bufora ekranu.
- Import, przesuwanie, scroll i zatwierdzenie PNG w tle; blokada drugiego
  zatwierdzenia i brak zastosowania porzuconego kadru po anulowaniu.
- Zachowano wcześniejsze sprawdzenia formularza, rang, uprawnień, rejestru,
  nawigacji mapy, pełnego pasa baneru, portretu i nakładek skina.
- Przed próbą odtworzono brakującą po wyczyszczeniu środowiska mapę testową
  na prawdziwym serwerze: seed 1, wieś/farma, chunk 38,51, 49 kafli.
  To pierwsze uruchomienie testu terenu; nie powtarzano jego osobnego restartu.
- Wcześniejsze próby: `res19a` wymagała brakującego pliku mapy; `res19b`
  wykazała zależne od FPS oczekiwanie w teście przeciągania rang. Test czeka
  teraz do końca przewijania, maksymalnie 3 sekundy czasu rzeczywistego.
  Nie zmieniano mechanizmu przeciągania rang. Końcowa próba `res19c`: PASS.

### Dwa klienty i zapis świata

Próba `net19a`: własny serwer na 127.0.0.1:25597 oraz dwa prawdziwe
klienty 960×540, osobne katalogi gry. `owner.PASS`, `member.PASS`,
`restart.PASS` w `build/network-smoke-net19a`. Procesy zakończone poprawnie.

- Utworzenie gangu, zgłoszenie, przyjęcie i przypisanie prawa wyglądu przez UI.
- Szczegółowe losowe logo 2048×2048: 19 196 396 znaków Base64.
- Szczegółowy losowy baner 4096×951: 17 687 236 znaków Base64.
- Oba obrazy jednocześnie w stanie gangu, zgodne SHA-256 u obu klientów,
  właściwe rozmiary tekstur, wspólny motyw i zachowane uprawnienia.
- Odrzucone niedozwolone zmiany wyglądu/baneru i usunięcie lidera.
- Restart tego samego świata zachował oba obrazy, członków, właściciela,
  rangę, prawa, motyw oraz rzeczywiste daty dołączenia.

Nie potwierdza to pobrania konkretnego skina z Mojang ani jakości dowolnego
obrazu użytkownika; jakość źródła jest niezależnym ograniczeniem.
Wcześniejsze sprawdzenie 0.1.18: [raport](docs/VALIDATION_0.1.18.md).

## Ograniczenia

Większa tekstura nie odtworzy szczegółów, których nie ma w źródle.
Starsze zapisy działają; dla większej szczegółowości trzeba ponownie wczytać oryginał.
Przechowywany jest zatwierdzony PNG, nie pełny plik sprzed kadrowania.
Nie zmieniano obsługiwanych formatów ani pozostałych mechanik moda.
