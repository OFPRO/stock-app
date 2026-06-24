import csv
import io
import os
import uuid
import subprocess
import json
from datetime import datetime, timedelta, timezone

import jwt
from cryptography.hazmat.primitives import serialization, hashes
from cryptography.hazmat.primitives.asymmetric import rsa, padding
from cryptography.hazmat.backends import default_backend

_VIRTUAL_KEYWORDS = (
    'bluetooth', 'virtual', 'hyper-v', 'vmware', 'docker',
    'vpn', 'loopback', 'pseudo', 'tunnel', 'tap',
    'vmnet', 'virtualbox', 'hamachi', 'nordlynx',
)

_license_payload = None


def _normalize_mac(mac):
    mac = mac.strip().upper().replace('-', ':').replace('.', ':')
    parts = [p.zfill(2) for p in mac.split(':') if p]
    if len(parts) != 6:
        return None
    return ':'.join(parts)


def get_mac_address():
    if os.name == 'nt':
        try:
            result = subprocess.run(
                ['getmac', '/fo', 'csv', '/nh'],
                capture_output=True, text=True, timeout=5
            )
            if result.returncode == 0:
                reader = csv.reader(io.StringIO(result.stdout))
                for row in reader:
                    if len(row) >= 3:
                        conn_name = row[0].lower()
                        adapter = row[1].lower()
                        raw_mac = row[2].strip()
                        combined = f'{conn_name} {adapter}'
                        if any(kw in combined for kw in _VIRTUAL_KEYWORDS):
                            continue
                        if raw_mac in ('FF-FF-FF-FF-FF-FF', '00-00-00-00-00-00', '', 'N/A'):
                            continue
                        mac = _normalize_mac(raw_mac)
                        if mac:
                            return mac
        except (subprocess.TimeoutExpired, FileNotFoundError, csv.Error):
            pass

    raw = uuid.getnode()
    if raw is not None and (raw & 0x010000000000) == 0:
        mac = ':'.join(f'{(raw >> (5 - i) * 8) & 0xFF:02x}' for i in range(6)).upper()
        return mac

    return None


def generate_keypair(private_path='private.pem', public_path='public.pem'):
    key = rsa.generate_private_key(
        public_exponent=65537,
        key_size=2048,
        backend=default_backend()
    )
    private_pem = key.private_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PrivateFormat.PKCS8,
        encryption_algorithm=serialization.NoEncryption()
    )
    public_pem = key.public_key().public_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PublicFormat.SubjectPublicKeyInfo
    )
    with open(private_path, 'wb') as f:
        f.write(private_pem)
    with open(public_path, 'wb') as f:
        f.write(public_pem)
    return private_path, public_path


def sign_license(mac, client, days=365, private_key_path='private.pem'):
    with open(private_key_path, 'rb') as f:
        private_key = serialization.load_pem_private_key(
            f.read(), password=None, backend=default_backend()
        )
    now = datetime.now(timezone.utc)
    payload = {
        'mac': _normalize_mac(mac),
        'client': client,
        'iat': int(now.timestamp()),
        'exp': int((now + timedelta(days=days)).timestamp()),
    }
    token = jwt.encode(payload, private_key, algorithm='RS256')
    return token


def validate_license(token, public_key_path='public.pem'):
    if not os.path.exists(public_key_path):
        return None
    with open(public_key_path, 'rb') as f:
        public_key = serialization.load_pem_public_key(
            f.read(), backend=default_backend()
        )
    try:
        payload = jwt.decode(token, public_key, algorithms=['RS256'])
    except (jwt.ExpiredSignatureError, jwt.InvalidTokenError, ValueError):
        return None

    mac = get_mac_address()
    if not mac:
        return None
    token_mac = _normalize_mac(payload.get('mac', ''))
    if not token_mac or token_mac != _normalize_mac(mac):
        return None

    return payload


def save_license(token, path='.license'):
    data = {
        'token': token,
        'activated_at': datetime.now(timezone.utc).isoformat(),
    }
    with open(path, 'w') as f:
        json.dump(data, f)


def load_license(path='.license', public_key_path='public.pem'):
    global _license_payload

    if not os.path.exists(path):
        _license_payload = None
        return None

    try:
        with open(path) as f:
            data = json.load(f)
    except (json.JSONDecodeError, IOError):
        _license_payload = None
        return None

    token = data.get('token')
    if not token:
        _license_payload = None
        return None

    payload = validate_license(token, public_key_path)
    if not payload:
        _license_payload = None
        return None

    activated_str = data.get('activated_at')
    if activated_str:
        try:
            activated = datetime.fromisoformat(activated_str)
            if activated > datetime.now(timezone.utc):
                _license_payload = None
                return None
        except (ValueError, TypeError):
            pass

    _license_payload = payload
    return payload


def get_cached_payload():
    return _license_payload
