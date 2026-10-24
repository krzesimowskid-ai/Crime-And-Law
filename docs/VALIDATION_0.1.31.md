# Walidacja 0.1.31 — 24 października 2026

Minecraft 1.21.1, NeoForge 21.1.248, Java 21. Protokół 18, schemat danych 4.

## Poprawa HUD-u po zrzucie użytkownika

- Minimapka 136×86 jednostek: spokojne szarości terenu, przygaszona niebieska
  woda, krycie terenu 176/255 i brak nieprzezroczystego podkładu. Brakujące
  kafle są przyciemnione bez szachownicy. Filtr dwuliniowy ustawiany po
  każdym uploadzie: NativeImage.upload przywracał poprzednio filtr najbliższy.
- Północ na przecięciu kierunku z krawędzią prostokąta, z zapasem na znak.
  Znak nie wędruje przez środek. Gracz jest ostrą strzałką z ciemną obwódką.
- Dwa kompaktowe wiersze pieniędzy bez dużych kart, tytułów i akcentów tła.
  Wektorowy banknot i karta, gładki podzbiór DejaVu Sans Bold, cień tekstu.
  Pełna maksymalna kwota mieści się po automatycznym zmniejszeniu tekstu.
- Wektorowe kształty są grupowane w buforze GUI; normalizacja kolejności
  wierzchołków zapobiega odrzucaniu ikon przez back-face culling.

## Zakres sprawdzenia

Test geometrii sprawdza granice wskaźnika północy dla każdego stopnia od
−720 do +720 i cztery znane kierunki. Test układu obejmuje osiem rozmiarów
natywnego GUI oraz kolizje z hotbarem, sercami i nieaktywnym czatem.

Rzeczywisty renderer i dwa klienty TCP: 1920×1080, GUI 2/4, cztery kierunki
patrzenia, pełne maksymalne kwoty, F1, otwarty czat i oczekiwanie na saldo.
Przed zakończeniem test sprawdza jasne piksele w obu obszarach ikon, aby
wykryć niewidoczne kształty. Zrzuty podlegają także kontroli wzrokowej.
Kwoty 148 840 / 69 420 i maksymalne kwoty są wyłącznie danymi podglądu
klienta i przed końcem testu są przywracane. Serwer niczego nie dopisuje.

Powtórzono istniejący scenariusz wpłat, wypłat, premii, odrzuconych pakietów,
nieuprawnionych operacji, rzeczywistej śmierci/respawnu, czarnej listy i celów.
Restart tego samego świata weryfikuje zachowane salda i historie. Ekonomia,
prywatne pakiety sald i zapis świata nie były zmieniane w tym wydaniu.

Sterowniki: `src/financeTest`, opcja `-PfinanceSmoke`, wyłączone z wydania.
Wyniki końcowe i suma kontrolna JAR-u: `validation-0.1.31.json`.

## Wydanie

Końcowe `clean build --offline` zakończone kodem 0. **152 testy**, zero
niepowodzeń, błędów i pominięć. CRC głównego JAR-u oraz 15 bibliotek w środku
poprawne. Brak klas automatyzacji. Czcionka HUD i jej licencja dołączone.

`crimeandlaw-0.1.31.jar`: 2 009 939 bajtów.

SHA-256: `b7bba6e659050f7f46d98fcb0d10ad28b12a15d535b6647934a02f7b15adadb7`.

Po kontroli usunięto stary duplikat licencji z niepoprawnej ścieżki zasobów
i ponownie spakowano `processResources jar` (bez zmian w klasach).
