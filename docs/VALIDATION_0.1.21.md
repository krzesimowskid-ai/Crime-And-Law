# Weryfikacja 0.1.21

Minecraft 1.21.1, NeoForge 21.1.248, Java 21. Protokół 12 bez zmian.

## Zakres

Wspólna oś nagłówka: baner, logo oraz ramka nazwy. Poprzednia pozycja
logo i nazwy była o 3 jednostki GUI w lewo względem środka pełnego pasa.
Zmiana nie wpływa na zapis obrazów ani na kadr i powiększenie użytkownika.

## Wyniki

- Końcowe `build --offline`: **BUILD SUCCESSFUL**.
- 97 testów JUnit: bez niepowodzeń, błędów i pominięć.
- `header21d`: prawdziwy renderer Minecrafta, 1920×1080. Pionowe źródło
  240×300 dopasowane w całości, następnie dodatkowo oddalone do 0,65
  tego przybliżenia. Każdy wariant sprawdzony przy skalach GUI 2 i 4.
- Cztery porównania pikseli rzeczywistego obrazu: środek widocznej grafiki
  banera, czerwonego logo oraz obramówki nazwy wyniósł za każdym razem
  dokładnie **1089,5 px**. Dopuszczalna różnica próby: 1,1 fizycznego piksela;
  zmierzona różnica pomiędzy tymi trzema elementami: **0 px**.
- Powstały cztery zrzuty `header-alignment-0.png`–`header-alignment-3.png`.
  Wynik: `build/ui-smoke-header21d/result.txt`, zakończenie klienta kodem 0.

Próba używa kontrolowanej pionowej grafiki, żeby wykrywać przesunięcie osi
niezależnie od zawartości oryginalnego obrazu. Porównuje obraz wyrenderowany,
nie tylko wartości współrzędnych w kodzie. Nie wymusza zmiany proporcji,
wybranego kadru ani zapisanego banera gracza.

## Gotowy JAR

`crimeandlaw-0.1.21.jar`: 1 840 415 bajtów. Bez błędów CRC, powtórzonych
wpisów i klas automatyzacji. 97 klas produkcyjnych zgodnych bajt w bajt
z kompilacją, 15 dołączonych bibliotek JarJar, metadane wersji 0.1.21.

SHA-256: `95054d56075f97cfe8e03eb5176f611f4e6b30dfcc24f5c4c6a7e941c5439a2b`.

Poprzednie pełne sprawdzenie mapy i sieci:
[0.1.20](VALIDATION_0.1.20.md).
