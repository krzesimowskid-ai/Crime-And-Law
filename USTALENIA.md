# Crime And Law — ustalenia projektu

Stan rozmowy: 17 października 2026, godz. 08:41, Europe/Warsaw.

## Status i sposób dalszej pracy

To zapis pomysłów i decyzji użytkownika, a nie opis gotowego moda. Na moment zapisu repozytorium nie zawierało implementacji. Użytkownik poprosił o zachowanie ustaleń **bez rozpoczynania kodowania**; chce teraz omówić i przeanalizować osobny mod tworzony wcześniej z innym AI.

Crime And Law jest nowym, osobnym modem. McArk pozostaje odłożony, zachowując cel odwzorowania ASA 1:1. Nie zmieniać projektu McArk w Crime And Law ani nie usuwać jego materiałów w ramach tej pracy.

Użytkownik może przypominać sobie pomysły stopniowo. Nie traktować opisu jako zamkniętej specyfikacji. Zachowywać pomysły, nie zmieniać ich bez pytania i zadawać najwyżej jedno konkretne pytanie naraz. Propozycje asystenta nie stają się ustaleniami bez akceptacji użytkownika.

## Kierunek i atmosfera

- Autorski mod do Minecrafta w gangsterskim klimacie.
- Własne AI dla NPC; jego szczegółowy zakres pozostaje do określenia.
- Ważne są obsługa urządzeń, animacje, modele, dźwięki i atmosfera prowadzenia laboratorium.
- Użytkownik chce złożonego procesu angażującego gracza, a nie pojedynczej wymiany składników na gotowy produkt w jednym oknie craftingu.
- Wydajność jest ważna od początku.
- Minecraft 1.21.1 z NeoForge został zaproponowany przez asystenta jako platforma startowa; nie zatwierdzono jeszcze wersji nowego projektu.

## Materiały, produkty i maszyny

Nowsze podsumowanie przekazane przez użytkownika zastępuje wcześniejsze rozważania o rzeczywistych recepturach:

- Produkty i receptury mają być fikcyjne, niezwiązane z prawdziwymi narkotykami ani rzeczywistą chemią.
- **Furonatamina** jest przykładową fikcyjną nazwą, nie zatwierdzoną nazwą pierwszego produktu.
- Użytkownik chce przedmiotów reprezentujących cały układ okresowy. Mogą mieć rzeczywiste nazwy i symbole, np. węgiel, sód, azot i tlen.
- Węgiel jako pierwiastek ma być osobnym przedmiotem względem minecraftowego węgla.
- Worek, pudełko lub pojemnik jest umowną jednostką gry i nie oznacza rzeczywistej ilości.
- Temperatura może być wyrażana w **termach ™**, a ciśnienie w **barytach (Bs)**. Są to abstrakcyjne parametry bez przelicznika na rzeczywiste jednostki.
- Duże maszyny multiblokowe mają mieć wejście na surowce i wyjście na produkt.
- Obsługa może obejmować minigry i regulację parametrów jako fikcyjne mechaniki gry.
- Użytkownik chce także odprowadzania dymu/oparów. Dokładne urządzenia, połączenia i reguły symulacji nie są jeszcze ustalone.
- Użytkownik chciał efektów działania produktów na postać. Konkretne profile, czasy, wartości, tolerancja i uzależnienie nie zostały zatwierdzone.

## Handel

- Użytkownik rozważa sprzedaż we wioskach: wieśniakom albo specjalnym handlarzom/dilerom NPC.
- Nie zapadła ostateczna decyzja, komu dokładnie i w jaki sposób się sprzedaje.
- Osobni handlarze z budżetem, popytem, reputacją, pamięcią transakcji i oceną jakości to propozycja asystenta.
- Waluta, ceny i wartości ekonomiczne pozostają do ustalenia.

## Panel gangów

Pomysły użytkownika:

- Jeden klawisz otwiera panel zależnie od członkostwa w gangu. Konkretny klawisz nie został wybrany.
- Gracz bez gangu widzi opcje/zakładki **„Stwórz swój gang”** oraz **„Znajdź gang”**.
- Wyszukiwanie umożliwia znalezienie i dołączenie do gangów innych graczy w danym świecie/na serwerze. Nie ustalono listy między różnymi serwerami.
- Przy tworzeniu gangu ustala się nazwę i własne logo; opis jest rozważaną opcją.
- Logo ma umożliwiać własny obrazek, nie jedynie wybór z gotowych wzorów.
- Import obrazu z komputera i z linku to pomysły do technicznego sprawdzenia, a nie potwierdzone działające funkcje.
- Po utworzeniu lub dołączeniu ten sam klawisz otwiera menu aktualnego gangu.
- Jedną z zakładek menu ma być mapa świata pokazująca m.in. kontrolowane obszary.
- Rangi, uprawnienia, zaproszenia, akceptowanie zgłoszeń, limity członków i koszty utworzenia nie zostały określone.

## Gangi NPC — pomysł na później

- Użytkownik rozważa gangi złożone z NPC i mające własne AI.
- Do takich gangów gracz nie może dołączać; miałyby być przeciwnikami.
- Gangi i ich budowle mają być zróżnicowane.
- Rozważane są gangi na powierzchni i ukryte pod ziemią.
- To kierunek do późniejszego rozwinięcia, nie zadanie implementacyjne na teraz.
- Sposób walki, skład gangów, zachowania AI, generacja baz i system ich odnajdywania pozostają otwarte.

## Terytoria

- Podstawową jednostką jest chunk o powierzchni 16 × 16 bloków.
- Własność obejmuje całą wysokość chunka: od najniższego do najwyższego poziomu świata. Użytkownik nie chce osobnej własności powierzchni i podziemia w tej samej kolumnie.
- Jeżeli choć fragment wioski leży w chunku, cały chunk zalicza się do jej obszaru. Sposób technicznego rozpoznawania granic wioski nie został wybrany.
- Neutralny, niezajęty teren można przejąć za opłatą. Wartość i waluta nie są ustalone.
- Wioski i zajęte tereny wymagają innego sposobu przejęcia niż zakup neutralnej ziemi.
- Walka z mieszkańcami wioski jest luźnym pomysłem użytkownika; nie zatwierdzono jeszcze warunków przejmowania.
- Nie ustalono odległości między działkami, wymogu sąsiedztwa, limitu terytorium, utrzymania ani zasad ochrony przed utratą terenu.

## Widoczność i odkrywanie ukrytych terytoriów

- Gangi na powierzchni mają widoczne budynki, a przynależność ich terenu jest normalnie widoczna.
- Teren ukrytego podziemnego gangu początkowo wygląda na mapie jak neutralny.
- Próba zakupu takiego chunka zostaje odrzucona. Komunikat informuje, że teren jest zajęty przez gang.
- Odmowa nie ujawnia automatycznie położenia bazy ani wejścia do niej.
- Gra **nie obrysowuje automatycznie** ukrytego terytorium ani nie zaznacza go za gracza. Użytkownik wyraźnie doprecyzował ręczne oznaczanie.
- Gracz może sam wyciągać wnioski ze sprawdzania kolejnych chunków.
- Dokładna treść komunikatu i ujawnianie nazwy ukrytego gangu nie zostały doprecyzowane.

## Ręczne oznaczenia na mapie

Potwierdzone przez użytkownika:

- Zakładka/opcja **„Zaznacz”** pozwala graczowi samodzielnie oznaczyć teren, np. podejrzewaną lokalizację ukrytego gangu.
- Kolor wybiera się płynnie z pełnego **koła barw/palety**, nie z kilku gotowych próbek.
- Są **oba zakresy widoczności**: **„Tylko ja”** oraz **„Mój gang”**.
- Oznaczenie jest notatką na mapie, nie zmianą własności.

Do rozważenia, bez zatwierdzenia:

- Wyświetlanie autora wspólnego oznaczenia.
- Uprawnienia do edycji/usuwania według rang.
- Nazwy i opisy znaczników, przezroczystość, dokładny sposób zaznaczania i zakres ujawnionej mapy.

Użytkownik lubi rozważanie obu wariantów przy dylematach. Nie oznacza to automatycznego zatwierdzania wszystkich przedstawianych opcji; konkretne decyzje nadal należy uzgadniać.

## Propozycja pierwszego prototypu — jeszcze bez realizacji

Asystent zaproponował: podstawę moda, jeden multiblok, jeden fikcyjny proces, prosty układ oparów, jednego handlarza, pieniądze, zapis oraz JAR do testów. To propozycja kolejności pracy, nie zatwierdzenie wyglądu, receptury, wymiarów maszyny ani parametrów.

Najbliższa dyspozycja użytkownika: **zachować ten opis bez implementowania moda i umożliwić osobną analizę innego, wcześniej tworzonego projektu, gdy zostanie wskazany.**
