@ECHO OFF
SETLOCAL

SET SERVER_DIR=C:\Users\acc4a\OneDrive\Desktop\tess
SET MODS_DIR=%SERVER_DIR%\mods
SET JAR_SRC=build\libs\peakskills-1.0.0.jar
SET JAR_DST=%MODS_DIR%\peakskills-1.0.0.jar

ECHO [Deploy] Stopping server if running...
FOR /F "tokens=1" %%P IN ('wmic process where "CommandLine like '%%tess%%server.jar%%'" get ProcessId 2^>nul ^| findstr /R "[0-9]"') DO (
    ECHO [Deploy] Killing java process %%P
    taskkill /PID %%P /F >nul 2>&1
)
TIMEOUT /T 2 /NOBREAK >nul

ECHO [Deploy] Waiting for jar to finish writing...
TIMEOUT /T 3 /NOBREAK >nul

ECHO [Deploy] Copying jar to mods folder...
COPY /Y "%JAR_SRC%" "%JAR_DST%" >nul
IF ERRORLEVEL 1 (
    ECHO [Deploy] ERROR: Failed to copy jar!
    EXIT /B 1
)

REM Verify the jar is a valid zip before starting the server
jar tf "%JAR_DST%" >nul 2>&1
IF ERRORLEVEL 1 (
    ECHO [Deploy] ERROR: Copied jar is corrupt!
    EXIT /B 1
)
ECHO [Deploy] Copied %JAR_SRC% -> %JAR_DST%

ECHO [Deploy] Starting server...
START "Minecraft Server" /D "%SERVER_DIR%" cmd /k "java -Xms1024M -Xmx2048M -jar server.jar --nogui"

ECHO [Deploy] Done!
ENDLOCAL
