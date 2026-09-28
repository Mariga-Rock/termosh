# Termosh

Нативный Android-клиент для **SSH** и **mosh**, написанный на Kotlin.

Всё работает локально — никаких серверных компонентов, никакой телеметрии.
Секреты хранятся в Android Keystore + SQLCipher. Лицензирование — Ed25519,
офлайн.

## Возможности

- **SSH** через [sshj](https://github.com/hierynomus/sshj) + BouncyCastle.
  Пароль, публичный ключ (Ed25519 / RSA / ECDSA P-256), ProxyJump.
- **mosh** — нативный `mosh-client` (`arm64-v8a`), запуск через JNI PTY.
  Переживает смену сети и долгие паузы.
- **TOFU** для host keys, включая хешированные записи `known_hosts` (HMAC-SHA1).
- **Импорт/экспорт**: `~/.ssh/config` (с `Include`), `known_hosts`,
  ConnectBot XML, собственный формат `.termosh` / `.termoshvault` (AES-GCM +
  PBKDF2), экспорт в OpenSSH config.
- **Терминал** — собственный эмулятор с ANSI, 256 цветами, truecolor,
  альтернативным экраном, scroll region.
- **Вкладки** с per-tab индикатором состояния, split-view.
- **Double-tap** для вставки из буфера.
- **Port forwarding**, сниппеты, TOTP-хранилище, логи, три темы.

## Требования

- Android 14+ (`minSdk = 34`, `targetSdk = 34`).
- `arm64-v8a` (единственная поддерживаемая ABI).

## Установка

Скачайте APK из [releases](https://github.com/Mariga-Rock/termosh/releases)
и установите на устройство. Разрешите установку из неизвестных источников,
если Android попросит.

Для разработки:

```bash
git clone https://github.com/Mariga-Rock/termosh.git
cd termosh
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
