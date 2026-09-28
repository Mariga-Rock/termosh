# Termosh 0.1.0 — beta 1

Первая beta-версия. Приложение можно ставить на реальное устройство и
использовать для повседневной работы с SSH и mosh.

## Что работает

- Подключение по SSH: пароль, публичный ключ (Ed25519 / RSA / ECDSA P-256).
- Проксирование через bastion (ProxyJump).
- mosh через нативный клиент.
- Вкладки с per-tab статусом, split-view.
- Double-tap для вставки из системного буфера.
- Импорт/экспорт: OpenSSH config, known_hosts (в т.ч. хешированные),
  ConnectBot XML, .termosh / .termoshvault, экспорт в OpenSSH config.
- Port forwarding.
- Сниппеты, TOTP-хранилище, три темы.
- Foreground-уведомление активной сессии.
- Лицензирование Free/Pro (офлайн, Ed25519).

## Что не работает или не проверено

См. https://github.com/Mariga-Rock/termosh/blob/main/docs/LIMITATIONS.md

Кратко:

- TOTP не подставляется при входе.
- Tmux-панель отсутствует.
- Rename/drag-and-drop вкладок нет.
- Внешняя клавиатура не тестировалась.
- Port forwarding и ProxyJump не проверены на живом сервере.

## Требования

- Android 14+ (minSdk 34).
- arm64-v8a (все современные телефоны).

## Установка

1. Скачайте `app-release.apk` из этого релиза.
2. Разрешите установку из неизвестных источников.
3. Установите APK.

## Как сообщить о баге

Откройте issue: https://github.com/Mariga-Rock/termosh/issues/new

Приложите:
- Версия Android и модель устройства.
- Шаги воспроизведения.
- Что ожидалось / что произошло.
- Лог: /sdcard/Android/data/app.termosh/files/crash.log

## Благодарности

- sshj, mosh, BouncyCastle, SQLCipher, Catppuccin.
