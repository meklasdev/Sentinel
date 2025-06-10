# WiFiCraft Sentinel

WiFiCraft Sentinel to zaawansowany plugin bezpieczeństwa dla serwerów Minecraft, który oferuje kompleksową ochronę przed cheatami, podejrzanym zachowaniem i nieautoryzowanym dostępem.

## Funkcje

- Zaawansowany system anti-cheat (Grim + Vulan)
- Analiza zachowania graczy
- System śledzenia IP
- Analiza sprzętu
- Integracja z Discord
- Panel bezpieczeństwa
- System alertów
- Logowanie zdarzeń

## Wymagania

- Java 21+
- PaperMC 1.21+
- GrimAPI 2.0.0 (opcjonalne)
- Vulan (opcjonalne)

## Instalacja

1. Pobierz najnowszą wersję pluginu z sekcji "Releases"
2. Umieść plik .jar w folderze `plugins` na serwerze
3. Uruchom serwer (plugin stworzy plik konfiguracji)
4. Skonfiguruj plugin według potrzeb

## Konfiguracja

Plugin tworzy plik `config.yml` w folderze `plugins/WiFiCraftSentinel/`. Możesz skonfigurować:

- Systemy anti-cheat
- Poziomy ostrzeżeń
- Czas trwania naruszeń
- Lista podejrzanych krajów i stref czasowych
- Lista podejrzanych dostawców chmury
- Lista podejrzanych centrów danych
- Konfiguracja GUI
- Integracja z Discord

## Komendy

- `/violation <gracz>` - Sprawdza naruszenia anti-cheat
- `/ipanalyze <gracz>` - Analizuje adres IP gracza
- `/hardware <gracz>` - Analizuje sprzęt gracza
- `/cloud <gracz>` - Sprawdza dostawcę chmury
- `/sentinel <podkomenda>` - Komendy administracyjne

## Uprawnienia

- `sentinel.admin` - Pełne uprawnienia
- `sentinel.violation.check` - Sprawdzanie naruszeń
- `sentinel.ip.analyze` - Analiza IP
- `sentinel.hardware.check` - Analiza sprzętu
- `sentinel.cloud.check` - Sprawdzanie chmury

## Testowanie

Aby przetestować plugin lokalnie:

```bash
# Zbuduj plugin
./gradlew build

# Uruchom serwer testowy
./gradlew runServer
```

## Wspieranie

Jeśli potrzebujesz pomocy lub napotkałeś problem:

- Otwórz issue na GitHub
- Sprawdź dokumentację
- Skontaktuj się z autorem
