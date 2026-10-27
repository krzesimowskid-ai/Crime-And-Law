# Aurelia — doniczki, wyposażenie i zbiór

Wersja testowa 0.1.50. Minecraft 1.21.1, NeoForge 21.1.248, Java 21.
Protokół **27**: ten sam JAR na serwerze i u graczy.
Aurelia jest fikcyjną rośliną; liczby są bilansem gry.

## Skąd wziąć wyposażenie

Ogrodnik jest zwykłym sklepem NPC. Kliknij go PPM, przewiń katalog i kup
przedmioty za **gotówkę**. Bank i saldo gangu nie są obciążane. Nie ma
sprzedaży plonów ani systemu zawodów. Administrator lub właściciel świata
jednoosobowego ustawia NPC przed sobą poleceniem:

```
/crimeandlaw uprawy sprzedawca
```

Dostępne jest też jajo ogrodnika w zakładce jaj przyzywających. Robocze ceny:

| Produkt | Cena | Zawartość |
|---|---:|---|
| Nasiono aurelii | 60 $ | 1 roślina |
| Woreczek nasion | 270 $ | 5 nasion |
| Sadzonka | 90 $ | 1 roślina, początkowy wzrost 20% |
| Mały worek ziemi | 20 $ | 1 pełne napełnienie |
| Średni worek ziemi | 38 $ | 2 pełne napełnienia |
| Duży worek ziemi | 54 $ | 3 pełne napełnienia |
| Doniczka | 100 $ | Sprzęt wielokrotnego użytku |
| Stalowy stelaż | 250 $ | Dwa bloki, jedno stanowisko |
| Lampa UV | 400 $ | Jedna doniczka w tym stelażu |
| Stalowa konewka | 100 $ | Pusta, pojemność 400 |
| Nożyce do przycinania | 80 $ | 192 poprawne cięcia |

Przykład środków testowych (administrator/właściciel świata):
`/crimeandlaw pieniadze dodaj NazwaGracza gotowka 2000`.
Ceny, czas wzrostu, pięć nasion w opakowaniu i próg ekspozycji UV to robocze
wartości wykonawcze, które można później zbilansować.

## Przygotowanie stanowiska

1. Ustaw ciemną plastikową doniczkę samodzielnie lub stalowy stelaż na dwa
   bloki. PPM montuje doniczkę na dole i UV u góry. Poprzeczka, obejmy
   i dwa pionowe uchwyty tworzą widoczne połączenie lampy z ramą.
2. Weź konewkę i napełnij PPM przy źródle wody. Pełna ma 400 jednostek;
   pusta doniczka przyjmuje 100. Model w ręce jest mniejszy i odsunięty
   od środka ekranu, z widocznym uchwytem.
3. Spójrz na doniczkę i naciśnij **Z** lub PPM. Wybierz materiał strzałkami
   albo kółkiem myszy nad listą.
4. Kliknij „Rozetnij worek”. **Przytrzymaj LPM przy kropce na złotym szwie
   i przeciągnij po całym oznaczeniu**. Ostrze jest widoczne przy szwie i przesuwa się razem z postępem cięcia.
   Otwieranie nie zużywa ziemi. Ze stosu otwiera jedną sztukę; pozostałe
   pozostają zamknięte. Rozcięty większy worek zachowuje otwarcie i licznik. Okno wyjaśnia,
   że już otwartego worka nie rozcina się ponownie przy drugim/trzecim użyciu.
5. Po rozcięciu **złap worek LPM** i przesuń go nad doniczkę **myszą**.
   Trzymając LPM, **A/D przechyla worek na boki, W/S w przód i w tył**.
   Obrót jest ręczny; sam chwyt nie zmienia kąta. Ziemia wypada z piętnastu
   miejsc przed krawędzią otworu, razem z jego obrotem i położeniem.
   Widoczne drobiny nie pozostają wewnątrz obracanego korpusu.
   Obrzeże, wewnętrzne/zewnętrzne ścianki i dno doniczki zatrzymują drobiny;
   kontakt jest sprawdzany również pomiędzy klatkami.
   Serwer zalicza tylko część trafiającą do komory, a nie cały strumień
   po trafieniu jego środka. Po puszczeniu LPM worek pozostaje w miejscu,
   zachowuje kąt i przestaje sypać. Można chwycić go ponownie.
6. Trafiające cząstki stopniowo wypełniają zwężającą się komorę. Powierzchnia
   ma drobny, łagodny wzór ziemi; usunięto wystające brązowe kostki.
   Po pełnym napełnieniu serwer pobiera **jedno pełne użycie worka**;
   duży nadal przechodzi 3 → 2 → 1 → zużyty.
7. Kliknij „Przygotuj sadzenie”. Przy zamkniętym woreczku najpierw otwórz
   szew w minigrze. **Chwyć nasiono LPM i celuj kursorem w dołek**.
   Jedna obręcz leży na nachylonej powierzchni dołka, a pionowy wskaźnik pokazuje miejsce upadku; blisko dołka działa
   pomoc w ustawieniu. Puść LPM — małe, owalne nasiono opadnie do środka. Nietrafione można podnieść ponownie;
   nie zużywa się przedmiotu za samo upuszczenie poza doniczką.
8. Po opadnięciu **przytrzymaj LPM na ziemi i kilkoma ruchami przysuń ją
   nad nasiono**. Dołek się zamyka; dopiero zatwierdzone zasypanie zużywa
   jedną sztukę. Kursor ma teraz własny model szarej rękawicy z palcami.
   Bezpośrednie użycie woreczka zostawia cztery luźne nasiona.
   Nadal można poza oknem rozpakować całe opakowanie PPM na pięć sztuk.
9. Wybierz „Weź konewkę”, złap LPM i ustaw strumień jak przy worku.
   Mysz przesuwa, WASD obraca ręcznie, bez automatycznego przechyłu. **Dziewięć cienkich
   strużek** stopniowo wychodzi z poszczególnych otworów sitka.
   Wylatująca woda porusza się od miejsca emisji; nie pojawia się od razu
   jako nieruchomy łuk sięgający ziemi. Woda uderza w ścianki i obrzeże
   doniczki, zamiast przenikać na drugą stronę.
   Serwer zalicza tylko trafiającą część strumieni. Po puszczeniu konewka nie upada. Udane
   podlanie pobiera tylko tyle wody, ile przyjęła doniczka.

PPM na tle obraca widok. Esc lub „Zamknij” zachowuje zatwierdzone etapy.
Przerwanie niepełnego wsypywania lub sadzenia nie daje plonu ani nie pobiera
materiału. Otwarcie worka zapisuje się osobno od jego zawartości i nie
zamienia całego stosu w rozcięte worki. Nasiono jest obiektem podlegającym
grawitacji w minigrze; worek i konewka są ręcznie ustawianymi narzędziami.

Nasiona nie sadzą na zaoranej ziemi; nie ma darmowej receptury ani zwrotu
nasion z niszczonej rośliny.

## Wzrost i plon

Pełen wzrost od nasiona zajmuje **15 minut aktywnego wzrostu** przy
20 tickach/s. Sadzonka zaczyna od 20%. Potrzebne są woda i światło:
naturalne światło dzienne albo zamontowane UV. Brak wody/światła wstrzymuje
wzrost. Niezaładowane chunki nie naliczają wzrostu w tle.

Roślina pobiera 1 jednostkę wody na 180 ticków wzrostu. Pełna doniczka
wystarcza na pełen cykl od nasiona; nie trzeba podlewać ponownie tylko
po to, aby zebrać już dojrzałą roślinę.

| Warunki podczas wzrostu | Losowany plon |
|---|---|
| Zwykłe światło | 3–6 liści, równe szanse |
| UV przez co najmniej 80% rzeczywistego wzrostu | 4–8 liści, równe szanse |

UV umożliwia wzrost w ciemnym wnętrzu. Oprawa daje własne fioletowe
światło skierowane w dół, spod świetlówek na roślinę i podłogę. Miękka
wiązka słabnie ku brzegom i z odległością; nad oprawą nie ma emisji.
Pełne, nieprzezroczyste bloki zasłaniają wiązkę. Jest to bezpośrednie
światło, bez odbijania od ścian i bez obchodzenia przeszkód.

Liście i inne małe modele próbkują światło przy swojej powierzchni;
nie używają odsunięcia o prawie pół bloku przeznaczonego dla pełnych bloków.
Zapobiega to przenoszeniu punktu oświetlenia górnego liścia ponad lampę.

Kolor oświetla tekstury materiałów przed mgłą; nie jest filtrem gotowego
obrazu. HUD zachowuje własne kolory. Usunięto reset wygładzania przy
aktualizacji tekstury światła oraz niepotrzebne odświeżanie niezmiennego
pola. Kolejność już wybranych lamp jest stała przy przechodzeniu między
nimi. Renderer uwzględnia osiem najbliższych lamp, bez generowania chunków.
Zasięg bezpośredniej wiązki wynosi do siedmiu bloków; jej faktyczny ślad
zależy od wysokości powierzchni pod lampą. Cienie częściowych kształtów są
przybliżone. Nie potrzeba zewnętrznej paczki shaderów.

UV nie zwiększa zwykłej jasności blokowej po stronie serwera; Aurelia
wykrywa zamontowaną lampę bezpośrednio. Inne mechaniki wymagające
waniliowego poziomu światła nie otrzymują od niej białego źródła.

UV nie daje dodatkowego przyspieszenia
ani premii dla sąsiednich doniczek. W tej wersji nie ma zużycia lampy ani
sieci elektrycznej. Włożenie lampy tuż przed zbiorem nie zmienia plonu.
Losowanie następuje raz przy dojrzewaniu; liczba liści, maska pozostałych,
ekspozycja i tożsamość rośliny zapisują się ze światem.

## Zbiór i kolejny cykl

Weź specjalne nożyce do głównej ręki, spójrz na dojrzałą roślinę i naciśnij
Z. Możesz też przejść do zbioru przyciskiem z przygotowania. Wybieraj
**dowolne widoczne liście**: kliknij liść lub znacznik przy nasadzie,
przytrzymaj LPM i puść w zielonym polu. Każde poprawne cięcie daje jeden
liść i jeden punkt zużycia nożyc. Liście mają łagodną, płytką krzywiznę zamiast ostrych załamań.
Odcięte liście mają różne kierunki lotu, grawitację, kołysanie i obrót
wokół własnego środka. Zachowują kształt przy cięciu i spadają poniżej
panelu aż cała sylwetka opuści dolną krawędź widoku Minecrafta.
Nie zatrzymują się na umownej podłodze ani nie znikają po stałym czasie. Animacja nie daje
dodatkowych przedmiotów. Zwykłe nożyce nie uruchamiają minigry.

Przeciąganie tła/PPM obraca roślinę, kółko zmienia zbliżenie. Esc zachowuje
pozostałe liście. Przy pełnym ekwipunku nagroda wypada dokładnie raz na ziemię.
Po ostatnim liściu **roślina i ziemia są zużyte**. Doniczka, stelaż i UV
zostają; kolejne sadzenie wymaga nowego użycia worka i nowego nasiona.

Shift + PPM pustą ręką wyjmuje UV z górnej części lub pustą doniczkę
z dolnej. Doniczki z podłożem nie wyjmuje się tą metodą. Zniszczenie sprzętu
usuwa uprawę i zużyte materiały; oddaje sam sprzęt, bez nasion, ziemi czy
nagrody za pominiętą minigrę. Obie części stelaża mają wspólny stan.

Nożyce zachowują również recepturę: trzy sztabki żelaza, dwie miedzi i patyk.

| | | |
|---|---|---|
| pusto | żelazo | żelazo |
| pusto | miedź | żelazo |
| miedź | pusto | patyk |

Dawne rośliny w istniejącym świecie zachowują częściowy, sześciolistny
zbiór. Po ostatnim liściu znikają; nie odrastają i nie oddają nasion.
Do testów administrator ma `/crimeandlaw uprawy dojrzej` po spojrzeniu na
zasadzoną doniczkę. Zwykły gracz nie może ominąć czasu wzrostu.

## Grafika i zapis

Sprzęt ma własne pikselowe materiały 16×16 oraz drukowane etykiety 32×48 i przestrzenną geometrię:
pusty środek doniczki, rant, szwy worków, licznik pojemności, uchwyt oraz
wylewkę konewki, stalowe łączenia i świetlówki UV. Roślina i nożyce
zachowują własne materiały 32×32 oraz schodkowe ścianki z poprzedniej wersji.
Filtr nearest zachowuje piksele. Materiały powierzchni nie są zlepkiem
tekstur bloków Minecrafta. Katalog sklepu przewija się, a okna skalują.

Przygotowanie i zbiór mają wyłączną rezerwację stanowiska. Serwer sprawdza
zasięg, widoczność, ochronę miejsca, źródłowy przedmiot i jego zawartość.
Jednorazowe rewizje żądań i wyceny sklepu nie pozwalają ponawiać skutków
starym pakietem. Po trzech sekundach bez potwierdzenia klient odzyskuje
stan bez ponawiania wydatku ani nagrody.

Wyniki i granice testów: [VALIDATION_0.1.50.md](VALIDATION_0.1.50.md).
