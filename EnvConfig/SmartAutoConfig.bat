::auto install

:Initialize
CLS
@ECHO OFF
ECHO.

goto start
::Parameter examples. Path with blank space should be quoted with "" to avoid errors
set SARootPath = "C:\SmartAutomation"
set ScriptPath = "C:\SmartAutomation\Scripts"
set InstallPkg = "C:\SmartAutomation\InstallPkg"
set PTFLogPath = "C:\SmartAutomation\PTF_Log"
set JenJobPath = "C:\Temp\workspace\Env_GitDownload" 

:start
set SARootPath = "C:\SmartAutomation"
set ScriptPath = "%SARootPath%\Scripts"
set InstallPkg = "%SARootPath%\InstallPkg"
set PTFLogPath = "%SARootPath%\PTF_Log"
set JenJobPath = "C:\Temp\workspace\Env_GitDownload" 
if not exist %SARootPath% md %SARootPath%
if not exist %ScriptPath%  md %ScriptPath%
if not exist %InstallPkg%  md %InstallPkg%
xcopy %JenJobPath%\EnvConfig  %ScriptPath% /s /h /d /c /y
xcopy %JenJobPath%\CI         %ScriptPath% /s /h /d /c /y
xcopy \\den00qhy.us.oracle.com\C$\SmartAutomation\Scripts\SmartAnalyze.jar  %ScriptPath% /s /h /d /c /y
xcopy %JenJobPath%\AnalyzeUOW\DB_Util.ini      %ScriptPath% /s /h /d /c /y
xcopy %JenJobPath%\AnalyzeUOW\log4j2-test.xml  %ScriptPath% /s /h /d /c /y
xcopy %JenJobPath%\AnalyzeUOW\lib %ScriptPath% /s /h /d /c /y
xcopy %JenJobPath%\Execution %ScriptPath% /s /h /d /c /y
xcopy %JenJobPath%\Report    %ScriptPath% /s /h /d /c /y