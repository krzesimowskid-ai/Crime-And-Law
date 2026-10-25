# Weryfikacja 0.1.42 — pikselowe modele i swobodny zbiór

25 października 2026. Minecraft 1.21.1, NeoForge 21.1.248, Java 21.
Protokół 22; wymagany JAR 0.1.42 na serwerze i u graczy.

## Wynik

Końcowe `build`: **BUILD SUCCESSFUL**. Wszystkie **181 testów w 33 zestawach**
przeszły, bez błędów, niepowodzeń ani pominięć. Usunięto losowy wybór liścia;
każde cięcie wiąże serwerowy czas z liściem wybranym przez gracza.

## Grafika w prawdziwym kliencie

- Cztery przypadki: 1920×1080 / GUI 2, 1920×1080 / GUI 4,
  855×481 / GUI 2 i 640×480 / GUI 2; obrót i zoom kamery.
- Sześć różnych znaczników; każdy można wybrać niezależnie od pozostałych.
- Liść, nasiona i nożyce mają dokładnie 32×32 piksele po załadowaniu,
  własne palety do 16 kolorów i filtry min/mag **GL_NEAREST**.
  Filtry pozostają ostre mimo prób zmiany przez warstwy renderera.
- Modele mają przednie, tylne i schodkowe boczne powierzchnie.
  W galerii sprawdzono widok przedni, ukośny oraz rozmiar ekwipunku.
- Atlas bloków pozostał 1024×512 z czterema poziomami mipmap.
- Obejrzano model w świecie, ekran zbioru, przytrzymanie i zebrane liście;
  zapisano 9 zrzutów wizualnych i 16 z rozgrywki sieciowej.

## Serwer, dwa klienty i restart

Prawdziwy skrót Z otworzył minigrę po wczytaniu świata. Gracz zbierał liście
w wybranej kolejności **5, 1, 3, 0, 4, 2**. Każde przytrzymanie/puszczenie
przeszło przez TCP: jeden liść i jeden punkt zużycia. Roślina odrosła od
etapu 1. Drugi klient nie mógł przejąć trwającej rezerwacji.

Sprawdzono brak nagród przy obcym/starym tokenie, nieprawidłowym ID liścia,
puszczeniu bez rozpoczęcia, za szybkim i za późnym cięciu. Podczas poprawnie
odmierzonego przytrzymania wysłano rozpoczęcie i zakończenie dla innego
liścia: serwer zachował pierwotny cel i czas, bez dodatkowej nagrody.
Powtórzone zakończenie i pakiet zamkniętej sesji również niczego nie dodały.

Sprawdzono zwykłe nożyce, zasięg, ścianę i veto zdarzenia ochrony miejsca.
Pełny ekwipunek kreatywny spowodował dokładnie jeden przedmiot na ziemi,
zamiast utraty lub powielenia nagrody. Po trzech sekundach bez potwierdzenia
klient odzyskał stan przez anulowanie, bez ponawiania nagrody.

Rzeczywisty restart dyskowy zachował częściowy zbiór drugiej rośliny
(pięć pozostałych liści), odrost pierwszej, dokładnie siedem nagród
oraz siedem punktów zużycia narzędzia w danych gracza.

Test jednostkowy sprawdza wszystkie **720 kolejności** sześciu liści.
Pozostałe istniejące testy upraw, finansów, uprawnień i interfejsu przeszły.

## Archiwum wydania

- `crimeandlaw-0.1.42.jar`: **3 817 162 bajty**.
- SHA-256: `e9e5d185ada814027db1d6f2e00bc36b1c649dc921fa980bc5b62726797d71d4`.
- Poprawne CRC, brak powtórzonych wpisów i klas scenariuszy testowych.
- 200 klas produkcyjnych identycznych bajtowo z końcową kompilacją.
- Trzy oryginalne PNG identyczne z zasobami źródłowymi.
- Usunięto stary realistyczny PNG i nieużywane renderery liścia.
- Szczegółowe wyniki: [validation-0.1.42.json](validation-0.1.42.json).

## Zakres potwierdzenia

To kontrola zmian grafiki i swobodnego wyboru liścia wraz z istniejącym
zestawem testów. Nie jest ponownym audytem wszystkich źródeł moda ani
pomiarem dużej farmy na każdym sprzęcie. Pełny przegląd wcześniejszego
menu i finansów opisuje [raport 0.1.40](VALIDATION_0.1.40.md).
