# Aurelia — doniczki, wyposażenie i zbiór

Wersja 0.1.44. Minecraft 1.21.1, NeoForge 21.1.248, Java 21.
Protokół **24**: ten sam JAR na serwerze i u graczy.
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
   i przeciągnij po całym oznaczeniu**. Widać ostrze i odchylający się brzeg.
   Otwieranie nie zużywa ziemi. Ze stosu otwiera jedną sztukę; pozostałe
   pozostają zamknięte. Rozcięty większy worek zachowuje otwarcie i licznik.
5. Po rozcięciu **złap worek LPM** i przesuń go nad doniczkę. **A/D**
   obraca w lewo/prawo, **W/S** przechyla przód/tył. Dopiero odpowiednie
   pochylenie wysypuje ziemię. Strumień jest liczony z rzeczywistego otworu
   modelu; nietrafienie nie zapełnia doniczki. Po puszczeniu LPM worek
   pozostaje w miejscu, a nasypywanie staje. Można chwycić go ponownie.
6. Trafiające cząstki stopniowo wypełniają zwężającą się komorę. Powierzchnia
   i grudki mieszczą się wewnątrz ścianek także na początku napełniania.
   Po pełnym napełnieniu serwer pobiera **jedno pełne użycie worka**;
   duży nadal przechodzi 3 → 2 → 1 → zużyty.
7. Kliknij „Przygotuj sadzenie”. Przy zamkniętym woreczku najpierw otwórz
   szew w minigrze. **Chwyć nasiono LPM, przesuń nad dołek i puść**.
   Nasiono opada pod wpływem grawitacji. Nietrafione można podnieść ponownie;
   nie zużywa się przedmiotu za samo upuszczenie poza doniczką.
8. Po opadnięciu **przytrzymaj LPM na ziemi i kilkoma ruchami przysuń ją
   nad nasiono**. Dołek się zamyka; dopiero zatwierdzone zasypanie zużywa
   jedną sztukę. Bezpośrednie użycie woreczka zostawia cztery luźne nasiona.
   Nadal można poza oknem rozpakować całe opakowanie PPM na pięć sztuk.
9. Wybierz „Weź konewkę”, złap LPM i ustaw strumień jak przy worku.
   A/D przechyla, W/S kieruje przód/tył. Kropelki wychodzą z wylewki
   i poruszają się razem z nią. Po puszczeniu konewka nie upada. Udane
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

UV umożliwia wzrost w ciemnym wnętrzu. Widoczny fiolet pochodzi z własnego
lokalnego oświetlenia modeli i poświaty na dostępnych górnych powierzchniach
otoczenia. Nie wymaga paczki shaderów; logiczny poziom światła Minecrafta
pozostaje oddzielny od koloru. To lokalny efekt stanowiska, a nie zmiana
całego silnika Minecrafta na oświetlenie RGB.

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
liść i jeden punkt zużycia nożyc. Zwykłe nożyce nie uruchamiają minigry.

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

Wyniki i granice testów: [VALIDATION_0.1.44.md](VALIDATION_0.1.44.md).
