# Walidacja 0.1.26 — 24 października 2026

Wynik: **PASS**. Minecraft 1.21.1, NeoForge 21.1.248, Java 21, protokół 16.

Zmiana dotyczy wejścia nowego rywala do walki o neutralny chunk. Wymaga
terenu w jednym z ośmiu sąsiednich chunków, także po skosie, oraz decyzji
rzeczywistego lidera gangu. Uprawnieni członkowie już zapisanej drużyny
zachowują możliwość dołączenia. Warunki są ponownie sprawdzane na serwerze
i przekazywane klientowi jako dostępność przycisku oraz powód odmowy.

## Testy

- `build`: sukces; 128 testów JUnit, zero niepowodzeń i błędów.
- Osiem nowych testów sprawdza każdy kierunek sąsiedztwa, brak terenu,
  teren zbyt daleki, nieuwzględnianie samego pola, obcego gangu i innego
  wymiaru, granice świata, decyzję lidera mimo uprawnień zwykłego członka,
  późniejsze dołączanie członków, obronę własnego pola i brak zmian
  w stanie po podglądzie dostępności lub odrzuconej próbie.
- Dedykowany serwer odrzucił dołączenie lidera bez sąsiedniego terenu,
  dopuścił go po dodaniu diagonalnego sąsiada i odrzucił uprawnionego
  członka próbującego zapisać gang bez lidera. Odrzucone próby nie zmieniły
  listy drużyn. Snapshot przekazał członkowi tę samą blokadę co akcja.
- Dwa rzeczywiste klienty połączyły się z odizolowanym serwerem TCP.
  Lider rywala posiadał chunk (1,1) i dołączył do neutralnego captu na
  (0,0), klikając mapę i potwierdzając nowy dialog. Capt przeszedł do PvP.
  Po śmierci rywala i zakończeniu walki właściciel zobaczył nową własność,
  odebrał dokładnie 11 diamentów z depozytu, a stan wydarzenia został
  wyczyszczony u obu klientów.
- Render klienta: Linux/Mesa/Xvfb, 1600×900. Testy uruchamiane przez
  `-PcaptureSmoke`, źródła w `src/captureTest/java`.

## Pakiet

- Plik: `crimeandlaw-0.1.26.jar`, 1916825 bajtów.
- SHA-256: `21abae51663624a4e407bff3e78a958962925fa11a46c6bbaf522fc70b419e71`.
- CRC JAR-a i 15 bibliotek poprawne.
- Metadane 0.1.26 i nowa polityka dołączenia obecne; klasy smoke wykluczone.

Zapis świata zachowuje dotychczasowy format. Pozostałe reguły czasu,
obecności, cooldownu, ochrony i obrony zajętego pola pozostają z poprzedniej
wersji; opis w `CAPTY.md`. Wymagana aktualizacja serwera i wszystkich klientów.
