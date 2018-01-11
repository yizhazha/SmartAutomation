::auto install

:Initialize
CLS
@ECHO OFF
ECHO.

goto start
::Parameter examples. Path with blank space should be quoted with "" to avoid errors
set PTFSrcPath = "\\psbldfs\dfs\enterprise\PsTestFrameWork\856\PTF856_04D"
set PTFWrkPath = "C:\Program Files\PeopleSoft\PeopleSoft Test Framework"
set PTSrcPath  = "\\psbldfs\dfs\build\pt\ptship\pt85604d\install_Windows.ora"
set PTWrkPath  = "C:\SmartAutomation\pt"
set InstallPkg = "C:\SmartAutomation\Software"
set PTFLogPath = "C:\SmartAutomation\PTF_Log"

:start
::ECHO Start to install Git - Ignore for slaves?
::start /wait %InstallPkg%\Git-2.15.1.2-64-bit.exe /SILENT /norestart

ECHO Start to copy/update PTF folder......
if not exist "%PTFWrkPath%" md "%PTFWrkPath%"
xcopy "%PTFSrcPath%\*.*" "%PTFWrkPath%" /s /h /d /c /y

ECHO Start to copy/update PeopleTools folder
if not exist "%PTWrkPath%" md "%PTWrkPath%"
xcopy "%PTSrcPath%\*.*" "%PTWrkPath%" /s /h /d /c /y

ECHO Download from master and copy to/update local installation package folder
if not exist "%InstallPkg%" md "%InstallPkg%"
xcopy "\\den00qhy.us.oracle.com\c$\SmartAutomation\Software\*.*"  "%InstallPkg%" /s /h /d /c /y

ECHO Create PTF Log folder if not exist
if not exist "%PTFLogPath%" md "%PTFLogPath%" 

ECHO Start to install VC so that pside.exe can work......
cd\
cd SmartAutomation
start /wait %InstallPkg%\vc_redist.x64.exe /q /norestart
ECHO install VC x64 successfully......
start /wait %InstallPkg%\vc_redist.x86.exe /q /norestart
ECHO install VC x86 successfully......

::Check if python2.7 is already installed. Continue to install silently if not installed and exit if else.
::Default folder is C:\python27
ECHO Start to install python2.7 x64......
cd\
cd SmartAutomation
start /wait %InstallPkg%\python-2.7.14.amd64.msi /qn
ECHO install python2.7 successfully......

echo start to set python sys path....... 
echo %path%|findstr /i "c:\python27"&&(goto run)  
echo check path....  
::wmic ENVIRONMENT create name="path",VariableValue="%path%c:\python27;"  
echo check python path...... 
wmic ENVIRONMENT where "name='path' and username='<system>'" set VariableValue="%path%c:\python27;"
echo apply path......
set path=%path%c:\python27;

:run
::Check if setuptools already installed
if exist C:\Python27\Scripts\easy_install*.exe (goto run2) else (goto run1)

:run1
::install setuptools
::replace mimetypes.py to avoid coding error during setuptools
ECHO replace mimetypes.py....
replace %InstallPkg%\mimetypes.py "C:\python27\Lib"
if errorlevel 0 echo Successfully installed!
::install setuptools
ECHO Start to install SETUPTOOLS......
cd %InstallPkg%
cd setuptools*
python setup.py install
if errorlevel 0 echo Successfully installed!

:run2
echo install setuptools successfully......
::Check if pip already installed
if exist C:\Python27\Scripts\pip*.exe (goto run4) else (goto run3)

:run3
::install pip 
ECHO Start to install PIP......
cd %InstallPkg%
cd pip*
python setup.py install
if errorlevel 0 echo Successfully installed!

:run4
echo install pip successfully......

echo start to set python scripts path......
echo %path%|findstr /i "c:\python27\scripts"&&(goto run5)  
echo check path....  
::wmic ENVIRONMENT create name="path",VariableValue="c:\python27\scripts;%path%"  
echo check python path...... 
wmic ENVIRONMENT where "name='path' and username='<system>'" set VariableValue="%path%c:\python27\scripts;"
echo apply path......
set path=%path%c:\python27\scripts;

:run5
ECHO Start to install required packages......
for /f %%p in (C:\SmartAutomation\Scripts\PyPackages.txt) do pip install %%p

pause
exit