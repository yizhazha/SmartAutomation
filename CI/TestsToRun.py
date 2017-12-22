#-------------------------------------------------------------------------------
# Name:        TestsToRun
# Purpose:
#
# Author:      xiangll/zhniu
#
# Created:     17/12/2017
# Copyright:   (c) xiangll 2017
# Licence:     <your licence>
# Introduction: This module receives 7 parameters to get correct JSON file and execute PTF tests.
# Usage example: python TestsToRun.py 84077 E9200TS1 email@123.com C:\D\SmartAutomation\CI\JSON_files EP92PROD slc05EHL.US.ORACLE.COM:8001 \\slcnas463.us.oracle.com\enterprise\QEShare\SmartAutomation\PTF_Log
# Notice: Any path should not incldue SPACE.
#-------------------------------------------------------------------------------

import os
import re
import sys
import subprocess
import glob
import json
import time
from datetime import datetime

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
            execute_str = test_framework + " -CD=" + DB_name + " -CS=" + server_port + " -CO=VP1 -CP=VP1 -TST=" + test["test_Name"] + " -CUA=TRUE -TC=" + test["test_Case"] + " -EXO=" + exo + " -LOG=" + log_dir + "\\" + test["test_Name"] + ".xml"
            plan_list.append(test["test_Name"])
            ret = subprocess.call(execute_str, shell=True)
            if ret != 0:
                print("Exec %s with error return: %s" % (execute_str, str(ret)) )
            else:
                print("%s executed successfully." % (execute_str))

    exec_lists = os.listdir(log_dir)
    for logxml in exec_lists:
        #log = logxml.split(".")
        log = os.path.splitext(logxml)
        exec_list.append(log[0])
    diff_num = len(plan_list) - len(exec_list)  # Number of no run tests
    diff_list = list(set(plan_list).difference(set(exec_list))) # List of no run tests' name
    print diff_num
    print diff_list
    return (diff_num, diff_list)

#Start below:
script, uow, exo, email, filePath, DB_name, server_port, log_dir= sys.argv

#Get JSON file name
json_name = get_JSONFile(uow, exo, email, filePath)
#Get JSON file full path
json_file = os.path.join(filePath, json_name)
#Load JSON file
with open(json_file, 'r') as f:
    json_data = json.load(f)
#Execute PTF tests
run_ptf_tests(DB_name, server_port, exo, log_dir)


##if __name__ == '__main__':
##    run_ptf_tests("EP92PROD", "slc05EHL.US.ORACLE.COM:8001", "E92AUQA1", "\\\slcnas463.us.oracle.com\enterprise\QEShare\SmartAutomation\PTF_Log")