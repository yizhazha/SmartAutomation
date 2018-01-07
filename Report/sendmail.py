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
cf = ConfigParser.ConfigParser()
cf.read("config.conf")
uow=sys.argv[1]
exo=sys.argv[2]
email=sys.argv[3]
filepath = sys.argv[4]
productsel=sys.argv[5]
# jsonfolder = FindJSON.get_JSONFile(uow,exo,email,filePath)[:-5]
jsonfname = FindJSON.json_name
jsonfolder=jsonfname[:-5]
folder_name = filepath + os.sep + jsonfolder
# jsonfpath = os.path.join(filepath, jsonfname)
path=filepath+os.sep+jsonfname
file=open(path)
fileJson= json.load(file)
file.close()
UOW = fileJson["UOW"]
BugNo = fileJson["BugNo"]
User = fileJson["User"]
Products = fileJson["Products"]
emails=[]
emails.append(User)
HTMLOUTPUT = cf.get("source_dir","LOGDIR")+jsonfolder+"/result.html"
# load html report
fp=open(HTMLOUTPUT, 'rb')
msg=MIMEText(fp.read(), 'html','utf-8');
fp.close()

##--Ending of Loading result.html##

# Get the product selection from Web UI
# cfe=ConfigParser.ConfigParser()
# cfe.read("WebPar.txt")
# productsel= cfe.get("products","product")

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
if productsel == "ALL":
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
