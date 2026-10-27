# Weryfikacja 0.1.50 — liście, UV i doniczka

Minecraft 1.21.1 / NeoForge 21.1.248 / Java 21 / protokół 27.
**Wersja testowa na kopię świata.** Poprzedni nieregularny problem zapisu
stanowiska pozostaje otwarty; ta zmiana nie modyfikuje implementacji zapisu.

## Zmiana wynikająca z nagrania

Obejrzano lokalnie całe nagranie LAN 14-33-00 (41,4793 s): górne liście
bez koloru UV oraz wyraźne załamania liści i zatrzymywanie odciętych części.
Przenikanie ziemi/wody przez doniczkę zgłoszono w opisie; ten film pokazuje
świat i zbiór. Kolizje odtworzono osobno w rzeczywistym oknie przygotowania.

- Światło modeli/liści próbkuje przy powierzchni (0,012 bloku). Przesunięcie
  0,45 bloku pozostaje dla terenu. Wcześniej górna powierzchnia liścia mogła
  pobrać światło z punktu ponad emiterem, gdzie kierunkowe UV ma zerową emisję.
  Brak białej emisji/filtra obrazu; wiązka wciąż jest skierowana w dół.
- Cząstki ziemi i wody mają kontakt z ośmiokątnym obrzeżem, zwężającą się
  ścianką wewnętrzną/zewnętrzną, powierzchnią ziemi i dnem. Tor jest dzielony
  pomiędzy klatkami, z dokładniejszym wyznaczeniem chwili pierwszego kontaktu.
  Nie przelatują dalej przez wykryty materiał. Strumień obok doniczki może
  spaść na zewnątrz. Kolizja dotyczy efektów wizualnych; koszt i serwerowe
  naliczanie trafienia pozostają bez zmian.
- Płytszy profil liścia: bez ostrych załamań, nadal pikselowa bryła z grubością.
  Odcięta geometria jest tą samą bryłą, obracaną sztywno wokół własnego środka.
- Usunięto zatrzymanie lotu na sztucznej podłodze i usuwanie po 2,3 s.
  Liść leci poniżej panelu w stronę dołu widoku Minecrafta. Jest usuwany
  dopiero po opuszczeniu dolnej krawędzi przez całą sylwetkę.

## Testy i oględziny

254 testy / 41 zestawów, 0 błędów/awarii/pominięć. Nowe przypadki:
kontakt ze ścianką/rantem od środka i od zewnątrz, wypełniona/pusta doniczka,
zależność od czasu klatki i rozmiaru cząstki, start i sztywność lotu liścia,
łagodniejsza krzywizna, dalszy spadek i opuszczenie widoku, regresja UV
na górnym liściu (stare przesunięcie ponad źródłem, nowe pod nim).

`crop-1`: test kolizji i dotychczasowe kontrole UV przeszły. Automat
fotografował ponownie otwarte okno zamiast świata przy przejściu do zbioru;
przebieg przerwano. Zmieniono wyłącznie synchronizację automatyzacji, aby
czekała na zamknięcie okna przed zdjęciem świata i ponownym otwarciem zbioru.

`crop-2`: świeży przebieg, dwa rzeczywiste klienty TCP i serwer. PASS:

- Wsypywanie skierowane na obrzeże i zwężającą się ściankę: rzeczywiste
  kontakty potwierdzone. Podczas wsypywania/podlewania każda poruszająca się
  cząstka była sprawdzana względem materiału doniczki; zachowana kontrola worka.
- Zrzuty trafiania ziemią w rant/ściankę i podlewania obejrzane.
- Dojrzała roślina sfotografowana w świecie, bez okna minigry: także górne
  liście mają fioletowe oświetlenie. Zrzut `owner-mature-crop-upper-leaves-uv.png`.
- Rzeczywiste cięcie, lot przez dawną podłogę i poniżej panelu, kontrola
  położenia całej bryły przy usunięciu. Obejrzane zrzuty lotu przy 0,9 i 1,3 s;
  przy 1,3 s liść pozostaje częściowo widoczny przy dolnej krawędzi Minecrafta.
- Osiem swobodnie wybieranych cięć: osiem nagród i osiem zużyć nożyc,
  brak zwrotu ziemi/nasion ani automatycznego odnawiania rośliny.
- Zachowanie ilości materiałów, zakupy/replay, sesje dwóch graczy,
  ochrona bloków, zamiana źródła, przedwczesne ukończenie, worki 1/2/3 użycia,
  cztery rozmiary/skale GUI i dotychczasowe kontrole UV Fancy/Fabulous.
- Pierwszy serwer exit 0; pełna kontrola restartu z dysku PASS w tym przebiegu.
  Sprzęt, zużyta ziemia, stan drugiej rośliny, przedmioty, woda i gotówka zachowane.

## Granice

Przejście tego restartu nie jest naprawą wcześniejszego nieregularnego błędu
zapisu. Nadal używaj kopii świata. Kolizje cząstek używają geometrycznego
przybliżenia bryły i promienia drobiny; krótkie efekty kontaktu są kosmetyczne.
Cienie częściowych bloków UV pozostają przybliżone. Nie deklarujemy braku
wszystkich błędów całego moda ani zgodności z każdą zewnętrzną paczką shaderów.

## JAR

- `crimeandlaw-0.1.50.jar`, 3964891 bajtów.
- SHA-256 `cc1f70e5187facd998a29baf30bb7c32ecde1038851c45815c296187ed9f4224`.
- CRC poprawne, brak duplikatów i klas Smoke.
- 256 klas produkcyjnych i 34 zasoby zgodne bajtowo z kompilacją/źródłami.
- Pakowanie bez źródeł automatyzacji. Kod wykonawczy nie był zmieniany po
  drugiej próbie w grze; ostatnie zmiany dotyczyły synchronizacji testu i dokumentacji.

Dane: [validation-0.1.50.json](validation-0.1.50.json).
