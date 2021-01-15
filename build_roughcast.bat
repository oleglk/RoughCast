@REM build_roughcast.bat

set _OLD_PATH=%PATH%
set PATH="C:\Program Files\Java\jdk-14.0.1\bin";%PATH%

set _ROOT=%~dp0
set _CP=%_ROOT%\classes

set _JAVAC=javac -g


cd %_ROOT%\src
@echo ======== Started  RoughCast compilation under "%_ROOT%\src" ========

@echo deleting class folder
rd /s/q %_ROOT%\classes
@echo recreating class folder
md %_ROOT%\classes

cd %_ROOT%\src\okutils
@echo ---- Begin compiling okutils subfolder "%CD%" ----
%_JAVAC% -d %_ROOT%\classes -cp %_CP% *.java
@echo ---- End   compiling okutils subfolder "%CD%" ----

cd %_ROOT%\src\roughcast
@echo ---- Begin compiling roughcast subfolder "%CD%" ----
%_JAVAC% -d %_ROOT%\classes -cp %_CP% *.java
@echo ---- End   compiling roughcast subfolder "%CD%" ----


REM ~ cd %_ROOT%\src
REM ~ @echo ---- Begin compiling RoughCast top-level folder "%CD%" ----
REM ~ %_JAVAC% -d %_ROOT%\classes -cp %_CP% *.java
REM ~ @echo ---- End   compiling RoughCast top-level folder "%CD%" ----



cd %_ROOT%
set PATH=%_OLD_PATH%
@echo ======== Finished RoughCast compilation under "%_ROOT%\src" ========