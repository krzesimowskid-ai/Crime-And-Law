# Weryfikacja 0.1.49 — minigra przygotowania

Minecraft 1.21.1 / NeoForge 21.1.248 / Java 21 / protokół 27.
**Wersja testowa na kopię świata: kontrola zapisu stanowiska po restarcie FAIL.**

## Materiał i zmiana

Obejrzano lokalnie nagranie 16-49-38 (49,881 s), przegląd całej sekwencji
oraz dokładniejsze klatki otwierania, nasiona i początku podlewania.
Na nagraniu wybrano otwarty duży worek z 1/3 pozostałych użyć. Zgodnie
z wcześniejszym ustaleniem nie rozcina się go ponownie; okno teraz to wyjaśnia.
Nowy, zamknięty worek nadal wymaga przeciągnięcia po szwie, z widocznym nożem
prowadzonym zgodnie z postępem. Ostrze jest umieszczone w układzie modelu.

Piętnaście emiterów ziemi przesunięto z wnętrza rantu przed jego zewnętrzną
krawędź. Podczas ruchu narzędzia widoczne cząstki są sprawdzane względem
obróconej, zwężającej się bryły worka; cząstka wewnątrz nie jest rysowana.
To kontrola wizualnej kolizji, bez dodatkowych przedmiotów i bez refundacji.
Serwer nadal wylicza trafienie do doniczki z tej samej geometrii emisji.

Pierścień celu leży na wewnętrznej pochyłości dołka, a nie na górnej płaskiej
powierzchni ziemi. Usunięto drugi nakładający się cel. Wysokość spoczynku
nasiona uwzględnia jego grubość; serwer używa tego samego poziomu lotu.

Woda nie jest już pełnym łukiem rysowanym natychmiast od sitka do ziemi.
Każda z dziewięciu strużek składa się z krótkich odcinków historii ruchu
własnej cząstki; początek wypływu narasta od sitka zgodnie z czasem i grawitacją.

## Testy

244 testy, 39 zestawów, 0 błędów/awarii/pominięć. Dodano sprawdzenie emisji
przy wielu kombinacjach przechyłów obu osi, kolizji w obróconym układzie
worka oraz wysokości pierścienia wewnątrz zagłębienia.

Przebieg `preparation-1`, rzeczywisty serwer i dwa klienty TCP:

- PASS nowy worek nie otwiera się przez samo czekanie 450 ms;
  zrzuty zamkniętego, częściowo i całkowicie rozciętego worka obejrzane.
- PASS podczas całej rzeczywistej fazy wsypywania żaden renderowany środek
  drobiny z marginesem 0,007 nie leżał wewnątrz bieżącej bryły worka.
- PASS ręczny obrót WASD, chwyt LPM/mysz, brak samoczynnego obrotu,
  zatrzymanie i ponowny chwyt, częściowe/chybione trafienie.
- PASS pierwsza woda ma wiek mniejszy niż czas lotu do ziemi;
  zrzuty początku wypływu i dziewięciu późniejszych strużek obejrzane.
- PASS pierścień widoczny wewnątrz dołka, nietrafione nasiono/odzyskanie,
  zasypanie, podlanie, pełny cykl i osiem swobodnie wybieranych cięć.
- PASS zachowanie materiałów, zakupów, replay, blokad sesji i wcześniejszych
  adversarial checks; cztery rozmiary/skale GUI i dotychczasowe kontrole UV.
- PASS pierwszy serwer kończy kodem 0; znaczniki owner, rival, server,
  security, wasd, uv oraz recovery obecne.
- **FAIL ponowne uruchomienie z dysku:** `Reusable equipment and exhausted soil persist`.
  Marker `server.FAIL` z fazy restartu, brak `restart.PASS`. To ponowne
  wystąpienie otwartego problemu zapisu z wcześniejszych wydań.

Nie zmieniano implementacji zapisu świata. Nie uznajemy tej wersji za
bezpieczną dla jedynego egzemplarza świata ani całego moda za wolny od błędów.

## JAR

- `crimeandlaw-0.1.49.jar`, 3959324 bajtów.
- SHA-256 `e141ed1a870e79d119e508955f019eb82aa7caa9e443b8321f27de429677a9d0`.
- CRC poprawne, brak duplikatów i klas Smoke.
- 252 klas produkcyjnych i 34 zasoby zgodne bajtowo z kompilacją/źródłami.
- Zwykłe pakowanie bez źródeł testu graficznego; kod wykonawczy po próbie
  nie był zmieniany. Raport danych: [validation-0.1.49.json](validation-0.1.49.json).
