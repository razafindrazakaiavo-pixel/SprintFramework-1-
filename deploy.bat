@echo off
setlocal EnableDelayedExpansion

echo ==================================================
echo  Deploy Framework - JDK 17+ requis, sortie Java 17
echo ==================================================

rem --- 1. Chercher un javac compatible ---
set "JAVAC="
set "JARTOOL="

rem Priorite 1 : JAVA_HOME
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" (
        set "JAVAC=%JAVA_HOME%\bin\javac.exe"
        set "JARTOOL=%JAVA_HOME%\bin\jar.exe"
    )
)

rem Priorite 2 : chemins connus sous Windows
if not defined JAVAC (
    for %%J in (
        "C:\Program Files\Java\jdk-24\bin\javac.exe"
        "C:\Program Files\Java\jdk-21\bin\javac.exe"
        "C:\Program Files\Java\jdk-17\bin\javac.exe"
        "C:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot\bin\javac.exe"
        "C:\Program Files\Eclipse Adoptium\jdk-17.0.14.7-hotspot\bin\javac.exe"
    ) do (
        if not defined JAVAC (
            if exist %%J (
                set "JAVAC=%%~J"
                set "JARTOOL=%%~dpJjar.exe"
            )
        )
    )
)

rem Priorite 3 : javac du PATH
if not defined JAVAC (
    set "JAVAC=javac"
    set "JARTOOL=jar"
)

echo JDK utilise : "!JAVAC!"
"!JAVAC!" -version
if errorlevel 1 (
    echo.
    echo [ERREUR] Aucun javac fonctionnel trouve.
    echo Installe un JDK 17, 21 ou 24 et definis JAVA_HOME.
    echo Exemple : setx JAVA_HOME "C:\Program Files\Java\jdk-24"
    pause
    exit /b 1
)

rem --- 2. Verifier version minimale 17 ---
rem javac 1.8  -^> ancien format, javac 17/21/24 -^> nouveau format
"!JAVAC!" -version 2>&1 | findstr /C:"1.8" >nul
if not errorlevel 1 (
    echo.
    echo [ERREUR] Le javac detecte est Java 8 : "!JAVAC!"
    echo Le framework utilise pattern-matching -^> JDK 17 minimum requis.
    echo Definis JAVA_HOME vers un JDK recent.
    echo Exemple : setx JAVA_HOME "C:\Program Files\Java\jdk-24"
    pause
    exit /b 1
)

echo nettoyage...
if exist build rmdir /s /q build
mkdir build\classes
if exist sources.txt del sources.txt

echo compilation...
for /r src %%f in (*.java) do (
    echo %%f>> sources.txt
)

rem --release 17 : bytecode v61 (lisible par JDK 17 : compil du test + Tomcat).
rem -encoding UTF-8 : accents, -parameters : noms des parametres gardes (binding).
"!JAVAC!" --release 17 -encoding UTF-8 -parameters -cp "lib/*" -d build\classes @sources.txt

if errorlevel 1 (
    echo Compilation failed.
    del sources.txt
    pause
    exit /b 1
)

del sources.txt

echo creation du jar...
"!JARTOOL!" cf Framework.jar -C build\classes .

echo termine : Framework.jar genere (bytecode Java 17).
pause
endlocal
