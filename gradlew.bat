@ECHO OFF
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@IF "%DEBUG%" == "" ECHO OFF
SETLOCAL

set DIRNAME=%~dp0
IF "%DIRNAME%" == "" SET DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% NEQ 0 (
  echo.
  echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
  echo.
  goto end
)

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME%
set JAVACMD=%JAVA_HOME%\bin\java.exe

"%JAVACMD%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
:end
