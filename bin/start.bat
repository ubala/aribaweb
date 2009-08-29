@rem ##########################################################################
@rem
@rem  Start up AribaWeb demos
@rem
@rem ##########################################################################
@if "%DEBUG%" == "" @echo off
@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

@rem Determine what directory it is in.
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.\

@rem Define AW_HOME if not set
if not "%AW_HOME%" == "" goto check_ANT_HOME
set AW_HOME=%DIRNAME%..
echo Setting AW_HOME.  To support running the AW command more easily, to this:
echo     set AW_HOME=%AW_HOME%
echo     set PATH=%%AW_HOME%%;%%PATH%%
echo.

echo Starting up demo app...
%AW_HOME%\bin\aw.bat ant -emacs -logger org.apache.tools.ant.NoBannerLogger -f %AW_HOME%\build.xml tomcat-build-browse

:end
@rem End local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" endlocal
@rem If we have any error messages, we want to keep the command window open long enough to display them
pause
