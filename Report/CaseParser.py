# -*- coding: utf-8 -*-
import xmltodict
import json
import os
import ConfigParser
import datetime,logging
from optparse import OptionParser
logging.basicConfig(format='%(asctime)s-%(process)d-%(levelname)s: %(message)s')
log = logging.getLogger('result_analysis')
log.setLevel(logging.DEBUG)
import FindJSON



'''
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
'''
class GlobalParameters:

    def __init__(self, log_folder="", file_name=""):
        #self.source_dir = source_dir
        self.log_folder = log_folder
        self.file_name = file_name

        #self.log_dir = "%s%s\%s"%(self.source_dir,self.folder_name,self.file_name)
        self.log_dir = self.log_folder + '\\'+self.file_name
        self.result_xml = "%s\\report\\result.xml"%(self.log_folder)
        self.output = "%s\\report\\output.xml"%(self.log_folder)
        self.html_output = "%s\\report\\result.html"%(self.log_folder)
        self.report_folder = "%s\\report\\"%(self.log_folder)


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

        file_object = open(g_para.output, 'w')
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

    def getResultNum(self ,PassOrFailed):
        num = 0
        for case in self.testcaseList:
            if case.status == PassOrFailed:
                num = num + 1
        return str(num)

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
    def __init__(self, logExport):
        #self.path = loginfo["Path"] + "\\" + loginfo["File"];
        self.path = logExport["Path"];

def convertXMLToJson(path):
    f = open(path, 'r')
    #g = open("D:/test/log/84077_E92BISD2_daiqi@oracle.com_201712252030/84077_E92BISD2_daiqi@oracle.com.xml")
    #doc1 = xmltodict.parse(g)
    doc = xmltodict.parse(f)
    return json.loads(json.dumps(doc))


def findAllTestCase(caseDict):
    testCaseList = []
    for key in caseDict:
        if "Exec" in key:
            if "_TEST_" in caseDict[key]["Name"] or "GL_" in caseDict[key]["Name"] or "IN_" in caseDict[key]["Name"]:
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
            if "_TEST" in steps[key]["Name"] or "GL_" in steps[key]["Name"]:  # this is a testCase, just ignore
                continue

            step = TestStep(steps[key]["Name"], steps[key]["Status"], int(key[4:]))
            putStepIntoTestCase(testCaseList, step)


def caseParser(file):
    # convert xml to json
    jObj = convertXMLToJson(file)
    #print(jObj)
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


def generateHTMLReport(testResult,g_para):
    report_html = open(g_para.html_output, "wb")
    report_html.write("<html><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>\n")
    report_html.write("<style type=\"text/css\">\n")
    report_html.write("<!--\n")
    #report_html.write("body{margin:0; font-family:Tahoma;Simsun;font-size:12px;}\n") Comment by Xiuyi
    report_html.write("table{border:1px #E1E1E1 solid;}\n")
    report_html.write("td{border:1px #E1E1E1 solid;}\n")
    # report_html.write(".title {font-size: 12px; COLOR: #FFFFFF; font-family: Tahoma;}\n") Comment by Xiuyi
    # report_html.write(".desc {font-size: 12px; COLOR: #000000; font-family: Tahoma;}\n") Comment by Xiuyi
    # Modify HTML format by Xiuyi
    report_html.write(".title {font-size: 13px; COLOR: #000000; font-family: Segoe UI;background-color:rgb(201, 216, 242);text-align:center;}\n")
    report_html.write(".desc {font-size: 13px; COLOR: #000000; font-family: Segoe UI;text-align:center;}\n")
    report_html.write("font {font-weight:normal;font-family:Segoe UI;font-size: 20px;color:#0000FF;}\n")
    #report_html.write("p{font-size: 24px; color:#0000FF; font-family: Segoe UI; text-align:center ;font-weight: bold; padding-bottom:0px;margin-bottom:0px;}\n")
    report_html.write("-->\n")
    report_html.write("</style>\n")
    report_html.write("<br>")

    '''check runtime env'''
    param = testResult.testsuite_list[0].param
    #report_html.write("<head><center><font face=\"Segoe UI\" size=5 color=0xF0F0F>ENV</center></head>\n")
    report_html.write("<head><center style=\"margin-top:10px;\"><font>Environment</font></center></head>\n")
    #report_html.write("<p> ENV </p>\n")
    report_html.write("<body><table cellpadding=\"0\" cellspacing=\"0\" align=\"center\"\n>")
    #report_html.write("<tr bgcolor=#858585 align=center class=title><td width=\"250\">Server</td><td width=\"100\">User</td><td width=\"150\">ExecOpt</td></tr>\n")
    report_html.write("<tr class=title><td width=\"250\">DataBase</td><td width=\"150\">ExecOpt</td><td width=\"100\">Run Times</td></tr>\n")
    report_html.write("<tr class=desc><td><B>"+param.server+"</B></td><td>" + param.execopt+ "</td><td>" +str(len(testResult.testsuite_list))+ "</td></tr>\n")
    #report_html.write("<tr align=center bgcolor=#FFFFFF class=desc><td><B>"+param.server+"</B></td><td>"+param.user+"</td><td>" + param.execopt+ "</td></tr>\n")

    report_html.write("</table>\n")
    report_html.write("<br>")

    #TestSuite Summary
    report_html.write("<head><center style=\"margin-top:10px;\"><font>Summary</font></center></head>\n")
    report_html.write("<body><table cellpadding=\"0\" cellspacing=\"0\" align=\"center\"\n>")
    #report_html.write("<tr bgcolor=#858585 align=center class=title><td width=\"250\">Test Suite</td><td width=\"100\">Num of Testcase</td><td width=\"200\">LogPath</td><td width=\"100\">Result</td><td width=\"100\">Pass Rate</td></tr>\n")
    #report_html.write("<tr class=title><td width=\"60\">Run Time</td><td width=\"250\">Test Suite</td><td width=\"100\">Num of Testcase</td><td width=\"200\">LogPath</td><td width=\"100\">Result</td><td width=\"100\">Pass Rate</td><td width=\"500\">RichTextLog</td></tr>\n")
    report_html.write("<tr class=title><td width=\"60\">Run Time</td><td width=\"250\">Test Suite</td><td width=\"100\">Num of Testcase</td><td width=\"100\">Number of Pass</td><td width=\"100\">Number of Fail</td><td width=\"500\">RichTextLogPath</td></tr>\n")
#blue color: #034579
    for testsuite in testResult.testsuite_list:
        if testsuite.status == "Passed":
            report_html.write("<tr class=desc><td>" \
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
            report_html.write("<tr class=desc><td>" \
                              + str(testResult.testsuite_list.index(testsuite) + 1) + "</td>" + "<td>" \
                              + testsuite.param.testname + "</td>" + "<td>" \
                              + str(len(testsuite.testcaseList)) + "</td>" \
                             # + testsuite.file + "</td>"
                             # + testsuite.status + "</td><td style=\"color:red;\">" \
                             # + testsuite.getPassRate() + "</td>" + "<td><a href='" \
                             # + testsuite.loginfo.path + "'>" \
                              + "<td>" +testsuite.getResultNum("Passed")+ "</td>" \
                              + "<td>" + testsuite.getResultNum("Failed") + "</td>" \
                              + "<td>" + testsuite.loginfo.path + "</td> "
                              "</tr>\n")

    report_html.write("</table>\n")
    report_html.write("<br>")

    #TestCase Detail
    report_html.write("<head><center style=\"margin-top:10px;\"><font>TestCase Details</font></center></head>\n")
    report_html.write("<body><table cellpadding=\"0\" cellspacing=\"0\" align=\"center\"\n>")
    report_html.write(
        "<tr align=center class=title><td width=\"60\">Run Time</td><td width=\"150\">Test Suite</td><td width=\"200\">TestCaseName</td><td width=\"80\">Result</td><td width=\"300\">Error Message</td><td width=\"300\">Component</td></tr>\n")

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
                report_html.write("<tr class=desc><td>" \
                                  #+ str(testResult.testsuite_list.index(testsuite) + 1) + "</td>" + "<td>" \
                                  #+ testsuite.param.testname + "</td><td>" \
                                  # + str(exec_id) + "</B></td><td>" \
                                  + testcase_name + "</td><td style=\"color:green;\">" \
                                  + status + "</td><td>" + "None" + "</td><td>" + "None" + "</td></tr>\n")
            else:
               # failed_info = item.failedMsg.toString()
                msg =item.failedMsg.failedInfo["Msg"]
                component=item.failedMsg.failedInfo["Menu"]
                component = component+"."+ item.failedMsg.failedInfo["Component"]
                component = component+"()"
                report_html.write("<tr class=desc><td>" \
                                  #+ str(testResult.testsuite_list.index(testsuite) + 1) + "</td>" + "<td>" \
                                  #+ testsuite.param.testname + "</td><td>" \
                                  # + str(exec_id) + "</B></td><td>" \
                                  + testcase_name + "</td><td style=\"color:red;\">" \
                                  + status + "</td><td>" \
                                  + msg + "</td><td>" + component + "</td></tr>\n")

    report_html.write("</table>\n")
    report_html.write("<br>")


if __name__ == "__main__":

#    cf = ConfigParser.ConfigParser()
 #   cf.read("config.conf")
 #   source_dir = cf.get("source_dir","LOGDIR")

    # get the input
    usage = "usage: python -T <tag> -P <log path> "
    parser = OptionParser(usage=usage, epilog="")
    parser.add_option("-U","--UOW", type="string", help="UOW number", default="staging")
    parser.add_option("-D", "--DB", type="string", help="DB name", default="staging")
    parser.add_option("-E", "--Email", type="string", help="Emial info", default="staging")
    parser.add_option("-F", "--FolderPath", type="string", help="log folder name", default="staging")
#    parser.add_option("-P", "--Product", type="string", help="Product", default="staging")

#    parser.add_option("-F", "--folder", type="string", help="folder name", default="staging")
#    parser.add_option("-S", "--path", type="string", help="source folder name", default="staging")
    (options, args) = parser.parse_args()


    #read config file
    #folder_name = datetime.datetime.now().strftime("%Y%m%d")
    UOW_id = parser.values.UOW
    DB_info = parser.values.DB
    email_info = parser.values.Email
    source_dir = parser.values.FolderPath

    json_filename = FindJSON.get_JSONFile(UOW_id, DB_info, email_info, source_dir)
    print "The log folder name is:"+json_filename
    log_folder = source_dir + os.sep + json_filename
    print "The directory of log folder is:"+log_folder
    file_name = UOW_id+"_"+DB_info+"_"+email_info+".xml"
    print "The merged log file name is:"+file_name
    #init g_para
    g_para = GlobalParameters(log_folder, file_name)

 #   log.info("---------------------------Parameters-------------------------------------")
#    log.info("file name:     " + options.file)
 #   log.info("--------------------------------------------------------------------------")

    # create report folder
    if not os.path.exists(g_para.report_folder):
        print "Create report folder..."
        os.mkdir(g_para.report_folder)

    # delete report.html if it exists
    if os.path.exists(g_para.output):
        print "Remove existed report folder..."
        os.remove(g_para.output)

    testResult = TestResult()
    '''
    # loop all the test result in the folder
    files = os.listdir(g_para.log_dir)
    for file in files:
        if "xml" in file.split("."):
            # end with xml
            #target_file = g_para + file
            target_file = g_para.log_dir
            testsuite = caseParser(target_file)
            testResult.addTestSuite(testsuite)
'''

    print "Start to paser the xml log..."
    testsuite = caseParser(g_para.log_dir)
    testResult.addTestSuite(testsuite)
    print "Start to Generate HTML report..."
    generateHTMLReport(testResult, g_para)
    print "Completed to create result.html."
