# Java Assist Remover

This module scans the `plugins` directory on startup and removes any JAR files that do not correspond to a loaded plugin. It helps protect the server from malicious or unauthorized components that may be manually uploaded.

**How it works**
1. Each JAR file is inspected for a `plugin.yml` entry.
2. The plugin name from the descriptor is matched against the currently loaded plugins.
3. If a JAR does not match any loaded plugin, it is considered unauthorized and is deleted.

No additional configuration is required; the scanner runs automatically when Sentinel is enabled.
