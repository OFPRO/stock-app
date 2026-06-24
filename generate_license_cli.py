#!/usr/bin/env python3
import argparse
import sys
from license_manager import get_mac_address, sign_license, generate_keypair


def main():
    parser = argparse.ArgumentParser(description='Générer une licence JWT pour StockPro')
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--mac', type=str, help='Adresse MAC manuelle (XX:XX:XX:XX:XX:XX)')
    group.add_argument('--detect', action='store_true', help='Détecter la MAC de cette machine')
    parser.add_argument('--days', type=int, default=365, help='Durée de validité en jours')
    parser.add_argument('--client', type=str, default='Default', help='Nom du client')
    parser.add_argument('--key', type=str, default='private.pem', help='Chemin vers la clé privée')
    parser.add_argument('--generate-keys', action='store_true', help='Générer la paire de clés RSA')
    args = parser.parse_args()

    if args.generate_keys:
        priv, pub = generate_keypair()
        print(f'Clé privée : {priv}')
        print(f'Clé publique: {pub}')
        print('Gardez private.pem en sécurité. public.pem sera dans le .exe.')
        return

    if args.detect:
        mac = get_mac_address()
        if not mac:
            print('ERREUR: Impossible de détecter l\'adresse MAC', file=sys.stderr)
            sys.exit(1)
        print(f'MAC détectée : {mac}')
    else:
        mac = args.mac

    token = sign_license(mac, args.client, args.days, args.key)
    print()
    print('=== TOKEN DE LICENCE ===')
    print(token)
    print('=======================')
    print()
    print(f'Client : {args.client}')
    print(f'MAC     : {mac}')
    print(f'Durée   : {args.days} jours')
    print()
    print('Envoyez le token au client.')


if __name__ == '__main__':
    main()
