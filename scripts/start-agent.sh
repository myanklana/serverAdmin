#!/usr/bin/env sh
# Uso: ./start-agent.sh https://api.exemplo.com TOKEN_COM_32_OU_MAIS_CARACTERES
set -eu

api_url=${1:?Informe a URL da API.}
token=${2:?Informe o token do agente.}
script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
jar_path="$script_dir/server-manager-agent.jar"
config_path="$script_dir/config.json"

case "$api_url" in http://*|https://*) ;; *) echo 'A URL da API deve iniciar com http:// ou https://.' >&2; exit 1;; esac
[ ${#token} -ge 32 ] || { echo 'O token precisa ter no minimo 32 caracteres.' >&2; exit 1; }
[ -f "$jar_path" ] || { echo "JAR nao encontrado: $jar_path" >&2; exit 1; }

umask 077
printf '{"server":"%s","token":"%s"}\n' "${api_url%/}" "$token" > "$config_path"
echo "Enviando metricas para ${api_url%/} a cada 5 segundos. Use Ctrl+C para parar."
exec java -jar "$jar_path" "$config_path"
