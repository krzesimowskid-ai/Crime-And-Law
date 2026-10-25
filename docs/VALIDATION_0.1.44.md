# Weryfikacja 0.1.44 — ręczne przygotowanie doniczki

25 października 2026. Minecraft 1.21.1, NeoForge 21.1.248, Java 21.
Protokół **24**; jednakowy JAR na serwerze i u graczy.

## Zmiana

Nowa, ciemna plastikowa doniczka ma zwężane ośmiokątne wnętrze. Worki
mają własną przestrzenną bryłę, szwy, odchylany brzeg, kolory pojemności
i druk „AGRO / ZIEMIA”. To własne modele i materiały pikselowe. Referencje
stanowiły [oficjalny kadr Schedule I](https://www.scheduleonegame.com/presskit/schedule-one/images/26-plant.png.thumb.jpg)
i [ikona worka](https://www.schedule1wiki.org/icons/soil_icon.png);
grafiki, marki ani geometrii gry nie przenoszono do moda.

Lampa łączy się z ramą przez poprzeczkę, obejmy i dwa zachodzące na obudowę
uchwyty. Konewka w pierwszej osobie jest mniejsza i ma zmienione ustawienie.
Fiolet UV obejmuje modele stanowiska i dostępne górne powierzchnie otoczenia,
bez wymaganej paczki shaderów. Zasięg efektu i granice renderowania są
ograniczone, a geometria oświetlanych powierzchni buforowana przez 10 ticków.

Przygotowanie ma osobne etapy: przecięcie szwu, ręczne sypanie, otwarcie
woreczka, upuszczenie nasiona, zasypanie dołka i podlewanie. LPM chwyta,
mysz przesuwa, A/D oraz W/S przechylają. Worek i konewka pozostają
w miejscu po puszczeniu; nasiono podlega grawitacji. Wspólna geometria
klienta i serwera wyznacza otwór worka, wylewkę i tor strumienia.

## Testy jednostkowe

**209 testów w 36 zestawach**, bez błędów, niepowodzeń ani pominięć.
Dotychczasowe testy menu, finansów, mapy, upraw i zbioru pozostają w zestawie.
Nowe przypadki sprawdzają:

- Położenie rzeczywistego otworu po transformacji i przesunięcie wylewki.
- Brak strumienia przy puszczeniu lub niewłaściwym pochyleniu narzędzia.
- Trafienie strumienia w doniczkę na wszystkich poziomach napełnienia.
- Nietrafienie po odsunięciu narzędzia i zmianę toru przy przechyle w głąb.
- Mieszczenie powierzchni ziemi w zwężanym wnętrzu w 1001 krokach.
- Czas opadania nasiona i przecięcie toru z dołkiem.
- Odrzucenie NaN, nieskończoności, nadmiernego zasięgu i kątów.
- Zachowanie poprawnego kąta π po kodowaniu do float w pakiecie sieciowym.
- Odrzucenie narożników kwadratu poza ośmiokątną komorą.

Ten ostatni przypadek transportu kąta wykrył i zabezpiecza poprawkę błędu:
float π nieznacznie przekracza double π. Ścisła granica odrzucała całkowicie
odwrócony worek mimo poprawnego gestu.

## Dwa klienty TCP

Na dedykowanym serwerze dwa osobne klienty wykonały pełny przebieg przez
rzeczywiste okna, zdarzenia myszy i klawiatury oraz pakiety sieciowe:

1. Siedem zakupów u NPC; gotówka **2000 → 746**, koszt 1254. Ponowienie
   pakietu nie ponowiło zakupu. Bank nie był obciążany.
2. Ustawienie dwublokowej ramy, włożenie doniczki i UV, napełnienie konewki
   ze źródła **0 → 400**. Zrzut nocny potwierdza połączenia i fiolet UV.
3. Otwarcie Z i odmowa przejęcia tego samego stanowiska drugiemu graczowi.
   Po trzech sekundach poprawnie odzyskano symulowany brak potwierdzenia
   sklepu i przygotowania, bez ponowienia wydatku.
4. Przeciągnięcie ostrza po szwie, chwycenie worka LPM, przechylenie
   klawiszem D i sypanie. Puszczenie na 1,1 s zatrzymało postęp, a worek
   pozostał w miejscu. Dokończenie: pełna doniczka, duży worek **3 → 2**.
5. Otwarcie szwu woreczka nasion. Celowo nietrafione nasiono można ponownie
   podnieść bez kosztu. Prawidłowe upuszczenie uruchomiło grawitację,
   a przeciąganie ziemi dłonią zasypało dołek. Dopiero wtedy opakowanie
   zostało zużyte i pozostały **4 luźne nasiona**.
6. Chwycenie i przechylenie konewki klawiszem A. Woda wychodzi z poruszanej
   wylewki. Udane podlanie: **400 → 300**, doniczka **0 → 100**.
7. Zbiór ośmiu liści w kolejności **7, 2, 5, 0, 6, 1, 4, 3**. Dokładnie
   osiem nagród i osiem punktów zużycia nożyc. Powtórzenie nic nie dodało.
8. Po zbiorze roślina i ziemia zużyte, wyposażenie pozostało.

Czas wzrostu w przebiegu sieciowym przesunięto do końca wyłącznie
w scenariuszu testowym. Nie czekano 15 minut; pełne 18 000 kroków wzrostu
i ekspozycję sprawdzają istniejące testy jednostkowe.

## Próby niedozwolonych operacji

Na wątku serwera sprawdzono brak ukończenia przez samo odczekanie czasu,
pominięcie grawitacji i zasypywania, podmianę materiału, powtórzony FINISH,
wymianę całego bloku, ochronę miejsca, ścianę i odległy niezaładowany cel.
Kontrola zasięgu, tożsamości stanowiska, źródłowego ItemStack, świeżości
sterowania i rosnącego numeru próbki pozostaje po stronie serwera.

Zamknięty worek nie pozwolił sypać. Rozcięcie stosu dwóch małych worków
dało **jeden otwarty i jeden zamknięty**, bez utraty ziemi ani powielenia.
Duży worek na kolejnych doniczkach przeszedł **2 → 1 → zużyty**; ponowiony
pakiet nie pobrał dodatkowej porcji.

Sprawdzono też pełny ekwipunek przy rozpakowaniu i nagrodzie, zakup bez
gotówki mimo pieniędzy w banku, brak opłaty przy pełnym ekwipunku,
nieaktualną wycenę i oddalenie od NPC. Niszczenie dolnej/górnej części
ramy i eksplozja oddały wyposażenie raz; kreatywne niszczenie nie dało
drugiego zestawu. Dawna roślina nie oddała nasion ani darmowego plonu.

## Dysk i restart

Dwa kolejne pełne przebiegi na świeżych światach zakończyły się poprawnym
rzeczywistym restartem procesu. Zachowano migawki świata po zatrzymaniu,
przed ponownym uruchomieniem. Zgadzały się:

- Pierwszy stelaż z doniczką i UV, bez zużytej ziemi i rośliny.
- Zwykła roślina z zapisanym plonem 6, maską 47 i tym samym UUID.
- Gracz: 8 liści, 4 nasiona, 300 wody, 0 dużych worków, zużycie nożyc 8.
- Gotówka 746. Niezależny odczyt NBT potwierdził też jeden otwarty
  i jeden zamknięty mały worek po restarcie.

**Nierozstrzygnięta obserwacja:** jedna wcześniejsza próba restartu nie
odczytała zmian stanowisk w chunku, mimo przejścia kontroli przed zatrzymaniem.
Nie miała migawki sprzed restartu, więc nie ustalono, czy problem powstał
przy zapisie, czy ponownym odczycie. Dwa następne przebiegi z migawkami
przeszły bez zmiany produkcyjnego kodu zapisu. Nie uznajemy przyczyny tej
jednej próby za naprawioną ani wyjaśnioną. Launcher odrzuca teraz ponowne
użycie katalogu wyników, aby stare znaczniki PASS nie wpływały na test.

## Grafika i zakres

Obejrzano końcową galerię dziesięciu przedmiotów i zrzuty przebiegu TCP:
szew, unoszący się worek, rosnącą ziemię, opadające nasiono, zasypywanie,
strumień z konewki i nocne UV. Powierzchnia ziemi ma planarne współrzędne
tekstury, bez promienistej pajęczyny. Rant i grudki pozostają w doniczce.
Okna sprawdzono w **1920×1080 / GUI 2 i 4**, **855×481 / GUI 2** oraz
**640×480 / GUI 2**. Materiały mają filtr nearest; własne etykiety 32×48.

Kolor UV to lokalne oświetlenie modeli i poświata na widocznych górnych
powierzchniach kolizji. Nie jest globalnym silnikiem RGB dla wszystkich
ścian i bytów Minecrafta. Nie wykonano pomiaru FPS dużej farmy ani
awaryjnego odcięcia procesu między zapisami osobnych plików świata.
To kontrola zmienionego cyklu i istniejących testów, nie gwarancja braku
każdego błędu w całym modzie.

## Odtworzenie i archiwum

`./gradlew -PgardenSmoke compileJava prepareClientRun prepareServerRun`
kompiluje opcjonalne scenariusze. `tools/garden_smoke.py` uruchamia je
na Linuksie z Java 21 i Xvfb; narzędzia ustawia `CRIMEANDLAW_BUILD_TOOLS`.
Wybierz świeże `--output`, osobne od świata gry. `--visual` otwiera galerię.
Klasy scenariuszy są wyłączone z JAR wydania.

Dane wydania, suma SHA-256, rozmiar, porównanie klas i zasobów oraz wyniki
kontroli archiwum: [validation-0.1.44.json](validation-0.1.44.json).
