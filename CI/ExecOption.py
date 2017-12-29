# _*_ coding:UTF-8 _*_
'''

@author: zhniu
'''
import sys
import os
import FindJSON

uow = sys.argv[1]
exo = sys.argv[2]
email = sys.argv[3]
filePath = sys.argv[4]
URL = sys.argv[5]
hostName = URL.split['.'][0].split('//')[1]

json = FindJSON.get_JSONFile(uow,exo,email,filePath)[:-5]

def set_ExecutionOption(exo,hostName):
    dms = file(os.getcwd()+"\setupExecOption.dms", "w+")
    print dms
    line1 = """SET LOG \\\psbldfs.us.oracle.com\dfs\enterprise\QEShare\QEO-Partner\DataMover_Scripts\setupExecOption.log;

    --ACTIVE THE PTTST_CONFIG_NO_SSL--
    UPDATE PSOPRVERDFN SET ACTIVE_FLAG = 'A' where IB_OPERATIONNAME = 'PTTST_CONFIG_NO_SSL';
    COMMIT;"""
    dms.writelines(line1)
    dms.writelines('\n')
    line2 = """--Add Execution Option--
    delete from PSPTTSTOPTIONS where PTTST_EXOP_NAME = '%s';
    COMMIT;""" % exo
    dms.writelines(line2)
    dms.writelines('\n')
    line3 = """Insert into PSPTTSTOPTIONS values (
            '%s',
            'N',
            'http://%s.us.oracle.com:8000/psp/%sx/?&cmd=login&languageCd=ENG',
            'CH',
            'VP1',
            '1ENC1EED306F6AFC3A3E96F980DB62B88A9ACECDC0EF',
            'PSUNX',
            'QA',
            'Y',
            'N',
            'N',
            'N',
            'N',
            'MM/DD/YYYY',
            0,
            '\\\psbldfs\dfs\\build\pt\ptship\856\install_Windows.ora',
            '%s',
            'VP1',
            '1ENC1EED306F6AFC3A3E96F980DB62B88A9ACECDC0EF',
            ' ',
            'ORACLE',
            '\\\psbldfs.us.oracle.com\dfs\enterprise\QEShare\QEO-Partner\DataMover_Scripts',
            '\\\psbldfs.us.oracle.com\dfs\enterprise\QEShare\QEO-Partner\DataMover_Scripts',
            '\\\psbldfs.us.oracle.com\dfs\enterprise\QEShare\QEO-Partner\DataMover_Scripts',
            'Y',
            '\\\slcnas463.us.oracle.com\enterprise\QEShare\PTF_Log\%s ',
            '\\\slcnas463.us.oracle.com\enterprise\PTF\Logs\ToolsAutoStyle.xsl ',
            'Y',
            'Y',
            'N'
        );
    COMMIT;""" % (exo, hostName, exo.lower(), exo,json)
    dms.writelines(line3)
    dms.writelines('\n')
    line4 = """delete from PSPTTSTOPT_URL where PTTST_EXOP_NAME = '%s';
    COMMIT;""" % exo
    dms.writelines(line4)
    dms.writelines('\n')
    line5 = """Insert into PSPTTSTOPT_URL values (
            '%s',
            'PORTAL',
            'http://%s.us.oracle.com:8000/psc/%sx/EMPLOYEE/ERP/'
        );
    COMMIT;""" % (exo, hostName, exo.lower())
    dms.writelines(line5)


if __name__ == '__main__':
    set_ExecutionOption(exo,hostName)







