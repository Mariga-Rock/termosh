#!/usr/bin/env python3
"""
termosh-license — CLI для генерации лицензий Termosh.

Использование:
  python3 termosh-license.py keygen
      → приватный ключ ~/.termosh/owner.key
      → публичный ключ в base64 (вставить в OwnerPublicKey.kt приложения)

  python3 termosh-license.py request
      → показать пример строки запроса (пользователь копирует с телефона)

  python3 termosh-license.py issue --request '<json>' --name 'Иван' [--lifetime | --days 365]
      → выдать лицензионный код

  python3 termosh-license.py inspect '<code>'
      → расшифровать и показать содержимое кода

Зависимости: pip install cryptography
"""

import argparse
import base64
import json
import os
import secrets
import sys
import time
import uuid

try:
    from cryptography.hazmat.primitives.asymmetric.ed25519 import (
        Ed25519PrivateKey,
        Ed25519PublicKey,
    )
    from cryptography.hazmat.primitives import serialization
except ImportError:
    print("ERROR: pip install cryptography")
    sys.exit(1)


HOME_DIR = os.path.expanduser("~/.termosh")
PRIVKEY_PATH = os.path.join(HOME_DIR, "owner.key")


def b64u_encode(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def b64u_decode(s: str) -> bytes:
    pad = "=" * (-len(s) % 4)
    return base64.urlsafe_b64decode(s + pad)


def b64_encode(data: bytes) -> str:
    return base64.b64encode(data).decode("ascii")


def cmd_keygen(args):
    os.makedirs(HOME_DIR, exist_ok=True)
    if os.path.exists(PRIVKEY_PATH) and not args.force:
        print(f"Приватный ключ уже существует: {PRIVKEY_PATH}")
        print("Используй --force для перезаписи (старые лицензии перестанут работать).")
        return

    priv = Ed25519PrivateKey.generate()
    seed = priv.private_bytes(
        encoding=serialization.Encoding.Raw,
        format=serialization.PrivateFormat.Raw,
        encryption_algorithm=serialization.NoEncryption(),
    )
    with open(PRIVKEY_PATH, "wb") as f:
        f.write(seed)
    os.chmod(PRIVKEY_PATH, 0o600)

    pub = priv.public_key().public_bytes(
        encoding=serialization.Encoding.Raw,
        format=serialization.PublicFormat.Raw,
    )
    pub_b64 = b64_encode(pub)

    print("=== Приватный ключ ===")
    print(f"  {PRIVKEY_PATH}")
    print("  Беречь. Не показывать. Не коммитить в git.")
    print()
    print("=== Публичный ключ (вставить в приложение) ===")
    print(f"  app/.../core/licensing/OwnerPublicKey.kt:")
    print()
    print(f'    const val BASE64: String = "{pub_b64}"')
    print()


def cmd_request(args):
    example = {
        "uuid": "example-uuid-from-phone",
        "pubkey": "base64-ed25519-public-key-from-phone",
        "label": "Realme C67",
    }
    print("Пример строки запроса (пользователь копирует её с телефона):")
    print(json.dumps(example, ensure_ascii=False))


def cmd_issue(args):
    if not os.path.exists(PRIVKEY_PATH):
        print(f"ERROR: нет приватного ключа {PRIVKEY_PATH}. Сначала: keygen")
        sys.exit(1)

    seed = open(PRIVKEY_PATH, "rb").read()
    priv = Ed25519PrivateKey.from_private_bytes(seed)

    try:
        req = json.loads(args.request)
    except Exception as e:
        print(f"ERROR: не удалось разобрать --request как JSON: {e}")
        sys.exit(1)

    device_pub = req.get("pubkey") or ""
    if not device_pub:
        print("ERROR: в запросе нет 'pubkey'")
        sys.exit(1)

    now_ms = int(time.time() * 1000)
    expires_at = None
    if args.days:
        expires_at = now_ms + args.days * 86400 * 1000
    if args.lifetime:
        expires_at = None

    features = args.features.split(",") if args.features else ["all"]

    license = {
        "v": 1,
        "id": str(uuid.uuid4()),
        "name": args.name,
        "devicePublicKey": device_pub,
        "features": features,
        "issuedAt": now_ms,
        "expiresAt": expires_at,
        "nonce": secrets.token_urlsafe(12),
    }

    # Канонический JSON: точно в таком порядке, без пробелов
    canonical = json.dumps(license, ensure_ascii=False, separators=(",", ":"))

    signature = priv.sign(canonical.encode("utf-8"))

    code = b64u_encode(canonical.encode("utf-8")) + "." + b64u_encode(signature)

    print("=== Лицензионный код ===")
    print()
    print(code)
    print()
    print("=== Что внутри ===")
    print(json.dumps(license, ensure_ascii=False, indent=2))
    print()
    print("=== QR-код (сгенерируй отдельно) ===")
    print("  qrencode -o license.png '" + code + "'")


def cmd_inspect(args):
    code = args.code.strip()
    dot = code.find(".")
    if dot <= 0:
        print("ERROR: неверный формат")
        sys.exit(1)
    try:
        payload = b64u_decode(code[:dot]).decode("utf-8")
        lic = json.loads(payload)
        print(json.dumps(lic, ensure_ascii=False, indent=2))
    except Exception as e:
        print(f"ERROR: {e}")
        sys.exit(1)


def main():
    p = argparse.ArgumentParser(prog="termosh-license", description="Генератор лицензий Termosh")
    sub = p.add_subparsers(dest="cmd", required=True)

    p_keygen = sub.add_parser("keygen", help="Сгенерировать ключевую пару владельца")
    p_keygen.add_argument("--force", action="store_true", help="Перезаписать существующий ключ")
    p_keygen.set_defaults(func=cmd_keygen)

    p_req = sub.add_parser("request", help="Пример строки запроса с устройства")
    p_req.set_defaults(func=cmd_request)

    p_issue = sub.add_parser("issue", help="Выдать лицензию")
    p_issue.add_argument("--request", required=True, help="JSON-строка запроса с устройства")
    p_issue.add_argument("--name", required=True, help="Имя получателя")
    p_issue.add_argument("--days", type=int, default=None, help="Срок в днях (по умолчанию — бессрочно)")
    p_issue.add_argument("--lifetime", action="store_true", help="Бессрочная (по умолчанию)")
    p_issue.add_argument("--features", default="all", help="Список функций через запятую (по умолчанию: all)")
    p_issue.set_defaults(func=cmd_issue)

    p_insp = sub.add_parser("inspect", help="Показать содержимое лицензионного кода")
    p_insp.add_argument("code", help="Лицензионный код")
    p_insp.set_defaults(func=cmd_inspect)

    args = p.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
