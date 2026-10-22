# Sprawdzenie 0.1.17

22 października 2026. Minecraft 1.21.1, NeoForge 21.1.248, JDK 21.

## Zakres zmiany

- Mapa: górna ikona powrotu do gracza, bez dolnych strzałek; wyśrodkowane −/+
  i Odśwież; pełna ramka aktywnej Siatki. Przesuwanie nie zmienia wybranego
  chunka, również przy ruchu zbyt krótkim, aby przesunąć widok o cały chunk.
  Opis zaznaczonego terenu zachowuje ostatnie dane po odsunięciu widoku.
- Formularz tworzenia: zwarta karta, podgląd logo i wyróżniona główna czynność.
- Wyrównanie tekstu zależne od rodzaju przycisku; zachowana boczna nawigacja.
- Przełączniki uprawnień i ciągłe przewijanie przeciąganej rangi przy krawędzi,
  także gdy mysz się zatrzyma. Enter respektuje aktywne pole/przycisk.
- Karta członka ze skinem od głowy do torsu, rangą, statusem i datą dołączenia;
  licznik online / wszyscy, zamykanie karty kliknięciem poza nią, X lub Esc.
- Wspólny baner: jednolity kolor z kołem barw/kryciem albo obraz z kadrowaniem
  4:1, wynik PNG 1024×256 do 1,1 MB. Logo pozostaje osobne 512×512.
  Nazwa gangu ma własną ramkę. Osobna zakładka ustawień baneru.
- Zapis i sprawdzanie uprawnień po stronie serwera; zgodna migracja starszych
  światów, rzeczywiste daty nowych członkostw i pamiętanie publicznego profilu skina.

## Testy jednostkowe

79 testów JUnit: 0 błędów, 0 niepowodzeń, 0 pominiętych. Końcowe `build` zakończone sukcesem.
Dodano siedem prób dotyczących proporcji i granic szerokiego kadru,
przezroczystych marginesów, osobnej walidacji logo/baneru, uprawnień,
atomowego odrzucenia błędnego obrazu, migracji bez fikcyjnych dat,
zapisu profilu/dat i ograniczonego przesyłania statusów dużego gangu.
Rozszerzono kodek o równoczesne szczegółowe logo i baner: pełny JSON powyżej
2 mln znaków przechodzi zapis świata i kodek State, zachowując obie grafiki.
Zachowano wcześniejsze testy modelu, rang, rejestru, importu obrazów,
kolorów, skalowania, mapy i granic świata.

## Rzeczywisty renderer Minecrafta

Zaliczone przebiegi `menu17a` (1920×1080), `menu17-1280` i `menu17b-1280`
(1280×720). Po ostatniej korekcie krótkiego paska przewijania wykonano drugi
przebieg 1280×720. Każdy obejmuje 55 zrzutów oraz skale GUI 2 i maksymalną
(4 albo 3). Zaliczony przebieg końcowy `menu17-final` sprawdza także usunięcie szkicu
logo po potwierdzeniu jego zapisu, aby nie zasłaniał przyszłych zmian wspólnych.

Sprawdzono wszystkie dotychczasowe kategorie, import/kadrowanie logo,
prywatność interfejsu, rangi, zachowanie szkicu przy zmianie rozmiaru,
przewijanie rang bez kolejnych ruchów myszy, Enter na zaznaczonym X oraz
w polu nazwy, przyciski mapy i utrzymanie wyboru chunka po przesunięciu.
Nowe etapy obejmują baner koloru/obrazu, dialog szerokiego kadrowania,
pełną teksturę 1024×256, kartę skina, dopasowanie jej do okna i zamykanie.

Wizualnie obejrzano formularz, członków, rangi, mapę, baner i ustawienia.
Część danych jest syntetyczna. Mapa końcowych ujęć pochodzi z rzeczywistej
wioski dedykowanego serwera `terrain16a`; każdy dostępny piksel jej rastra
porównano z teksturą klienta. Dane backendu tej mapy nie zostały zmienione.
W 0.1.16 przeprowadzono jej pierwszy zapis, restarty, przebudowę powierzchni
oraz widoki granic świata bez wczytywania odległego terenu.

## Dwa rzeczywiste klienty przez TCP

`net17a`: dwa klienty Minecrafta 960×540, deweloperskie konta Owner16/Member16,
własny serwer na 127.0.0.1:25597. Wyniki OWNER_PASS i MEMBER_PASS.

- Tworzenie gangu, wyszukiwanie, zgłoszenie, przyjęcie, tworzenie/przypisanie
  rangi i zapisy wyglądu wykonane przez prawdziwe kontrolki gry oraz pakiety.
- Zwykły członek nie dostaje prywatnych zgłoszeń ani narzędzi zarządzania.
  Serwer odrzuca jego pakiety zmiany wspólnego tła i baneru.
- Po nadaniu wyłącznie MANAGE_THEME odblokowuje się zapis wyglądu,
  pozostając bez uprawnień do rang/usuwania członków.
- Logo 512×512: 1 200 288 znaków Base64. Baner 1024×256: 1 199 972 znaki.
  Obie duże grafiki są równocześnie w stanie gangu; faktyczny przesył
  klient → serwer → oba klienty przechodzi, a hashe SHA-256 i rozdzielczości
  tekstur są zgodne. Nie ograniczono próby do małych jednokolorowych PNG.
- Wspólne kolory, licznik dwóch graczy online i rzeczywista data członkostwa
  docierają z serwera. Próba usunięcia właściciela bez uprawnień jest odrzucona.
- Po zamknięciu klientów i poprawnym zapisie/zatrzymaniu serwera ponownie
  uruchomiono ten sam świat. RESTART_PASS: członkowie, właściciel,
  przypisana ranga/uprawnienia, motyw, duże logo/baner i daty zachowane.

Serwer testowy używa trybu headless. Są to lokalne konta deweloperskie,
nie logowanie do usług Mojang. Karta skina była renderowana z domyślnymi
skórkami; integracja korzysta z PlayerInfo/SkinManager i zapamiętanej
właściwości textures. Brak dostępu do usług profili w środowisku oznacza,
że pobrania nowej niestandardowej skórki z Mojang nie zweryfikowano end-to-end.

## Odtwarzanie

```text
./gradlew build
./gradlew -PuiSmoke -PuiSmokeId=menu17-final runClient
./gradlew -PuiSmoke -PuiSmokeId=menu17b-1280 -PuiWidth=1280 -PuiHeight=720 runClient
```

GUI wymaga ekranu/OpenGL i danych `build/server-smoke-terrain16a/view.json`.
Można je przygotować przez dwa uruchomienia
`./gradlew -PserverSmoke -PserverSmokeId=terrain16a runServer`, z EULA
zaakceptowaną tylko dla izolowanego katalogu tej próby.

Dla TCP użyć świeżego identyfikatora i przygotować klasy z `-PnetworkSmoke`.
`runNetworkServer`, `runNetworkOwner`, `runNetworkMember` uruchomić z tym samym
`-PnetworkSmokeId=nazwa` w oddzielnych terminalach, z serwerem jako pierwszym.
Serwer próby: 127.0.0.1:25597, online-mode=false i zaakceptowana EULA.
Po raportach obu klientów zapisać/zatrzymać serwer poleceniem stop;
ponowny start z `-PnetworkPersistence=true` sprawdza trwałość danych.
W tym środowisku użyto przygotowanych przez Gradle parametrów startu,
bez równoległego przebudowywania klas w czasie właściwych prób.

## Wydanie i granice

Protokół 9 wymaga tego samego JAR-a 0.1.17 u klientów i na serwerze.
Klasy automatyzacji z uiTest/serverTest/networkTest są wykluczone z JAR-a.
Biblioteki importu obrazów pozostają dołączone (15 bibliotek).

Dane kartograficzne są wspólne na serwerze, nie mają osobistej mgły odkrycia.
Przesuwanie odczytuje zapisane kafle; nie pobiera nieznanego odległego świata.
Brak przejmowania ziemi, rzadkich aktywnych bonusów, ekonomii, magazynów,
pojazdów, roślin i multibloków. Premie/Saldo/Czarna lista/Cele mają dotychczasowe
panele W przygotowaniu; Transport pozostaje ukrytą definicją.
Nie wykonano benchmarku dużego serwera ani testu dowolnej paczki innych modów.
Import nadal nie obejmuje HEIC/AVIF, PDF ani animowanego logo/baneru.
Lokalne narzędzia uruchomieniowe i konfiguracja Xvfb nie trafiają do wydania.
