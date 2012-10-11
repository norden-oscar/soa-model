@echo off
if not "%SOA_MODEL_HOME%" == "" goto homeSet
set "SOA_MODEL_HOME=%cd%"
if exist "%SOA_MODEL_HOME%\schemadiff.bat" goto homeOk

:homeSet
if exist "%SOA_MODEL_HOME%\schemadiff.bat" goto homeOk
echo Please set the SOA_MODEL_HOME environment variable to point to
echo the directory where you have extracted the SOA Model software 
echo and put it in the your path environment variable.
goto end

:homeOk
set "CLASSPATH=%CLASSPATH%;./lib/soa-model-core-1.2.0.jar"
set "CLASSPATH=%CLASSPATH%;./lib/commons-codec-1.3.jar"
set "CLASSPATH=%CLASSPATH%;./lib/commons-httpclient-3.1.jar"
set "CLASSPATH=%CLASSPATH%;./lib/commons-logging-1.1.1.jar"
set "CLASSPATH=%CLASSPATH%;./lib/groovy-all-1.8.6.jar"
set "CLASSPATH=%CLASSPATH%;./lib/commons-cli-1.2.jar"
set "CLASSPATH=%CLASSPATH%;./bin"
java  -classpath "%CLASSPATH%" org.membrane_soa.soa_model.diff.WSDLDiffCLI %1 %2 %3 %4 %5 %6

:end