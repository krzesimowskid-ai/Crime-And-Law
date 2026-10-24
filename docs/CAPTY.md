# Capty i zegar świata — 0.1.25

Minecraft 1.21.1, NeoForge 21.1.248, Java 21. Protokół 15; aktualizacja
serwera i wszystkich klientów jest wymagana.

## Przebieg

Lider aktywuje Kontrolę terytoriów w G → Mapa. Aktywacja na tym etapie
jest bezpłatna: ekonomia dolarów i reputacji nie jest jeszcze wdrożona.
Pierwszy teren w danym wymiarze można wybrać dowolnie. Następne chunki
muszą sąsiadować bokiem z własnym terytorium. Wymagane jest wcześniejsze
rozpoznanie terenu istniejącego świata; mapa nie generuje nowych chunków.

Uprawniony gracz staje w wybranym chunku w trybie przetrwania lub przygody
i potwierdza start. Obecni, uprawnieni członkowie gangu są automatycznie
zapisywani. Teren neutralny przejmowany jest po czasie, bez NPC do obrony.
Brak wymaganej obecności zatrzymuje postęp; po okresie tolerancji anuluje akcję.

Dołączenie rywala uruchamia przygotowanie do PvP. Atak na zajęty teren
rozpoczyna się od przygotowania, podczas którego obrońca może się zapisać.
Po przygotowaniu zapisy są zamknięte. Śmierć eliminuje z tego wydarzenia,
także po odrodzeniu lub zmianie gangu. Wygrywa ostatni gang z żywymi
uczestnikami, który utrzyma obecność w strefie przez wymagany czas.
Minimum startowe nie odbiera szansy ocalałym po stratach w PvP.
Przekroczenie czasu walki albo brak ocalałych daje remis i zachowuje
poprzednią własność. Własność obejmuje całą kolumnę chunka.

Jeden gang uczestniczy w jednym capcie naraz. Nowe ataki są blokowane poza
oknem godzin, ale trwające wydarzenie może się zakończyć. Capty są oznaczone
złotą obramówką mapy. Uczestnicy mają HUD z fazą i czasem oraz kolorowe nazwy
gangu i symbol sojusznika/wroga nad postaciami. Brak obrażeń PvP od graczy
niezapisanych do wydarzenia, friendly fire i PvP przed fazą walki.

## Domyślne ustawienia

| Parametr | Wartość |
| --- | --- |
| Godziny rozpoczynania | 16:00–22:00 w strefie serwera |
| Przejęcie neutralnego terenu | 300 sekund |
| Przygotowanie do PvP | 60 sekund |
| Utrzymanie po eliminacji rywali | 30 sekund |
| Maksymalna walka PvP | 900 sekund |
| Tolerancja nieobecności | 30 sekund |
| Cooldown gangu po wydarzeniu | 3600 sekund |
| Ochrona zdobytego chunku | 86400 sekund |
| Minimum startowe | 1 uczestnik, dla rozgrywki solo |

Obrońca ma cały czas przygotowania na zapis. Cooldown i ochrona korzystają
z rzeczywistych dat, również między restartami. Restart przerywa niedokończone
capty i nakłada cooldown, bez przyznawania terenu za czas poza uruchomioną grą.
Limity techniczne: 32 równoległe wydarzenia, osiem gangów i 64 graczy na wydarzenie.

## Czas i komendy

`/crimeandlaw czas Europe/Warsaw` wybiera wspólną strefę i włącza dobę 24 h.
Tab podpowiada dostępne strefy, np. `America/New_York` i `Asia/Tokyo`.
`/crimeandlaw czas` pokazuje ustawienia; `/crimeandlaw czas off` przywraca
poprzedni stan `doDaylightCycle` i normalny zegar Minecrafta.

Polecenia administracyjne są dla konsoli, operatora poziomu 2 lub właściciela
świata jednoosobowego. Nie trzeba wpisywać UUID właściciela do konfiguracji.
Domyślna strefa pochodzi z systemu uruchamiającego świat. Mod korzysta
z zegara systemowego i bazy stref Javy; nie wymaga internetu ani geolokalizacji.
Zmiany czasu letniego wynikają ze strefy. Świt przypada o 06:00, południe
o 12:00. Ruch, walka i maszyny zachowują zwykłe tempo. Sen nie przeskakuje zegara.

`/crimeandlaw capt` pokazuje zasady. `/crimeandlaw capt parametr liczba`
zmienia parametr. Czasy podaje się w sekundach, godziny jako liczby godzin.
Parametry: `otwarcie`, `zamkniecie`, `przejecie`, `przygotowanie`, `utrzymanie`,
`walka`, `nieobecnosc`, `cooldown`, `ochrona`, `uczestnicy`.
Przykład: `/crimeandlaw capt uczestnicy 5`. Otwarcie i zamknięcie o tej samej
godzinie oznacza dostęp całodobowy. Nowe zasady dotyczą nowych wydarzeń;
trwające zachowują swój zestaw zasad.

## Łupy i oznaczenia

Pozostałe na ziemi przedmioty z pierwszej śmierci zapisanych uczestników
zbierane są po zwycięstwie do trwałego depozytu zwycięzcy. System rozpoznaje
konkretny capt i nie zabiera wcześniej leżących, obcych itemów. Mixin zapobiega
łączeniu łupów aktywnego wydarzenia z identycznymi, obcymi stosami.
Ręcznie zebrane albo wyniesione poza chunk przedmioty nie powstają ponownie.
Remis nie zbiera łupów. Depozyt mieści 1024 stosy; nadmiar pozostaje na ziemi.

`/crimeandlaw lupy` przenosi do ekwipunku tyle łupów, ile się zmieści.
Reszta pozostaje zapisana. Wymaga prawa „Dostęp do magazynu / depozytu łupów”.
Fizyczny magazyn w bazie pozostaje do podłączenia w kolejnym etapie.

„Zaznacz” pozwala wybrać zakres: tylko dla mnie albo dla gangu, a następnie
kolor na kole. Oba oznaczenia mogą współistnieć i można je osobno usunąć.
Prywatne oznaczenia nie są wysyłane innym graczom. Limit to 256 na gracza
oraz 256 na gang, z trwałym zapisem w świecie. Kolor własności i oznaczeń
PvP można zmienić w Wygląd → Kolory menu, niezależnie od osobistego motywu.

Poziomy gangów, wypłaty w dolarach i kwalifikowanie bonusów produkcyjnych
pozostają osobnymi etapami. Samo zdobycie chunka nie przyznaje premii.
