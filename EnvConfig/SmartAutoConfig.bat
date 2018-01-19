::This file is used to create working directory structure on slaves for SmartAutomation project
::Please run it before run SmartAutomation project
::Author: fangfang.jiang@oracle.com

:Initialize
CLS
@ECHO OFF
ECHO.

::set SARootPath="C:\SmartAutomation"
set ScriptPath="%SARootPath%\Scripts"
set InstallPkg="%SARootPath%\InstallPkg"
set PTFLogPath="%SARootPath%\PTF_Log"
::set JenJobPath="C:\Temp\workspace\Env_GitDownload"

echo create root working folder if not exist
if not exist "%SARootPath%" md "%SARootPath%"

echo create scripts folder if not exist
if not exist %ScriptPath% md %ScriptPath%

echo create install packages folder if not exist
if not exist %InstallPkg% md %InstallPkg%

ECHO Create PTF Log folder if not exist
if not exist "%PTFLogPath%" md "%PTFLogPath%"

echo copy git downloaded EnvConfig scripts to working scripts folder
xcopy "%JenJobPath%\EnvConfig" %ScriptPath% /s /h /d /c /y

echo copy git downloaded CI scripts to working scripts folder
xcopy "%JenJobPath%\CI"        %ScriptPath% /s /h /d /c /y

echo copy git downloaded Calculation scripts to working scripts folder
xcopy "%JenJobPath%\AnalyzeUOW\SmartAnalyze.jar" %ScriptPath% /s /h /d /c /y
xcopy "%JenJobPath%\AnalyzeUOW\DB_Util.ini" %ScriptPath% /s /h /d /c /y
xcopy "%JenJobPath%\AnalyzeUOW\log4j2-test.xml" %ScriptPath% /s /h /d /c /y
xcopy "%JenJobPath%\AnalyzeUOW\prodSetup.ini" %ScriptPath% /s /h /d /c /y
if not exist %ScriptPath%\lib md %ScriptPath%\lib
xcopy "%JenJobPath%\AnalyzeUOW\lib" %ScriptPath%\lib /s /h /d /c /y

echo copy git downloaded Execution scripts to working scripts folder
xcopy "%JenJobPath%\Execution" %ScriptPath% /s /h /d /c /y

echo copy git downloaded Report scripts to working scripts folder
xcopy "%JenJobPath%\Report" %ScriptPath% /s /h /d /c /y