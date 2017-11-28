# -*- coding: utf-8 -*-
import os
import sys
#import datetime

toolspath = "\\\\psbldfs.us.oracle.com\\dfs\\build\\pt\\ptdist\\pt856\\85604d\\retail\\WINX86\\client-tools\\bin\\client\\winx86\\"


insertprjcmd = "pside.exe -HIDE -CT ORACLE -CS -CD EP92PROD -CO VP1 -CP VP1 -PJR ImportTest -OBJ 104,105 -OVW 1 -LF c:\\temp\\importPTFtest.log -HIDE -QUIET -SS NO -SN NO"
copytoprjcmd = "pside.exe -HIDE -PJTF ImportTest -FP c:\\temp\\export -CT ORACLE -CD EP92PROD -CO VP1 -CP VP1 -QUIET -LF c:\\temp\\copytofile.log"
copyfromprjcmd = "pside.exe -HIDE -PJFF ImportTest -FP c:\\temp\\export -CT ORACLE -CD E92AUQA3 -CO VP1 -CP VP1 -QUIET -LF c:\\temp\\copyfromfile.log"
datamovercmd = "psdmtx.exe -CT ORACLE -CD E92AUQA3 -CO VP1 -CP VP1 -FP \\\\psbldfs.us.oracle.com\\dfs\\enterprise\\QEShare\\QEO-Partner\\DataMover_Scripts\\setupExecOption.dms"

def InsertPrj():
    #os.system(psidepath + " -HIDE -CT ORACLE -CS -CD E92AUQA2 -CO VP1 -CP VP1 -PJR TWILIO -OBJ 104,105 -OVW 1 -LF c:\\temp\\importPTFtest.log -HIDE -QUIET -SS NO -SN NO")
    os.system(toolspath + insertprjcmd)

def CopyToPrj():
    #os.system(psidepath + " -HIDE -PJTF TWILIO -FP c:\\temp\\export -CT ORACLE -CD E92AUQA2 -CO VP1 -CP VP1 -QUIET -LF c:\\temp\\copytofile.log")
    os.system(toolspath + copytoprjcmd)

def CopyFromPrj(tdbName):
    #os.system(psidepath + " -HIDE -PJFF TWILIO -FP c:\\temp\\export -CT ORACLE -CD " + tdbName + " -CO VP1 -CP VP1 -QUIET -LF c:\\temp\\copyfromfile.log")
    os.system(toolspath + "pside.exe -HIDE -PJFF ImportTest -FP c:\\temp\\export -CT ORACLE -CO VP1 -CP VP1 -QUIET -LF c:\\temp\\copyfromfile.log -CD " + tdbName)

def SetupExecuOption(execdbname):
    os.system(toolspath + "psdmtx.exe -CT ORACLE -CO VP1 -CP VP1 -FP \\\\psbldfs.us.oracle.com\\dfs\\enterprise\\QEShare\\QEO-Partner\\DataMover_Scripts\\setupExecOption.dms -CD " + execdbname)

#def SetupExecuOption():
    #os.system(toolspath + "psdmtx.exe -CT ORACLE -CO VP1 -CP VP1 -FP \\\\psbldfs.us.oracle.com\\dfs\\enterprise\\QEShare\\QEO-Partner\\DataMover_Scripts\\setupExecOption.dms -CD E92AUQA3")

if __name__ == '__main__':
    InsertPrj()
    CopyToPrj()
    CopyFromPrj(sys.argv[1])
    SetupExecuOption(sys.argv[1])
    
    #SetupExecuOption()
