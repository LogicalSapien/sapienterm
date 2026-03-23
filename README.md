# SapienTerm

A modern Android SSH terminal app by [LogicalSapien](https://github.com/logicalsapien), built on ConnectBot's SSH engine.

## Features

- Full SSH terminal emulator with VT100/xterm support
- Material Design 3 UI with dark and light theme
- Card-based connection management with status indicators
- Quick Commands -- save, organize, and instantly send commands to the terminal
- Quick Commands toolbar above keyboard in the terminal session
- Credentials vault -- securely store SSH keys and passwords with Android Keystore encryption
- SSH key generation (RSA 2048/4096, Ed25519) and import from file
- Link credentials to connections for automatic authentication
- Export/Import data with optional encrypted backup (.sapienterm)
- Port forwarding (local, remote, dynamic/SOCKS5)
- Host key verification with fingerprint display
- Multiple simultaneous terminal sessions
- Keep-alive ping support
- All original ConnectBot features preserved

## Requirements

- Android 8.0+ (API 26)
- JDK 17 (for building from source)

## Building from Source

```sh
git clone <repo-url>
cd sapienterm
./gradlew assembleOssDebug
```

The APK will be at `app/build/outputs/apk/oss/debug/app-oss-debug.apk`.

## Install via USB

```sh
adb install app/build/outputs/apk/oss/debug/app-oss-debug.apk
```

## Tech Stack

Kotlin, Jetpack Compose, Material Design 3, Hilt, Room, sshlib, termlib

## Attribution

- Built on [ConnectBot](https://github.com/connectbot/connectbot) (Apache 2.0 License)
- SSH engine: [sshlib](https://github.com/connectbot/sshlib) by ConnectBot
- Terminal emulation: termlib by ConnectBot

## License

Apache 2.0
