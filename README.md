# Crime And Law

Pierwszy prototyp autorskiego moda gangsterskiego: kompaktowy panel gangów,
członkostwo, rangi i wygląd menu. Wersja **0.1.0**.

## Uruchomienie

- Minecraft **1.21.1**, Java **21**, NeoForge **21.1.248 lub nowszy z serii 21.1**.
- Włóż `crimeandlaw-0.1.0.jar` do `mods`.
- Na serwerze plik musi być zainstalowany **u graczy i na serwerze**.
- W świecie jednoosobowym część serwerowa uruchamia się razem ze światem.
- Klawisz **G** otwiera panel; skrót można zmienić w sterowaniu Minecrafta.

## Co obejmuje wersja

Bez gangu: „Stwórz gang” i „Znajdź gang”. Własna nazwa i logo.
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

Wpisz lokalną ścieżkę PNG, np. `C:\Users\Gracz\Pictures\logo.png`,
i kliknij „Wczytaj PNG”. Obraz jest dopasowywany z zachowaniem proporcji
oraz przezroczystości do 64×64. Następnie utwórz gang albo zapisz jego logo.
PNG źródłowy: do 5 MB i 4096×4096. Przesyłane logo: do 16 KB i 64×64.
Serwer sam sprawdza format, rozmiar i zawartość obrazu; inni gracze nie
potrzebują pliku na dysku. Import z linku nie jest jeszcze obsługiwany.

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

Wynik: `build/libs/crimeandlaw-0.1.0.jar`.
Gradle Wrapper 9.2.1, ModDevGradle 2.0.148, NeoForge 21.1.248.
`./gradlew test` uruchamia testy modelu gangów i walidacji logo.

## Dalszy zakres

Mapa, własność chunków, oznaczenia, NPC, produkcja, handel oraz rozbudowane
animacje pozostają do wykonania. Ta wersja nie dodaje tych systemów.
Makieta była referencją układu; elementy zmniejszono zgodnie z uwagą użytkownika.
