# -*- coding: utf-8 -*-
# Author: ally.yu@oracle.com; nina.zhao@oracle.com#

import ConfigParser
import datetime
import smtplib
import os
import json
import xmltodict
from email.mime.text import MIMEText
import FindJSON
import sys


##--Beginning of Loading result.html##
jsonfolder = FindJSON.get_JSONFile(uow,exo,email,filePath)[:-5]
folder_name = filePath + os.sep + jsonfolder

cf = ConfigParser.ConfigParser()
cf.read("config.conf")
HTMLOUTPUT = cf.get("source_dir","LOGDIR")+folder_name+"/result.html"
# print HTMLOUTPUT

# load html report
fp=open(HTMLOUTPUT, 'rb')
msg=MIMEText(fp.read(), 'html','utf-8');
fp.close()

##--Ending of Loading result.html##

# Get the product selection from Web UI
# cfe=ConfigParser.ConfigParser()
# cfe.read("WebPar.txt")
# productsel= cfe.get("products","product")
productsel=sys.argv[5]
#Get the UOW and User info from JSON file
# path = "C:" + os.sep + "Python27" + os.sep + "EX" + os.sep + "84077_E92BISD2_daiqi@oracle.com_20171218221958.json"
filepath = sys.argv[4]
jsonfname = FindJSON.json_name
jsonfpath = os.path.join(filepath, jsonfname)
path=filePath+os.sep+jsonfilename
file=open(path)
fileJson= json.load(file)
file.close()
UOW = fileJson["UOW"]
BugNo = fileJson["BugNo"]
User = fileJson["User"]
Products = fileJson["Products"]
emails=[]
emails.append(User)
#Define the default product owner
ownerDict={"Expense": "ally.yu@oracle.com",
           "Billing": "qi.dai@oracle.com",
           "Accounts Receivable": "yue.xu@oracle.com",
           "eBill Payment": "qi.dai@oracle.com",
           "Purchasing": "aifeng.shi@oracle.com",
           "Accounts Receivable": "nina.zhao@oracle.com, dinga.du@oracle.com",
           "Billing": "qi.dai@oracle.com, dinga.du@oracle.com",
           "Inventory": "xiuyi.du@oracle.com",
           "Mobile Inventory":"xiuyi.du@oracle.com",
           "Order Management": "shuang.he@oracle.com",
           "Cost Management": "changqin.he@oracle.com",
           "Manufacturing": "changqin.he@oracle.com",
           "Supplier Scorecarding": "changqin.he@oracle.com",}
owneremails = []
if productsel == "*":
    for item in Products:
        owneremail  = ownerDict[item]
        if owneremail != 'NULL':
            owneremails+=owneremail.split(',')

else:
        owneremail = ownerDict[productsel]
        if owneremail != 'NULL':
            owneremails=owneremail.split(',')

#Email List Combine
# emails=emails+owneremails
emails.extend(owneremails)
emails=list(set(emails))
print emails

#Sendemail
msg['Subject'] = 'UOW'+UOW+' '+'Impacted Automation Test Result'
msg['From'] =cf.get("mail_server","From")
msg['To'] = ','.join(emails)
user = cf.get("mail_server", "ADRR")
password = cf.get("mail_server", "PWD")
s = smtplib.SMTP('ap6023fems.us.oracle.com')
s.sendmail(msg['From'], emails, msg.as_string())
s.quit()
