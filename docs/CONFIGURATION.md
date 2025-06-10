# WiFiCraft Sentinel Configuration Guide

## Konfiguracja Modułu Discord

Moduł Discord jest odpowiedzialny za wysyłanie powiadomień i zarządzanie slash command'ami na Discordzie.

### Konfiguracja Webhook'ów
```yaml
discord:
  enabled: true # Włączenie/wyłączenie modułu Discord
  bot-token: ""  # Token bota Discord
  webhook-config:
    enabled: true # Włączenie/wyłączenie webhook'ów
    default-webhook: "" # Domyślny webhook Discord dla wszystkich alertów
    webhooks:
      # Możliwość zdefiniowania wielu webhook'ów dla różnych typów alertów
      security:
        webhook-url: "" # Webhook dla alertów bezpieczeństwa
        severity-level: 2 # Minimalny poziom ważności dla tego webhook'a (1-4)
        enabled: true # Włączenie/wyłączenie tego webhook'a
        color: "RED" # Kolor wiadomości (RED, ORANGE, YELLOW, GREEN)
      performance:
        webhook-url: "" # Webhook dla alertów wydajności
        severity-level: 1 # Minimalny poziom ważności dla tego webhook'a
        enabled: true
        color: "YELLOW"
      moderation:
        webhook-url: "" # Webhook dla akcji moderacyjnych
        severity-level: 2
        enabled: true
        color: "BLUE"
      suspicious:
        webhook-url: "" # Webhook dla podejrzanych działań
        severity-level: 3
        enabled: true
        color: "ORANGE"
  help-channel-id: "" # ID kanału pomocy
  guild-id: "" # ID serwera Discord
  slash-commands-enabled: true # Włączenie slash command'ów
  prefix: "!" # Prefix dla slash command'ów
  cooldown: 5 # Cooldown w sekundach
  min-permission: "operator" # Minimalne uprawnienia do komend
  role-sync:
    enabled: true # Włączenie synchronizacji ról
    player-mappings:
      # Przykład:
      # "player1": "123456789012345678"
    role-mappings:
      # Przykład:
      # "Staff": "staff"
      # "VIP": "vip"
  debug: false
  slash-commands:
    enabled: true # Włączenie slash command'ów
    prefix: "/" # Prefix dla slash command'ów
    cooldown: 5 # Cooldown w sekundach
    permissions:
      player: "moderator" # Uprawnienia dla komendy player
      alert: "admin" # Uprawnienia dla komendy alert
      report: "moderator" # Uprawnienia dla komendy report
```

Moduł Discord jest odpowiedzialny za wysyłanie powiadomień i zarządzanie slash command'ami na Discordzie.

### Konfiguracja Webhook'ów

```yaml
discord:
  enabled: true # Włączenie/wyłączenie modułu Discord
  bot-token: ""  # Token bota Discord
  webhook-config:
    enabled: true # Włączenie/wyłączenie webhook'ów
    default-webhook: "" # Domyślny webhook Discord dla wszystkich alertów
    webhooks:
      # Możliwość zdefiniowania wielu webhook'ów dla różnych typów alertów
      security:
        webhook-url: "" # Webhook dla alertów bezpieczeństwa
        severity-level: 2 # Minimalny poziom ważności dla tego webhook'a (1-4)
        enabled: true # Włączenie/wyłączenie tego webhook'a
        color: "RED" # Kolor wiadomości (RED, ORANGE, YELLOW, GREEN)
      performance:
        webhook-url: "" # Webhook dla alertów wydajności
        severity-level: 1 # Minimalny poziom ważności dla tego webhook'a
        enabled: true
        color: "YELLOW"
      moderation:
        webhook-url: "" # Webhook dla akcji moderacyjnych
        severity-level: 2
        enabled: true
        color: "BLUE"
      suspicious:
        webhook-url: "" # Webhook dla podejrzanych działań
        severity-level: 3
        enabled: true
        color: "ORANGE"
  help-channel-id: "" # ID kanału pomocy
  guild-id: "" # ID serwera Discord
  slash-commands-enabled: true # Włączenie slash command'ów
  prefix: "!" # Prefix dla slash command'ów
  cooldown: 5 # Cooldown w sekundach
  min-permission: "operator" # Minimalne uprawnienia do komend
  role-sync:
    enabled: true # Włączenie synchronizacji ról
    player-mappings:
      # Przykład:
      # "player1": "123456789012345678"
    role-mappings:
      # Przykład:
      # "Staff": "staff"
      # "VIP": "vip"
  debug: false
  slash-commands:
    enabled: true # Włączenie slash command'ów
    prefix: "/" # Prefix dla slash command'ów
    cooldown: 5 # Cooldown w sekundach
    permissions:
      player: "moderator" # Uprawnienia dla komendy player
      alert: "admin" # Uprawnienia dla komendy alert
      report: "moderator" # Uprawnienia dla komendy report
```

### Śledzenie Zachowania
```yaml
security:
  behavior-tracking:
    enabled: true # Włączenie/wyłączenie śledzenia
    history-duration: "1h" # Czas przechowywania historii (np., "1h", "24h", "7d")
    alert-threshold: 3 # Liczba podejrzanych zachowań przed wyzwoleniem alertu
    pattern-matching: true # Włączenie dopasowywania wzorców
    scoring-enabled: true # Włączenie systemu punktacji
```

### System Alertów
```yaml
security:
  alerts:
    enabled: true # Włączenie/wyłączenie alertów
    severity-threshold: 2 # Minimalny poziom ważności dla alertów (1-4)
    notification-delay: "10s" # Opóźnienie przed wysłaniem powiadomienia
    retry-attempts: 3 # Liczba prób wysłania nieudanego alertu
```

### IP Analysis
```yaml
security:
  ip-analysis:
    enabled: true # Enable/disable IP analysis
    vpn-check: true # Enable VPN detection
    proxy-check: true # Enable proxy detection
    geolocation-enabled: true # Enable geolocation analysis
    suspicious-locations: ["RU", "CN", "IR"] # List of suspicious country codes
```

### Hardware Fingerprinting
```yaml
security:
  hardware-fingerprinting:
    enabled: true # Enable/disable hardware fingerprinting
    cache-duration: "24h" # Duration to cache fingerprint data
    update-interval: "1h" # Interval between fingerprint updates
    cache-size: 1000 # Maximum number of cached fingerprints
```

### Performance Settings
```yaml
security:
  performance:
    max-threads: 4 # Maximum number of processing threads
    queue-size: 1000 # Maximum queue size for processing
    batch-size: 100 # Number of events processed in batch
```

## Logging Configuration

The logging system allows you to configure how security events are logged.

```yaml
logging:
  level: "INFO" # Log level (INFO, WARNING, SEVERE, FINE)
  log-file: "sentinel.log" # Path to log file
  log-rotation: true # Enable log rotation
  max-log-size: "10MB" # Maximum size of log file before rotation
  rotation-count: 5 # Number of rotated logs to keep
```

## Logging Configuration

The logging system allows you to configure how security events are logged.

```yaml
logging:
  level: "INFO" # Log level (INFO, WARNING, SEVERE, FINE)
  log-file: "sentinel.log" # Path to log file
```

## Severity Levels

The system uses a severity level system for alerts:

- Level 1 (Green): Low severity
- Level 2 (Yellow): Medium severity
- Level 3 (Orange): High severity
- Level 4 (Red): Critical severity

These levels are used for both logging and Discord alerts, with corresponding colors in Discord messages.

## Best Practices

1. Regularly review logs for suspicious activity
   - Use log rotation to prevent disk space issues
   - Set up alert thresholds based on server size
   - Monitor performance metrics

2. Keep Discord webhook URL secure
   - Never share the webhook URL
   - Use environment variables for configuration
   - Regularly rotate webhook URLs

3. Adjust thresholds based on server needs
   - Start with default values
   - Monitor false positives/negatives
   - Adjust thresholds gradually

4. Regularly update configuration
   - Check for new suspicious locations
   - Update behavior patterns
   - Review performance metrics

5. Monitor performance impact
   - Use performance settings
   - Monitor thread usage
   - Adjust batch sizes as needed

6. Security Recommendations
   - Enable all security features initially
   - Review alerts daily
   - Keep software updated
   - Regular backups

## Troubleshooting

If you encounter issues with alerts or logging:

1. Check if the Discord webhook URL is correct
2. Verify that the log file has proper permissions
3. Ensure that the severity threshold is properly configured
4. Check the server logs for errors
5. Monitor performance metrics
6. Review configuration settings
7. Check for updates to the plugin
8. Contact support if needed

## Performance Monitoring

To monitor Sentinel's performance:

1. Use performance settings in configuration
2. Monitor thread usage
3. Check queue sizes
4. Review batch processing times
5. Adjust settings based on server load

## Advanced Configuration

For advanced users:

1. Customize behavior patterns
2. Adjust scoring system
3. Modify alert templates
4. Configure custom thresholds
5. Implement custom logging

## Security Recommendations

1. Regular updates
2. Secure configuration
3. Monitor performance
4. Regular backups
5. Keep logs secure
6. Review alerts daily

## Troubleshooting Guide

### Common Issues

1. **Alerts Not Working**
   - Verify Discord webhook URL
   - Check severity threshold
   - Review log files
   - Monitor plugin permissions

2. **Performance Issues**
   - Check thread usage
   - Monitor queue sizes
   - Review batch processing
   - Adjust performance settings

3. **Configuration Errors**
   - Validate YAML syntax
   - Check for missing values
   - Review log messages
   - Use default configuration

### Troubleshooting Steps

1. **Basic Checks**
   - Verify plugin is enabled
   - Check permissions
   - Review logs
   - Test configuration

2. **Advanced Troubleshooting**
   - Enable debug logging
   - Review thread dumps
   - Check memory usage
   - Monitor CPU load

3. **Performance Optimization**
   - Adjust thread count
   - Modify batch sizes
   - Review queue settings
   - Monitor memory usage

### Error Codes

| Error Code | Description | Solution |
|------------|-------------|----------|
| 400 | Invalid Configuration | Check YAML syntax |
| 401 | Permission Denied | Verify permissions |
| 404 | Webhook Not Found | Check Discord URL |
| 500 | Internal Error | Review logs |
| 503 | Service Unavailable | Check server status |
