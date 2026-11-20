@echo off
chcp 65001 >nul
title Sincronizador Automatico — O Modpack Definitivo (GitHub + Prism Launcher)
cd /d "%~dp0"

echo ============================================================================
echo   SINCRONIZADOR DO MODPACK (VOCE + SEU PRIMO ^<--^> GITHUB ^<--^> PRISM)
echo ============================================================================
echo.

:: 1. Localiza Java 17 automaticamente (Prism Launcher ou JAVA_HOME do sistema)
if exist "%APPDATA%\PrismLauncher\java\java-runtime-gamma\bin\javac.exe" (
    set "JAVA_HOME=%APPDATA%\PrismLauncher\java\java-runtime-gamma"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

:: 2. Salva temporariamente alteracoes locais e baixa novidades do GitHub
echo [1/4] Baixando atualizacoes recentes do GitHub (origin/main)...
git pull --rebase --autostash origin main
if errorlevel 1 (
    echo [AVISO] Conflito ou falha no git pull. Verifique o status acima.
    pause
    exit /b 1
)

:: 3. Recompila o Core Mod (mythic_contracts) se houver Java 17 disponivel
echo.
echo [2/4] Compilando Core Mod (mythic_contracts-1.0.0.jar)...
call gradlew.bat build --no-daemon
if errorlevel 1 (
    echo [ERRO] Falha ao compilar o Core Mod Java.
    pause
    exit /b 1
)

:: 4. Atualiza index.toml, pack.toml, .mrpack e sincroniza com a instancia do Prism Launcher
echo.
echo [3/4] Atualizando Packwiz Index, .mrpack e instancia local do Prism Launcher...
python tools\update_pack_index.py

:: 5. Envia quaisquer novas alteracoes para o GitHub
echo.
echo [4/4] Verificando alteracoes locais para enviar ao GitHub...
git add .
git diff --cached --quiet
if errorlevel 1 (
    set /p COMMIT_MSG="Digite um resumo rapido da sua alteracao (ou Enter para mensagem automatica): "
    if "%COMMIT_MSG%"=="" set "COMMIT_MSG=chore(sync): atualizacao sincronizada do modpack e core mod"
    git commit -m "%COMMIT_MSG%"
    git push origin main
    echo.
    echo [SUCESSO] Suas alteracoes foram enviadas para https://github.com/Tayozinn/o-modpack-definitivo !
) else (
    echo [OK] Nenhuma alteracao local pendente. Seu modpack e Prism Launcher ja estao 100%% atualizados!
)

echo.
pause
