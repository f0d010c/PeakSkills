@ECHO OFF
SETLOCAL

SET PROJECT_DIR=%~dp0
SET SERVER_DIR=C:\Users\acc4a\OneDrive\Desktop\tess
SET MODS_DIR=%SERVER_DIR%\mods
SET JAR_SRC=%PROJECT_DIR%build\libs\peakskills-1.0.0.jar
SET JAR_DST=%MODS_DIR%\peakskills-1.0.0.jar

ECHO [Build] Building mod...
CALL "%PROJECT_DIR%gradlew.bat" build
IF ERRORLEVEL 1 (
    ECHO [Build] BUILD FAILED - skipping deploy.
    PAUSE
    EXIT /B 1
)

REM Gradle has fully exited here - file handles are released
ECHO [Deploy] Stopping server if running...
FOR /F "tokens=1" %%P IN ('wmic process where "CommandLine like '%%tess%%server.jar%%'" get ProcessId 2^>nul ^| findstr /R "[0-9]"') DO (
    ECHO [Deploy] Killing java process %%P
    taskkill /PID %%P /F >nul 2>&1
)
TIMEOUT /T 3 /NOBREAK >nul

ECHO [Deploy] Copying jar...
COPY /Y "%JAR_SRC%" "%JAR_DST%" >nul
IF ERRORLEVEL 1 (
    ECHO [Deploy] ERROR: Copy failed!
    PAUSE
    EXIT /B 1
)
ECHO [Deploy] Copied successfully.

ECHO [Deploy] Starting server...
START "Minecraft Server" /D "%SERVER_DIR%" cmd /k "java -Xms1024M -Xmx2048M -jar server.jar --nogui"

ECHO [Deploy] Done!
ENDLOCAL
