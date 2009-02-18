@rem ##########################################################################
@rem
@rem  Bootstrap script for running AribaWeb ant commands
@rem
@rem ##########################################################################
@if "%DEBUG%" == "" @echo off
@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

:begin
@rem Determine what directory it is in.
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.\

@rem Find Builtin Commands
set AWCMD=
if "x%~1" == "xcreate-project" set AWCMD="%~1"
if "x%~1" == "x" set AWCMD="Welcome"
if "x%~1" == "x-h" set AWCMD="help"
if "x%~1" == "x-help" set AWCMD="help"
if "x%~1" == "xhelp" set AWCMD="help"
if "x" == "x%AWCMD%" set OTHERCMD=%~1
shift

:check_AW_HOME
@rem Define AW_HOME if not set
if not "%AW_HOME%" == "" goto check_ANT_HOME
set AW_HOME=%DIRNAME%..
echo Setting AW_HOME.  To support running the AW command more easily, to this:
echo     set AW_HOME=%AW_HOME%
echo     set PATH=%%AW_HOME%%\bin;%%PATH%%
echo.

:check_ANT_HOME
if not "%ANT_HOME%" == "" goto init
set ANT_HOME=%AW_HOME%\tools\ant
set PATH=%ANT_HOME%\bin;%PATH%
echo Setting ANT_HOME to: %ANT_HOME%
set ANT_OPTS=-Xmx400m -XX:MaxPermSize=300m

:init
@rem Get command-line arguments, handling Windowz variants
if not "%OS%" == "Windows_NT" goto win9xME_args
if "%eval[2+2]" == "4" goto 4NT_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=1

:win9xME_args_slurp
if "x%~1" == "x" goto execute

rem horrible roll your own arg processing inspired by groovy / jruby equivalent

rem escape quotes (-q), minus (-d), star (-s).
set _ARGS=%*
if not defined _ARGS goto execute
set _ARGS=%_ARGS:-=-d%
set _ARGS=%_ARGS:"=-q%
rem Windowz will try to match * with files so we escape it here
rem but it is also a meta char for env var string substitution
rem so it can't be first char here, hack just for common cases.
rem If in doubt use a space or bracket before * if using -e.
set _ARGS=%_ARGS: *= -s%
set _ARGS=%_ARGS:)*=)-s%
set _ARGS=%_ARGS:0*=0-s%
set _ARGS=%_ARGS:1*=1-s%
set _ARGS=%_ARGS:2*=2-s%
set _ARGS=%_ARGS:3*=3-s%
set _ARGS=%_ARGS:4*=4-s%
set _ARGS=%_ARGS:5*=5-s%
set _ARGS=%_ARGS:6*=6-s%
set _ARGS=%_ARGS:7*=7-s%
set _ARGS=%_ARGS:8*=8-s%
set _ARGS=%_ARGS:9*=9-s%
rem prequote all args for 'for' statement
set _ARGS="%_ARGS%"

:win9xME_args_loop
rem split args by spaces into first and rest
for /f "tokens=1,*" %%i in (%_ARGS%) do call :get_arg "%%i" "%%j"
goto process_arg

:get_arg
rem remove quotes around first arg
for %%i in (%1) do set _ARG=%%~i
rem set the remaining args
set _ARGS=%2
rem return
goto :EOF

:process_arg
if "%_ARG%" == "" goto execute

rem now unescape -q, -d, -s
set _ARG=%_ARG:-q="%
set _ARG=%_ARG:-d=-%
set _ARG=%_ARG:-s=*%
if "x2" == "x%_SKIP%" goto skip_path
if "x1" == "x%_SKIP%" goto skip_main

set CMD_LINE_ARGS=%CMD_LINE_ARGS% %_ARG%
set _ARG=
goto win9xME_args_loop

:skip_path
set _ARG=
set _SKIP=1
goto win9xME_args_loop

:skip_main
set _ARG=
set _SKIP=0
goto win9xME_args_loop

:4NT_args
@rem Get arguments from the 4NT Shell from JP Software
set CMD_LINE_ARGS=%$

:execute
@rem Setup the command line

set ANT_EXE=%ANT_HOME%\bin\ant.bat

@rem Execute Ant
if "x" == "x%AWCMD%" goto other_command
echo Running Ant with built-in command %AWCMD%...
"%ANT_EXE%" -emacs -logger org.apache.tools.ant.NoBannerLogger -f %AW_HOME%\tools\build-commands.xml %AWCMD% %CMD_LINE_ARGS%
goto end

:other_command
echo Running other command "%OTHERCMD% with args "%CMD_LINE_ARGS%"
%OTHERCMD% %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" endlocal
