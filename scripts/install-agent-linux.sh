#!/usr/bin/env sh
# Installs the agent as a systemd service. Run as root next to server-manager-agent.jar.
# Usage: sudo ./install-agent-linux.sh https://painel.exemplo.com TOKEN
set -eu
api_url=${1:?Informe a URL do painel.}
token=${2:?Informe o token do agente.}
[ "$(id -u)" -eq 0 ] || { echo 'Execute como root (sudo).' >&2; exit 1; }
[ ${#token} -ge 32 ] || { echo 'O token precisa ter no minimo 32 caracteres.' >&2; exit 1; }
src=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
[ -f "$src/server-manager-agent.jar" ] || { echo 'server-manager-agent.jar nao encontrado.' >&2; exit 1; }

install -d -m 755 /opt/server-manager-agent
install -m 644 "$src/server-manager-agent.jar" /opt/server-manager-agent/server-manager-agent.jar
id -u server-manager-agent >/dev/null 2>&1 || useradd --system --no-create-home --shell /usr/sbin/nologin server-manager-agent
umask 077
printf '{"server":"%s","token":"%s"}\n' "${api_url%/}" "$token" > /opt/server-manager-agent/config.json
chown server-manager-agent /opt/server-manager-agent/config.json

cat > /etc/systemd/system/server-manager-agent.service <<UNIT
[Unit]
Description=Server Manager Agent
After=network-online.target
Wants=network-online.target

[Service]
User=server-manager-agent
ExecStart=/usr/bin/java -jar /opt/server-manager-agent/server-manager-agent.jar /opt/server-manager-agent/config.json
Restart=always
RestartSec=10
NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
UNIT
systemctl daemon-reload
systemctl enable --now server-manager-agent
echo 'Agente instalado. Logs: journalctl -u server-manager-agent -f'
