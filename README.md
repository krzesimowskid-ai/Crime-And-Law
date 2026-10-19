# Crime And Law

Pierwszy prototyp autorskiego moda gangsterskiego: kompaktowy panel gangów,
członkostwo, rangi i wygląd menu. Wersja **0.1.2**.

## Uruchomienie

- Minecraft **1.21.1**, Java **21**, NeoForge **21.1.248 lub nowszy z serii 21.1**.
- Włóż `crimeandlaw-0.1.2.jar` do `mods`, zastępując poprzedni JAR tego moda.
- Na serwerze plik musi być zainstalowany **u graczy i na serwerze**.
- W świecie jednoosobowym część serwerowa uruchamia się razem ze światem.
- Klawisz **G** otwiera panel; skrót można zmienić w sterowaniu Minecrafta.

## Co obejmuje wersja

Bez gangu: dwie równe zakładki u góry okna — „Stwórz gang” i „Znajdź gang”.
Pod nimi wspólny obszar formularza lub wyszukiwarki, zgodnie ze szkicem użytkownika.
Własna nazwa i opcjonalne logo. Esc zamyka okno.
Zgłoszenia do istniejącego gangu akceptuje lider albo uprawniony członek.

Po dołączeniu: mały nagłówek z logo, nazwą i rangą, wąska kolumna zakładek,
przewijana treść po prawej. Gang, Członkowie, Rangi i Ustawienia mają działającą
logikę. Mapa wyraźnie informuje o planowanym dalszym etapie.

- Lista członków i zgłoszeń, przyjmowanie, odrzucanie, usuwanie.
- Przekazanie przywództwa i opuszczenie gangu.
- Domyślne rangi Lider i Członek oraz własne rangi.
- Uprawnienia do zgłoszeń, usuwania członków, zarządzania rangami i wyglądem.
- Kolory tła/obramowania oraz krycie tła. Lider/uprawniona osoba zapisuje
  wspólny motyw; członek może zapisać osobisty motyw lub wrócić do wspólnego.
- Zapis gangów w świecie i osobistego motywu w konfiguracji klienta.

## Własne logo

Możesz wybrać obraz z okna plików, wkleić jego lokalną ścieżkę albo przeciągnąć
jeden obraz z folderu na otwarty panel gangu w Minecraftcie. PNG i JPG, do 20 MB.
W razie potrzeby przełącz grę w okno klawiszem F11, aby widzieć grę i folder.

Po odczytaniu obrazu otwiera się „Dopasuj logo”. Przeciągaj obraz w podglądzie,
przybliżaj kółkiem myszy lub suwakiem. Możesz wybrać kwadratowy kadr albo cały
obraz z zachowaniem proporcji i przezroczystymi marginesami. „Anuluj”/X/Esc
zachowuje poprzednie logo. „Zastosuj logo” przygotowuje podgląd; zapisz go
przez „Utwórz gang” albo Ustawienia → Logo → „Zapisz logo gangu”.

Obraz jest dopasowywany do 64×64. Przesyłane logo jest sprawdzane przez serwer
jako PNG do 20 KB; pozostali gracze nie potrzebują pliku na swoim komputerze.
Import z linku nie jest jeszcze obsługiwany. W razie błędu pojawia się jego
konkretna przyczyna. Nie zamieniaj rozszerzenia JPG/SVG/WebP na PNG samą zmianą nazwy.

## Ustawienia i kolejność rang

Ustawienia mają kategorie „Wygląd” i „Logo”. Przyciski zmiany tła i obramówki
otwierają małe okno z kołem barw, nasyceniem, jasnością, HEX i RGB. Tło ma
również suwak krycia. Po „Zastosuj” widzisz podgląd menu; wybierz zapis osobisty
lub zapis dla gangu. Możesz wrócić do wspólnego wyglądu gangu.

W zakładce Rangi chwyć rangę na liście po lewej i przeciągnij ją w nowe miejsce.
Można umieścić dowolną rangę nad liderem, również rangę bez uprawnień.
To kolejność wyświetlania: nie zmienia uprawnień ani właściciela gangu.
Porządkowanie wymaga uprawnienia do zarządzania rangami; nowy porządek zapisuje serwer.

## Zapis i zasady prototypu

Dane gangów są w `data/crimeandlaw_gangs.dat` w katalogu świata Overworld.
Osobisty wygląd: `config/crimeandlaw-appearance.json`. Zmiana osobistego
motywu nie zmienia danych serwera. Domyślna granatowo-grafitowa baza z bordowym
akcentem służy pierwszym testom i jest konfigurowalna.

Jeden gang na gracza, 64 członków na gang, maksymalnie 12 rang i 512 gangów
na świat. To limity zasobów prototypu, nie ostateczny balans. Nazwy gangów
mają 3–32 znaki. Uprawnienia są weryfikowane przez serwer. Nie można usunąć
lidera ani nadać rangi lidera przez zwykłą zmianę rangi.

Gdy lider ma innych członków, przed odejściem musi przekazać przywództwo.
Odejście ostatniego członka usuwa pusty gang po potwierdzeniu.

## Budowanie

Z JDK 21:

```text
Windows: gradlew.bat build
Linux/macOS: ./gradlew build
```

Wynik: `build/libs/crimeandlaw-0.1.2.jar`.
Gradle Wrapper 9.2.1, ModDevGradle 2.0.148, NeoForge 21.1.248.
`./gradlew test` uruchamia testy modelu gangów, walidacji logo oraz proporcji menu.

Test graficzny z przykładowymi danymi: `./gradlew -PuiSmoke runClient`.
Wymaga działającego ekranu/OpenGL. Otwiera widoki menu i nowe edytory w prawdziwym kliencie,
sprawdza kliknięcia, wpisywanie, wybór kolorów, kadrowanie oraz przeciąganie rang, zapisuje obrazy i raport w `build/ui-smoke`,
po czym zamyka klienta. Kod z `src/uiTest` jest wyłączony w normalnym buildzie.
Test nie łączy dwóch graczy i nie zastępuje testu sieciowego.

## Nowe w 0.1.2

- Import obrazu przez przeciągnięcie, okno wyboru i ścieżkę; edytor kadrowania.
- Kategorie ustawień, koło kolorów, jasność, nasycenie, krycie oraz HEX/RGB.
- Przeciąganie rang z zapisem kolejności i bez zmieniania uprawnień.
- Dopasowany nagłówek i ramka nazwy, wyraźniejsze przyciski, usunięty napis „Esc”.

## Poprawki 0.1.1

- Układ tworzenia i wyszukiwania według nowego szkicu, bez bocznego menu na tym ekranie.
- Efekt rozmycia tła nie nakłada się na tekst interfejsu.
- Wielkość okna i kontrolek jest niezależna od globalnej skali GUI Minecrafta.
  Współrzędne kliknięć, przeciągania i przewijania uwzględniają skalę panelu.
- Przycisk utworzenia gangu jest widoczny bez przewijania.

## Dalszy zakres

Mapa, własność chunków, oznaczenia, NPC, produkcja, handel oraz rozbudowane
animacje pozostają do wykonania. Ta wersja nie dodaje tych systemów.
Makieta była referencją układu; elementy zmniejszono zgodnie z uwagą użytkownika.

