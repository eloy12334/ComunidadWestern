$ErrorActionPreference = "Stop"
Set-Location -LiteralPath $PSScriptRoot

Write-Host "===============================================" -ForegroundColor Cyan
Write-Host " COMUNIDAD WESTERN - RELAY FCM LOCAL" -ForegroundColor Cyan
Write-Host "===============================================" -ForegroundColor Cyan

$credentialPath = Read-Host "Pega la ruta completa del JSON de cuenta de servicio (guardado FUERA del proyecto)"
if ([string]::IsNullOrWhiteSpace($credentialPath)) {
    Write-Host "No se indicó el archivo de credenciales." -ForegroundColor Red
    exit 1
}

$resolvedCredential = Resolve-Path -LiteralPath $credentialPath -ErrorAction SilentlyContinue
if (-not $resolvedCredential) {
    Write-Host "No se encontró el archivo indicado." -ForegroundColor Red
    exit 1
}

$env:GOOGLE_APPLICATION_CREDENTIALS = $resolvedCredential.Path
$env:FIREBASE_PROJECT_ID = "comunidad-western"
$env:FIREBASE_DATABASE_URL = "https://comunidad-western-default-rtdb.firebaseio.com"
$env:FIREBASE_COMMUNITY_ID = "comunidad01"

if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    Write-Host "Node.js no está instalado o no está disponible en PATH. Instala Node.js 22 o superior." -ForegroundColor Red
    exit 1
}

$nodeMajor = [int]((node -p "process.versions.node.split('.')[0]").Trim())
if ($nodeMajor -lt 22) {
    Write-Host "Se requiere Node.js 22 o superior. Versión detectada: $(node --version)" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path -LiteralPath (Join-Path $PSScriptRoot "node_modules"))) {
    Write-Host "Instalando dependencias de Node.js..." -ForegroundColor Yellow
    npm install
    if ($LASTEXITCODE -ne 0) {
        Write-Host "No se pudieron instalar las dependencias." -ForegroundColor Red
        exit $LASTEXITCODE
    }
}

Write-Host "Iniciando relay. Mantén esta ventana abierta." -ForegroundColor Green
npm start
exit $LASTEXITCODE
