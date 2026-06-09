# Privacy Policy — SapienTerm

**Last updated: 2026-06-09**

SapienTerm ("the app") is an open-source Android SSH terminal client developed by LogicalSapien.

## Data collection

The app does **not** collect, store remotely, transmit, or share any personal data. There is no analytics SDK, no crash-reporting service, and no network communication initiated by the app itself other than the SSH/Telnet connections you explicitly create.

## Data stored on your device

The following data is stored locally on your device only:

- **SSH host configurations** — hostnames, ports, usernames, and connection preferences, stored in an encrypted Room database.
- **SSH credentials** — private keys and passwords, protected by Android Keystore encryption. Keys never leave the device in plaintext.
- **App preferences** — theme, font size, and UI settings, stored in Android DataStore.

This data is not backed up to Google's cloud backup service (`allowBackup` is disabled). You may export an encrypted backup manually via the app's export feature.

## Permissions

| Permission | Reason |
|---|---|
| `INTERNET` | Required to open SSH/Telnet connections |
| `ACCESS_NETWORK_STATE` | Detect network changes to reconnect sessions |
| `FOREGROUND_SERVICE` | Keep terminal sessions alive when the app is in the background |
| `FOREGROUND_SERVICE_REMOTE_MESSAGING` | Required by Android 14+ for the foreground service type |
| `WAKE_LOCK` | Keep the CPU awake during active SSH sessions |
| `VIBRATE` | Optional haptic feedback for terminal bell events |
| `POST_NOTIFICATIONS` | Show a persistent notification for active terminal sessions |

## Contact

If you have questions about this policy, open an issue at https://github.com/LogicalSapien/sapienterm/issues.
