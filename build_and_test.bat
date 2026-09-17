@echo off
echo ===================================================
echo Building IgnitionAI 2.0 Phase 1 Modules...
echo ===================================================

if not exist "out" mkdir "out"

rem Compile all modules
powershell -Command "Get-ChildItem -Recurse -Filter *.java | ForEach-Object { '\"' + $_.FullName.Replace('\', '/') + '\"' } | Out-File -Encoding ascii sources.txt"
javac -encoding UTF-8 -d out @sources.txt
del sources.txt

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    exit /b %ERRORLEVEL%
)

echo.
echo ===================================================
echo Running Tests...
echo ===================================================

echo [TEST] Technical Sources
java -cp out com.ignitionai.technicalsources.test.TechnicalSourceTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

echo.
echo [TEST] Fact Extraction
java -cp out com.ignitionai.factextraction.test.FactExtractionTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

echo.
echo [TEST] Consistency Checks
java -cp out com.ignitionai.consistencychecks.test.ConsistencyCheckTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

echo.
echo [TEST] Linked Registries
java -cp out com.ignitionai.linkedregistries.test.LinkedRegistriesTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

echo.
echo [TEST] Vehicle Context (Phase 3)
java -cp out com.ignitionai.context.VehicleContextTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

echo.
echo [TEST] Direct Features (Phase 3)
java -cp out com.ignitionai.features.DirectFeaturesTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

echo.
echo [TEST] Virtual Sensors (Phase 3)
java -cp out com.ignitionai.virtualsensors.VirtualSensorsTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

echo.
echo ===================================================
echo ALL TESTS PASSED SUCCESSFULLY!
echo ===================================================
exit /b 0
