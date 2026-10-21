# Sprawdzenie 0.1.12

21 października 2026. Minecraft 1.21.1, NeoForge 21.1.248, JDK 21.

## Zmiana

Przywrócono własne rangi: tworzenie i edycja, cztery uprawnienia, przypisywanie
członkom, kolejność zmieniana przeciąganiem. Pozycja na liście nie zmienia
uprawnień ani własności. Serwer sprawdza uprawnienia dla każdej operacji.
Tylko lider przekazuje przywództwo; jego rola jest chroniona. Zachowano
roboczy limit dawnego edytora: 12 rang, bez limitu członków i zgłoszeń.

Zapis bez rang dostaje „Lider”/„Członek” oraz przypisania według właściciela.
Zapis zawierający dawne własne rangi zachowuje je. Prywatne zgłoszenia trafiają
tylko do osób z INVITE lub lidera; odebranie uprawnienia wycofuje je z kolejnego
snapshotu. Zdarzenia rang trafiają do wspólnego rejestru. Protokół sieciowy 5
wymaga aktualizacji moda na kliencie i serwerze.

Z obu formularzy logo usunięto pole ścieżki i „Wczytaj i dopasuj”. Pozostają
systemowy wybór pliku i przeciąganie. Usunięto formaty oraz tekst o opcjonalnym
logo i późniejszej zmianie. Kadrowanie, bezstratny PNG 512×512, limit 1,1 MB,
liniowe skalowanie, duże obrazy źródłowe i kodeki pozostają dostępne.

## Testy automatyczne

54 testy JUnit, zero błędów i pominiętych testów. Nowe sprawdzenia obejmują:

- Tworzenie, edycję, przypisywanie i zapis własnej rangi wraz z uprawnieniami
  oraz zdarzeniami rejestru.
- Delegowane zgłoszenia i wygląd działają, a pozostałe operacje są odrzucane.
- Umieszczenie rangi nad Liderem nie daje jej uprawnień ani własności.
- Nieprawidłowe aktualizacje i kolejność nie modyfikują danych częściowo.
- Chroniona ranga Lidera, przekazanie jej wraz z własnością, brak możliwości
  przypisania jej przez zwykłe zarządzanie rangami.
- Migracja zapisów bez rang zachowuje członkostwo, logo i rejestr.
- Prywatne zgłoszenia są widoczne dla rekrutera i znikają po odebraniu INVITE.
- Wcześniejsze dane własnych rang i uprawnień są odczytywane.

Zachowano testy importu i detali 512×512, obu kodeków sieciowych, limitów,
członkostwa bez limitu, rejestru, paginacji, kolorów i układu.

## Klient graficzny

CRIME_UI_SMOKE_PASS: rzeczywisty renderer Minecrafta, 26 zrzutów 1920×1080,
GUI 2 i 4, Mesa llvmpipe. Formularz tworzenia ma tylko jedno pole (nazwę),
ustawienia logo nie mają pól ścieżki. Test sprawdza brak usuniętych napisów.
Przeciągnięcie pliku nadal otwiera kadrowanie i daje teksturę 512×512.

Sprawdzono widoczną zakładkę Rangi, formularz z nazwą i uprawnieniami,
przeciąganie wiersza, wyświetlanie rangi nad Liderem, blokadę edycji dla
zwykłego członka oraz ustawienia rekrutera z delegowanymi uprawnieniami.
Potwierdzenia operacji rang w tym teście pochodzą z danych przykładowych
przez rzeczywiste metody modelu; nie jest to połączenie z drugim graczem.
Testy poprzednich kategorii, kolorów, rejestru i logo także przeszły.

## Budowanie i ograniczenia

`./gradlew build` oraz `./gradlew -PuiSmoke runClient`.
Test kodeków nie jest testem sieci TCP ani gry dwóch klientów na serwerze.
Testy UI używają przykładowych danych. Fizyczne okno wyboru pliku w Windows
i przeciąganie z Eksploratora nie były automatyzowane. Oryginalnego
problematycznego pliku logo użytkownika nie otrzymano.

Źródła: PNG, JPEG, GIF, BMP, WebP, SVG, TIFF, ICO, ICNS, PSD, TGA, PCX i PNM.
Limit źródła rastrowego 64 MB, 100 000 px na bok i miliard pikseli. Podgląd
do 1024 px. SVG do 8 MB, bez odczytu zewnętrznych zasobów. HEIC/AVIF/PDF
oraz animowane logo pozostają nieobsługiwane. GIF używa pierwszej klatki.

Lokalny fallback java.home/bin/java w NeoForm Runtime pozostaje wyłącznie
narzędziem środowiska; nie trafia do JAR-a ani repozytorium. Niedostępność
usług kont Minecraft nie blokuje testu z danymi przykładowymi.
