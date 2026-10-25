# Finanse — 0.1.39

Zakładka **Saldo gangu** pokazuje wspólne saldo i własne konto gracza.
**Wpłać** przenosi pieniądze z własnego konta bankowego do gangu; **Wypłać** robi przelew
w przeciwną stronę. Można dodać komentarz. Każda czynność wymaga osobnego prawa
rangi; lider posiada wszystkie działające prawa. Historia salda zawiera także
koszt wypłaconych premii. Zwykły Rejestr zachowuje zdarzenia niefinansowe.

W **Premie → Wypłać premię** wybiera się jednego członka. **Premia masowa**
pozwala zaznaczyć do 100 członków na kolejnych stronach i wpisać wspólną kwotę
na osobę. Widać liczbę odbiorców, łączny koszt i dostępne saldo przed wysłaniem.
Wybór obejmuje członków offline. Lista ma wyszukiwanie; premii nie można
wypłacić samemu sobie. Odbiorcy muszą należeć do gangu w momencie realizacji.
W tej wersji nie ma importowania plików CSV/TXT ani różnych kwot w jednym
formularzu masowym. Zwykłe premie można wypłacać z różnymi kwotami.

Historia ma wyszukiwanie według nadawcy, odbiorcy, typu lub komentarza,
po 100 wpisów na stronę i pionowy pasek przewijania. Zachowuje do 10 000
ostatnich transakcji gangu. Najazd na wiersz pokazuje pełne nazwy, rangi,
kwotę i komentarz. Nazwy i rangi w historii odpowiadają chwili wypłaty.

Konta zapisują się razem ze światem. Pieniądze osobiste pozostają po odejściu
z gangu. Ostatni członek musi wypłacić wspólne saldo przed usunięciem gangu.
Nowe konta mają 0 $. Dochody z handlu i kontrolowanych terenów nie są jeszcze
podłączone. Komenda do testów/administracji:

```
/crimeandlaw pieniadze
/crimeandlaw pieniadze dodaj NazwaGracza 100000
```

Pierwsza pokazuje własne konto. Druga jest dostępna dla operatora (poziom 2),
konsoli lub właściciela świata jednoosobowego. Nie ma publicznej komendy
przyznawania sobie pieniędzy na cudzym serwerze.

Całkowite dolary są roboczą jednostką gry. Kwoty muszą być dodatnie;
maksymalne saldo konta to 1 000 000 000 000 $. Kwoty z ułamkami,
przekroczenie salda, powtórzeni odbiorcy i nieprawidłowe komentarze są
odrzucane przed zmianą kont. Cała premia masowa jest rozliczana atomowo.
Jednorazowy token serwera zapobiega ponownemu rozliczeniu tego samego pakietu.
Klient dostaje własne saldo i wycinek historii gangu, bez cudzych sald osobistych.

Referencje wyglądu: przesłane `Zrzut ekranu 2026-10-21 182047(1).png` (saldo)
i `Zrzut ekranu 2026-10-21 182152(1).png` (premie). Opis premii sprawdzono
24 października 2026 w [oficjalnej wiki Majestic](https://wiki.gta5majestic.com/pl/posts/informacje-ogolne).
Układ dostosowano do zaakceptowanego rozmiaru menu Minecrafta. Kwoty i progi
Majestic nie są przenoszone do autorskiego balansu Crime And Law.

Wersja 0.1.30 wymaga tego samego JAR-u na kliencie i serwerze (protokół 18).
Starszy zapis świata jest odczytywany i migrowany; nie trzeba zakładać nowego świata.

## Osobna gotówka i bank od 0.1.30

Stare pole salda osobistego zachowuje wszystkie środki jako bank. Nie są
kopiowane do gotówki. Nowa gotówka zaczyna od 0 $. Oddzielne komendy
`pieniadze dodaj NazwaGracza gotowka KWOTA` i `pieniadze dodaj NazwaGracza bank KWOTA`
mają te same wymagania uprawnień co dotychczasowe przyznawanie środków.
Nie istnieje akcja przelewu gotówka ↔ bank. Operacje gangu oraz premie
korzystają z banku; komentarz nadal jest opcjonalny.

Dane finansowe należą do świata i UUID, nie do ekwipunku ani klonowanej
encji gracza. Śmierć, respawn, przejście między wymiarami i ponowne wejście
nie naliczają pieniędzy. HUD otrzymuje prywatny pakiet dwóch sald, również
bez otwierania menu i przy otrzymaniu premii przez gracza z zamkniętym panelem.

Walidacja odczytu odrzuca także ułamkowe i przekroczone kwoty w pliku kont,
zamiast pozwalać Gsonowi obcinać je przy konwersji na long. Lista wypłat
jest kopiowana przed sprawdzeniem i wykonaniem całej transakcji.

## Brak odpowiedzi i stare formularze

Jednorazowy token należy do konkretnej sesji gracza i gangu. Nie działa
na cudzym koncie, po zmianie gangu ani po wcześniejszym wykorzystaniu.
Po pięciu sekundach bez potwierdzenia klient prosi o aktualny stan i
unieważnienie niewykonanego żądania. Nie wysyła automatycznie drugiego
przelewu. Po odzyskaniu danych należy sprawdzić historię przed następną
operacją. Obie strony połączenia wymagają wersji 0.1.39 / protokołu 20.
