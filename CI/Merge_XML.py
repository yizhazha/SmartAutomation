#!/usr/bin/python
# -*- coding UTF-8 -*-

import glob
import sys
import os
import copy
import fnmatch
import datetime
import xml.etree.cElementTree as ET
import FindJSON

# This module will merge single xml files to a dummy_shell xml.

##LOGDIR = "D:\Project\XML Logs"
##LOGPATH = "\\\slcnas463.us.oracle.com\enterprise\QEShare\PTF Log Archive\QA\SmartAutomation"
##SOURCE_XML = LOGDIR + "/*.xml"
##DUMMY_SHELL = "UOW_DB_EMAIL"
##DUMMY_XML = DUMMY_SHELL + ".xml"

class InfoList:
    def __init__(self, testName, testCase, status, xmlnode):
        self.testName = testName
        self.testCase = testCase
        self.status = status
        self.xmlnode = xmlnode

def getStartedTime(Files):
    files = glob.glob(Files)
    f = min(files, key=os.path.getmtime)
    t = os.path.getmtime(f)
    return datetime.datetime.fromtimestamp(t)

def createHeader(startedTime):
    root = ET.Element("execution")
    ET.SubElement(root, "Started").text = startedTime.strftime('%Y-%m-%d %H:%M:%S')
    CE = ET.SubElement(root, "Param")

    ET.SubElement(CE, "Database").text = "DB"
    ET.SubElement(CE, "TestName").text = DUMMY_SHELL
    ET.SubElement(CE, "TestCase").text = "DEFAULT"
    ET.SubElement(CE, "ExecOpt").text = "DB"
    ET.SubElement(root, "Status")
    ET.SubElement(root, "Test")

    return root

def createFooter(root, path):
    t = root.find("Test")
    ET.SubElement(t, "LogFolder").text = "QA"
    ET.SubElement(t, "LogName").text = DUMMY_SHELL

    l = ET.SubElement(root, "LogExport")
    ET.SubElement(l, "Path").text = path
    ET.SubElement(l, "File").text = DUMMY_XML
    ET.SubElement(l, "Exported").text = "True"

def indent(elem, level=0):
    i = "\n" + level * "  "
    j = "\n" + (level - 1) * "  "
    if len(elem):
        if not elem.text or not elem.text.strip():
            elem.text = i + "  "
        if not elem.tail or not elem.tail.strip():
            elem.tail = i
        for subelem in elem:
            indent(subelem, level + 1)
        if not elem.tail or not elem.tail.strip():
            elem.tail = j
    else:
        if level and (not elem.tail or not elem.tail.strip()):
            elem.tail = j
    return elem

def getSingleInfo(file):
    r = ET.parse(os.path.join(LOGDIR, file)).getroot()
    testName = r[1][1].text
    testCase = r[1][2].text
    status = r.find("Status").text

    if status == "Failed":
        # root.findall('ProcessStart[@Options="%s"]' % options)[-1].get('Id')
        m = r.findall('.//Msg/..')[-1]
        # m = r.find('.//Msg/..')
    else:
        m = r
    infoList = InfoList(testName, testCase, status, m)
    return infoList

def insertDummyXML(targetNode,singleInfo,execSeq,flag):

    execSeq = "Exec" + str(execSeq)
    CE = ET.SubElement(targetNode,execSeq)
    ET.SubElement(CE, "Name").text = singleInfo.testName
    ET.SubElement(CE, "Case").text = singleInfo.testCase
    if flag == "f":
        c = copy.deepcopy(singleInfo.xmlnode)
        CE.insert(2,c)

    ET.SubElement(CE, "Status").text = singleInfo.status

#if __name__ == "__main__":

#Start below:
##script, uow, exo, DB_name, email, log_dir, filePath, LOGPATH = sys.argv

uow = sys.argv[1]
exo = sys.argv[2]
DB_name = sys.argv[3]
email = sys.argv[4]
log_dir = sys.argv[5]
filePath = sys.argv[6]
LOGPATH = sys.argv[7]

#Get JSON file name, including extension
json_name = FindJSON.get_JSONFile(uow, exo, email, filePath)

LOGDIR = log_dir + os.sep + json_name[:-5]
#LOGPATH = "C:\SmartAutomation"
SOURCE_XML = LOGDIR + "/*.xml"
DUMMY_SHELL = uow + "_" + DB_name + "_" + email
DUMMY_XML = DUMMY_SHELL + ".xml"

root = createHeader(getStartedTime(SOURCE_XML))
filesList = fnmatch.filter(os.listdir(LOGDIR), '*.xml')
TestNode = root[3]

for index in range(len(filesList)):
    singleInfo = getSingleInfo(filesList[index])

    if singleInfo.status == "Failed":
        s = root.find('Status')
        s.text = "Failed"
        flag = "f"
    else:
        flag = "p"
    insertDummyXML(TestNode,singleInfo,index +1, flag)

createFooter(root, LOGPATH)
indent(root)
tree = ET.ElementTree(root)
tree.write(DUMMY_XML)