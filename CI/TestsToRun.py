#-------------------------------------------------------------------------------
# Name:        TestsToRun
# Purpose:
#
# Author:      xiangll/zhniu/PanYuan
#
# Created:     17/12/2017
# Copyright:   (c) xiangll 2017
# Licence:     <your licence>
# Introduction: This module receives 7 parameters to get correct JSON file,
#               backup choosn PTF tests from E92PDVL to target DB, and then
#               execute those PTF tests.
# Usage example: python TestsToRun.py 84077 E9200TS1 email@123.com C:\D\SmartAutomation\CI\JSON_files EP92PROD slc05EHL.US.ORACLE.COM:8001 \\slcnas463.us.oracle.com\enterprise\QEShare\SmartAutomation\PTF_Log
# Notice: Any path should not incldue SPACE.
#-------------------------------------------------------------------------------

#!/usr/bin/python
# -*- coding UTF-8 -*-

import os
import re
import sys
import subprocess
import glob
import json
import time
import cx_Oracle
from datetime import datetime
from pywinauto import application

# Return JSON file name, including extension
def get_JSONFile(uow, exo, email, filePath):
    timeStamp = 0
    prefix = uow + "_" + exo + "_" + email
    filelist = os.listdir(filePath)
    size = len(filelist)
    result=[]
    while size > 0:
        size = size - 1
        if (re.match(prefix,filelist[size])):
            result.append(filelist[size])
        else:
            continue
    print str(len(result)) + " JSON files found with the same perfix."

    if(len(result)==1):
        fileName = result[0]
    elif(len(result)==0):
        print "There is no JSON file found by current criteria."
    elif(len(result)>1):
        length = len(result)
        while length>0:
            length = length - 1
            content = result[length].split('_')
            file_json = content[3].split('.')
            if(timeStamp==0):
                timeStamp = long(file_json[0])
                #print type(timeStamp)
                fileName = result[length]
            elif (long(file_json[0]) - long(timeStamp)) > 0:
                timeStamp = file_json[0]
                fileName = result[length]
            #print fileName
    print "JSON file found: " + fileName
    return fileName

##uow = sys.argv[1]
##exo = sys.argv[2]
##email = sys.argv[3]
##filePath = sys.argv[4]

# Loop execute PTF tests. Return number and name_list of NO RUN PTF tests
# External call example: diff_num, diff_list = TestsToRun.run_ptf_tests(DB_name, server_port, exo, log_dir)
def run_ptf_tests(DB_name, server_port, exo, log_dir):
    test_framework = '"C:\\Program Files\\PeopleSoft\\PeopleSoft Test Framework\\PsTestFw.exe"'
    plan_list = []  #List of tests planed to execute
    exec_list = []  #List of tests actually executed
    # Execute PTF tests product by product
    for product in (json_data["Products"]):
        for test in (json_data[product]):
            #execute_str = test_framework + " -CD=" + DB_name + " -CS=" + server_port + " -CO=VP1 -CP=VP1 -TST=" + test["test_Name"] + " -CUA=TRUE -TC=" + test["test_Case"] + " -EXO=" + exo + " -LOG=" + log_dir + "\\"  + json_name[:-5] + "\\" + test["test_Name"] + ".xml"
            os.system("if not exist " + log_dir + "\\"  + json_name[:-5] + " md " + log_dir + "\\"  + json_name[:-5])
            execute_str = test_framework + " -CS=" + server_port + " -CO=VP1 -CP=VP1 -TST=" + test["test_Name"] + " -CUA=TRUE -TC=" + test["test_Case"] + " -EXO=" + exo + " -LOG=" + log_dir + "\\"  + json_name[:-5] + "\\" + test["test_Name"] + ".xml"
            plan_list.append(test["test_Name"])
            ret = subprocess.call(execute_str, shell=True)
            if ret != 0:
                print("Exec %s with error return: %s" % (execute_str, str(ret)) )
            else:
                print("%s executed successfully." % (execute_str))

    exec_lists = os.listdir(log_dir + os.sep + json_name[:-5])
    for logxml in exec_lists:
        #log = logxml.split(".")
        log = os.path.splitext(logxml)
        exec_list.append(log[0])
    diff_num = len(plan_list) - len(exec_list)  # Number of no run tests
    diff_list = list(set(plan_list).difference(set(exec_list))) # List of no run tests' name
    print diff_num
    print diff_list
    return (diff_num, diff_list)

# Get tests' name that need to backup
def get_all_test(tests):
    sql_base = 'select distinct pttst_cmd_recog ' + \
               'from pspttstcommand where pttst_cmd_obj_type = 35000 and pttst_cmd_type = 35001 ' + \
               'start with pttst_name in (%s) connect by nocycle prior pttst_cmd_recog = pttst_name'
    con = cx_Oracle.connect(db_user_name, db_password, db_name)
    cur = con.cursor()
    tests_str = "'%s'" % "','".join(tests)
    sql = sql_base % tests_str
    cur.execute(sql)
    names_set = set()
    for test_item in cur:
        names_set.add(test_item[0])
    for test_name in tests:
        names_set.add(test_name)
    print names_set
    cur.close()
    con.close()
    return list(names_set)

# Login database
def login(app, db_name,User_ID,User_Password):
    login_window = app.top_window()
    login_window.Maximize()
    #login_window.print_control_identifiers()
    login_window['&Database Name:Edit0'].type_keys(db_name)
    login_window['&User ID:Edit'].type_keys(User_ID)
    login_window['&Password:Edit'].type_keys(User_Password)
    login_window.OKButton.click()

# Copy tests to target database
def add_test(app,tests,main_form):
    main_form.MenuSelect('Insert->Definitions into Project...')
    insert_form=app.top_window()
    insert_form.ComboBox.Select('Tests')
    for test in tests:
        insert_form.Edit.type_keys(test)
        insert_form.ListBox.select('Test Cases')
        #insert_form.print_control_identifiers()
        insert_form['&Insert'].click()
        item_count = insert_form.ListView.ItemCount()
        if item_count > 0:
            insert_form.ListView.select(0)
            insert_form['&Insert'].click()
    insert_form['&Close'].click()

def save_project(main_form,app,proj_name):
    main_form.MenuSelect('File->Save Project As')
    save_window=app.top_window()
    #save_window.print_control_identifiers()
    save_window['Save Project &Name As:Edit'].type_keys(proj_name)
    save_window.OKButton.click()

def CopyToPrj():
    os.system(copytoprjcmd)

def CopyFromPrj():
    os.system(copyfromprjcmd)


#---------------------------Start below:----------------------------------
script, uow, exo, email, filePath, DB_name, server_port, log_dir = sys.argv

# Get JSON file name
json_name = get_JSONFile(uow, exo, email, filePath)
# Get JSON file full path
json_file = os.path.join(filePath, json_name)
# Load and parse JSON file, store all tests' name to "tests" list
with open(json_file, 'r') as f:
    json_data = json.load(f)
tests = []
for product in (json_data["Products"]):
    for test in (json_data[product]):
       tests.append(test["test_Name"])

# Backup PTF tests from EP92PDVL
##db_name = 'E92AUQA1'
##ps_tool_path = 'C:\SmartAutomation\pt\bin\client\winx86\pside.exe'
##User_ID='VP1'
##User_Password='VP1'
##db_user_name='emdbo'
##db_password='emdbo123'

db_name = 'EP92PDVL'
ps_tool_path = 'C:\\SmartAutomation\\pt\\bin\\client\\winx86\\pside.exe'
User_ID='VP1'
User_Password='QEDMO'
db_user_name='people'
db_password='peop1e'
t_db_name= exo
t_ps_tool_path = 'C:\\SmartAutomation\\pt\\bin\\client\\winx86\\pside.exe'
t_User_ID='VP1'
t_User_Password='VP1'
#tests=['PYSHELL01','PYSHELL02']
#tests=['AR_TEST_CFL_0401_26223375','AR_TEST_CFL_0706_25990155','AR_TEST_CFL_1603_26038232','AR_TEST_CFL_0401_25455024','AR_TEST_CFL_0401_25455024','AR_TEST_CFL_0606_25837936','AR_TEST_CFL_0606_26039175','AR_TEST_CFL_1003_25830876','AR_TEST_CFL_1301_25406962','AR_TEST_CFL_1402_25607937']
proj_name='PTF_TEST'+str(time.strftime('%Y%m%d%H%M%S'))
copytoprjcmd = "%s -HIDE -PJTF %s -FP c:\\temp\\export -CT ORACLE -CD %s -CO %s -CP %s -QUIET -LF c:\\temp\\copytofile.log" % (ps_tool_path,proj_name,db_name,User_ID,User_Password)
copyfromprjcmd = "%s -HIDE -PJFF %s -FP c:\\temp\\export -CT ORACLE -CD %s -CO %s -CP %s -QUIET -LF c:\\temp\\copyfromfile.log" % (t_ps_tool_path,proj_name,t_db_name,t_User_ID,t_User_Password)

test_list = get_all_test(tests)
app = application.Application()
app.start(ps_tool_path)
time.sleep(15)
login(app, db_name,User_ID,User_Password)
time.sleep(15)
main_form=app.top_window()
add_test(app,test_list,main_form)
main_form=app.top_window()
save_project(main_form,app,proj_name)
main_form.close()
CopyToPrj()
CopyFromPrj()

# Execute PTF tests
run_ptf_tests(DB_name, server_port, exo, log_dir)


##if __name__ == '__main__':
##    run_ptf_tests("EP92PROD", "slc05EHL.US.ORACLE.COM:8001", "E92AUQA1", "\\\slcnas463.us.oracle.com\enterprise\QEShare\SmartAutomation\PTF_Log")
