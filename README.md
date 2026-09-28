# Termosh

Android-клиент для **SSH** и **mosh**. Всё работает локально: без серверов,
без аккаунтов, без телеметрии. Пароли и ключи хранятся в Android Keystore и
SQLCipher.

> **Статус:** alpha. SSH и mosh работают. Часть UI-функций не реализована.
> См. [Известные ограничения](docs/LIMITATIONS.md).

## Возможности

- **SSH** — пароль, публичный ключ (Ed25519 / RSA / ECDSA P-256), ProxyJump.
- **mosh** — нативный клиент, переживает смену сети и долгие паузы.
- **Вкладки** — несколько серверов одновременно, split-view.
- **Импорт**: `~/.ssh/config` (с `Include`), `known_hosts` (включая
  хешированные), ConnectBot XML, `.termosh` / `.termoshvault`.
- **Экспорт**: `.termosh` (без секретов / с секретами), OpenSSH config.
- **Port forwarding**, сниппеты, TOTP-хранилище.
- **Три темы**: Tokyo Night, Catppuccin Frappe, GitHub Light.
- **Double-tap** в терминале — вставка из буфера.

## Установка

1. Открой [последний релиз](https://github.com/Mariga-Rock/termosh/releases).
2. Скачай `app-release.apk` на телефон.
3. Android скажет «Установка заблокирована» → **Настройки** → разреши
   установку из этого источника.
4. Вернись, установи APK.
5. Открой Termosh, добавь сервер, подключись.

## Требования

- Android 14 или новее (`minSdk 34`).
- Процессор `arm64-v8a` (все современные телефоны).

## Что работает / что не работает

См. [docs/LIMITATIONS.md](docs/LIMITATIONS.md). Кратко:

**Работает:** SSH, mosh, вкладки, импорт/экспорт, port forwarding,
сниппеты, темы, лицензирование.

**Не работает:** TOTP-интерактив (секреты хранятся, но не подставляются при
входе), tmux-панель, rename/drag-and-drop вкладок, FIDO2, пинч-зум.

**Не проверено:** port-forwarding на живом сервере, ProxyJump,
внешняя клавиатура.

## Обратная связь

- **Баг-репорт:** [открой issue](https://github.com/Mariga-Rock/termosh/issues/new).
  Опиши шаги воспроизведения, версию Android, модель телефона.
- **Логи падения:** приложи файл
  `/sdcard/Android/data/app.termosh/files/crash.log` (если есть).
  Или сними через `adb logcat -d | grep -iE 'Termosh|FATAL'`.

## Благодарности

- [sshj](https://github.com/hierynomus/sshj)
- [mosh](https://github.com/mobile-shell/mosh)
- [BouncyCastle](https://www.bouncycastle.org/)
- [SQLCipher](https://www.zetetic.net/sqlcipher/)
- [Catppuccin](https://github.com/catppuccin/catppuccin)

## Лицензия

MIT — см. [LICENSE](LICENSE).
