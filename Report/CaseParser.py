# -*- coding: utf-8 -*-
import xmltodict
import json
import os
import ConfigParser
import datetime

# get folder name by timestamp MMddYY
folder_name = datetime.datetime.now().strftime("%Y%m%d")

cf = ConfigParser.ConfigParser()

# read config file
cf.read("config.conf")
SOURCE_DIR = cf.get("source_dir", "LOGDIR")
# return all items
LOGDIR = SOURCE_DIR + folder_name + "/"
RESULT_XML = SOURCE_DIR + folder_name + "/log-new.xml"
OUTPUT = SOURCE_DIR + folder_name + "/result.xml"
HTMLOUTPUT = SOURCE_DIR + folder_name + "/result.html"

# delete report.html if it exists
if os.path.exists(OUTPUT):
    os.remove(OUTPUT)
if os.path.exists(OUTPUT):
    os.remove(HTMLOUTPUT)

files = os.listdir(LOGDIR)

MAX = 9999999999


class Param():
    def __init__(self, jobj):
        if jobj["execution"]["Param"].__contains__("Server"):
            self.server = jobj["execution"]["Param"]["Server"]
        else:
            self.server = jobj["execution"]["Param"]["Database"]
        if jobj["execution"]["Param"].__contains__("User"):
            self.user = jobj["execution"]["Param"]["User"]
        else:
            self.user = "VP1"
        self.testname = jobj["execution"]["Param"]["TestName"]
        self.execopt = jobj["execution"]["Param"]["ExecOpt"]

    def toDict(self):
        dict = {}
        dict["server"] = self.server
        dict["user"] = self.user
        dict["testname"] = self.testname
        dict["execopt"] = self.execopt
        return dict


class FailedMsg():
    def __init__(self, Line, FailedInfo):
        self.line = Line
        self.failedInfo = FailedInfo

    def toDict(self):
        doc = {"Line": self.line}
        doc.update(self.failedInfo)
        return doc

    def toString(self):
        detail = "\n"
        for key in self.failedInfo:
            detail = detail + key + ": " + self.failedInfo[key] + "\n"

        result = "Failed Line: " + str(self.line) + "\n" + "Detail Info: " + detail
        return result


class TestResult():
    def __init__(self):
        self.testsuite_list = []

    def addTestSuite(self, testsuite):
        self.testsuite_list.append(testsuite)


class TestSuite():
    def __init__(self, starttime, param, loginfo, status, file):
        self.starttime = starttime
        self.param = param
        self.status = status
        self.testcaseList = []
        self.loginfo = loginfo
        self.file = file

    def dumpToXML(self):
        doc = {}
        doc["starttime"] = self.starttime
        doc["status"] = self.status
        doc["param"] = self.param.toDict()
        doc["testcases"] = self.dumpTestcaseList()
        doc["loginfo"] = self.loginfo

        result = {"result": doc}
        convertedXml = xmltodict.unparse(result)

        file_object = open(OUTPUT, 'w')
        file_object.write(convertedXml)
        file_object.close()

    def dumpTestcaseList(self):
        doc = []
        for item in self.testcaseList:
            doc.append(item.toDict())

        return doc

    def getPassRate(self):
        passRate = 0.0
        numPass = 0
        numFailed = 0

        for case in self.testcaseList:
            if case.status == "Passed":
                numPass = numPass + 1
            else:
                numFailed = numFailed + 1
        if float(numPass + numFailed) == 0:
            passRate = "N/A"
            return str(passRate)
        else:
            RawpassRate = (float(numPass)) / float(numPass + numFailed)
            passRate = round(RawpassRate, 3)
            return str(passRate * 100) + "%"


class TestStep():
    def __init__(self, name, status, id):
        self.name = name
        self.status = status
        self.id = id

    def toDict(self):
        doc = {}
        doc["name"] = self.name
        doc["status"] = self.status
        doc["id"] = self.id
        return doc


class TestCase():
    def __init__(self, name, status, id):
        self.name = name
        self.status = status
        self.stepList = []
        self.start_step_index = -1
        self.end_step_index = -1
        self.id = id

    def addStep(self, step):
        self.stepList.append(step)
        self.stepList.sort(lambda x, y: cmp(x.id, y.id))

    def setStepIndex(self, start_index, end_index):
        self.start_step_index = start_index
        self.end_step_index = end_index

    def addFailedInfo(self, failedInfo, failedLine):
        self.failedMsg = FailedMsg(int(failedLine[7:]), failedInfo)

    def toDict(self):
        doc = {}
        doc["name"] = self.name
        doc["status"] = self.status
        doc["id"] = self.id
        doc["steps"] = self.dumpStepList()

        if self.status != "Passed":
            doc["failedMsg"] = self.failedMsg.toDict()
        return doc

    def dumpStepList(self):
        steps = []
        for item in self.stepList:
            steps.append(item.toDict())
        return steps


class LogExport():
    def __init__(self, loginfo):
        self.path = loginfo["Path"] + "\\" + loginfo["File"];


def convertXMLToJson(path):
    f = open(path, 'r')
    doc = xmltodict.parse(f)
    return json.loads(json.dumps(doc))


def findAllTestCase(caseDict):
    testCaseList = []
    for key in caseDict:
        if "Exec" in key:
            if "EX_TEST" in caseDict[key]["Name"]:
                # this is test case
                case = TestCase(caseDict[key]["Name"], caseDict[key]["Status"], int(key[4:]))
                for item in caseDict[key].keys():
                    if "LogLine" in item:
                        # has failed log info
                        case.addFailedInfo(caseDict[key][item], item)

                testCaseList.append(case)

    # sort the testCaseList according to ID
    testCaseList.sort(cmp=lambda x, y: cmp(x.id, y.id))

    return testCaseList


def putStepIntoTestCase(testCaseList, step):
    for item in testCaseList:
        if step.id > item.start_step_index and step.id < item.end_step_index:
            item.addStep(step)


def mapStepToTestCase(testCaseList, steps):
    for key in steps:
        if "Exec" in key:
            if "EX_TEST" in steps[key]["Name"]:  # this is a testCase, just ignore
                continue

            step = TestStep(steps[key]["Name"], steps[key]["Status"], int(key[4:]))
            putStepIntoTestCase(testCaseList, step)


def caseParser(file):
    # convert xml to json
    jObj = convertXMLToJson(file)
    # print(jObj)
    param = Param(jObj)
    loginfo = LogExport(jObj["execution"]["LogExport"])
    testsuite = TestSuite(jObj["execution"]["Started"], param, loginfo, jObj["execution"]["Status"], file)

    testCaseList = findAllTestCase(jObj["execution"]["Test"])
    for i in range(0, len(testCaseList)):  # need sort
        cur_case = testCaseList[i]
        if i == len(testCaseList) - 1:
            # this is the last case
            cur_case.setStepIndex(cur_case.id, MAX)
        else:
            next_case = testCaseList[i + 1]
            cur_case.setStepIndex(cur_case.id, next_case.id)

    mapStepToTestCase(testCaseList, jObj["execution"]["Test"])

    testsuite.testcaseList = testCaseList
    testsuite.dumpToXML()

    return testsuite


def output(testCaseList):
    for item in testCaseList:
        print "TestCase: " + item.name + " : " + item.status
        for step in item.stepList:
            print "TestStep: " + step.name + " : " + step.status


def generateHTMLReport(testResult):
    report_html = open(HTMLOUTPUT, "wb")
    report_html.write("<html><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>\n")
    report_html.write("<style type=\"text/css\">\n")
    report_html.write("<!--\n")
    report_html.write("body{margin:0; font-family:Tahoma;Simsun;font-size:12px;}\n")
    report_html.write("table{border:1px #E1E1E1 solid;}\n")
    report_html.write("td{border:1px #E1E1E1 solid;}\n")
    report_html.write(".title {font-size: 12px; COLOR: #FFFFFF; font-family: Tahoma;}\n")
    report_html.write(".desc {font-size: 12px; COLOR: #000000; font-family: Tahoma;}\n")
    report_html.write("-->\n")
    report_html.write("</style>\n")
    report_html.write("<br>")

    '''check runtime env'''
    param = testResult.testsuite_list[0].param
    report_html.write("<head><center><font face=\"黑体\" size=5 color=0xF0F0F>ENV</center></head>\n")
    report_html.write("<body><table cellpadding=\"0\" cellspacing=\"0\" align=\"center\"\n>")
    # report_html.write("<tr bgcolor=#858585 align=center class=title><td width=\"250\">Server</td><td width=\"100\">User</td><td width=\"150\">ExecOpt</td></tr>\n")
    report_html.write(
        "<tr bgcolor=#858585 align=center class=title><td width=\"250\">DataBase</td><td width=\"150\">ExecOpt</td><td width=\"100\">Run Times</td></tr>\n")
    report_html.write(
        "<tr align=center bgcolor=#FFFFFF class=desc><td><B>" + param.server + "</B></td><td>" + param.execopt + "</td><td>" + str(
            len(testResult.testsuite_list)) + "</td></tr>\n")
    # report_html.write("<tr align=center bgcolor=#FFFFFF class=desc><td><B>"+param.server+"</B></td><td>"+param.user+"</td><td>" + param.execopt+ "</td></tr>\n")

    report_html.write("</table>\n")
    report_html.write("<br>")

    # TestSuite Summary
    report_html.write("<head><center><font face=\"黑体\" size=5 color=0xF0F0F> Summary </center></head>\n")
    report_html.write("<body><table cellpadding=\"0\" cellspacing=\"0\" align=\"center\"\n>")
    # report_html.write("<tr bgcolor=#858585 align=center class=title><td width=\"250\">Test Suite</td><td width=\"100\">Num of Testcase</td><td width=\"200\">LogPath</td><td width=\"100\">Result</td><td width=\"100\">Pass Rate</td></tr>\n")
    report_html.write(
        "<tr bgcolor=#858585 align=center class=title><td width=\"60\">Run Time</td><td width=\"250\">Test Suite</td><td width=\"100\">Num of Testcase</td><td width=\"200\">LogPath</td><td width=\"100\">Result</td><td width=\"100\">Pass Rate</td><td width=\"500\">RichTextLog</td></tr>\n")
    # blue color: #034579
    for testsuite in testResult.testsuite_list:
        if testsuite.status == "Passed":
            report_html.write("<tr align=center bgcolor=#FFFFFF class=desc><td>" \
                              + str(testResult.testsuite_list.index(testsuite) + 1) + "</td>" + "<td>" \
                              + testsuite.param.testname + "</td>" + "<td>" \
                              + str(len(testsuite.testcaseList)) + "</td>" + "<td>" \
 \
                              + testsuite.file + "</td><td style=\"color:green;\">" \
                              + testsuite.status + "</td><td style=\"color:green;\">" \
                              + testsuite.getPassRate() + "</td>" + "<td><a href='" \
                              + testsuite.loginfo.path + "'>" \
                              + testsuite.loginfo.path + "</a></td></tr>\n")
        else:
            report_html.write("<tr align=center bgcolor=#FFFFFF class=desc><td>" \
                              + str(testResult.testsuite_list.index(testsuite) + 1) + "</td>" + "<td>" \
                              + testsuite.param.testname + "</td>" + "<td>" \
                              + str(len(testsuite.testcaseList)) + "</td>" + "<td>" \
                              + testsuite.file + "</td><td style=\"color:red;\">" \
                              + testsuite.status + "</td><td style=\"color:red;\">" \
                              + testsuite.getPassRate() + "</td>" + "<td><a href='" \
                              + testsuite.loginfo.path + "'>" \
                              + testsuite.loginfo.path + "</a></td></tr>\n")

    report_html.write("</table>\n")
    report_html.write("<br>")

    # TestCase Detail
    report_html.write("<head><center><font face=\"黑体\" size=5 color=0xF0F0F>TestCase Details</center></head>\n")
    report_html.write("<body><table cellpadding=\"0\" cellspacing=\"0\" align=\"center\"\n>")
    report_html.write(
        "<tr bgcolor=#858585 align=center class=title><td width=\"60\">Run Time</td><td width=\"150\">Test Suite</td><td width=\"200\">TestCaseName</td><td width=\"80\">Result</td><td width=\"300\">Error Message</td><td width=\"300\">Component</td></tr>\n")

    for testsuite in testResult.testsuite_list:
        testcaseList = testsuite.testcaseList

        length = str(len(testcaseList) + 1)
        run_time = str(testResult.testsuite_list.index(testsuite) + 1)

        report_html.write("<tr class=desc ><td rowspan=" + length + ">" \
                          + run_time + "</td>"
                          + "<td rowspan=" + length + ">" \
                          + testsuite.param.testname + "</td></tr>\n")

        for item in testcaseList:
            exec_id = item.id
            testcase_name = item.name
            num_of_step = len(item.stepList)
            status = item.status

            if status == "Passed":
                report_html.write("<tr align=center bgcolor=#FFFFFF class=desc><td>" \
                                  #+ str(testResult.testsuite_list.index(testsuite) + 1) + "</td>" + "<td>" \
                                  #+ testsuite.param.testname + "</td><td>" \
                                  # + str(exec_id) + "</B></td><td>" \
                                  + testcase_name + "</td><td style=\"color:green;\">" \
                                  + status + "</td><td>" + "None" + "</td><td>" + "None" + "</td></tr>\n")
            else:
                failed_info = item.failedMsg.toString()
                report_html.write("<tr align=center bgcolor=#FFFFFF class=desc><td>" \
                                  #+ str(testResult.testsuite_list.index(testsuite) + 1) + "</td>" + "<td>" \
                                  #+ testsuite.param.testname + "</td><td>" \
                                  # + str(exec_id) + "</B></td><td>" \
                                  + testcase_name + "</td><td style=\"color:red;\">" \
                                  + status + "</td><td>" \
                                  + failed_info + "</td><td>" + "testing" + "</td></tr>\n")

    report_html.write("</table>\n")
    report_html.write("<br>")


if __name__ == "__main__":
    # main()
    testResult = TestResult()
    # loop all the test result in the folder
    files = os.listdir(LOGDIR)
    for file in files:
        if "xml" in file.split("."):
            # end with xml
            target_file = LOGDIR + file
            testsuite = caseParser(target_file)
            testResult.addTestSuite(testsuite)

    generateHTMLReport(testResult)

    #  os.system('mutt -e ' + '"set content_type=text/html"' + " -s" + ' "Results of Automated Testing"' + " -c" + " " + "nina.zhao@oracle.com" + " " + "nina.zhao@oracle.com" + " < result")
