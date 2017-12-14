# -*- coding: utf-8 -*-
import ConfigParser
import datetime
import smtplib
import os
import json
from email.mime.text import MIMEText


##--Beginning of Loading result.html##
# get folder name by timestamp MMddYY
folder_name = datetime.datetime.now().strftime("%Y%m%d")

cf = ConfigParser.ConfigParser()
cf.read("config.conf")
HTMLOUTPUT = cf.get("source_dir","LOGDIR")+folder_name+"/result.html"

# load html report
fp=open(HTMLOUTPUT, 'rb')
msg=MIMEText(fp.read(), 'html','utf-8');
fp.close()

##--Ending of Loading result.html##

# Get the email address from Web UI
cfe=ConfigParser.ConfigParser()
cfe.read("WebPar.txt")
mails= cfe.get("send_email","email")
emails=[]
emails.append(mails.split(','))
# emails=tuple(emails)

#Get the UOW and Product info from JSON file
path = "C:" + os.sep + "Python27" + os.sep + "EX" + os.sep + "84077_E92BISD2_daiqi@oracle.com_20171212213204.json"
file=open(path)
fileJson= json.load(file)
UOW = fileJson["UOW"]
BugNo = fileJson["BugNo"]
Products = fileJson["Products"]
# Products = tuple(Products)

#Get the default product owner
cfdp=ConfigParser.ConfigParser()
cfdp.read("productowner.txt")
EXemail = cfdp.get("product_owner","Expense")
ARemail = cfdp.get("product_owner","Accounts Receivable")
if "Expense" in Products:
    EX = []
    EX.append(EXemail.split(','))
    emailex = EX
if "Accounts Receivable" in Products:
    AR = []
    AR.append(ARemail.split(','))
    emailar = AR

msg['Subject'] = 'UOW'+UOW+' '+'Impacted Automation Test Result'

#sendemail
emailf = []
emailf=emailex[0]+emailar[0]+emails[0]
emailf=list(set(emailf))
msg['From'] =cf.get("mail_server","From")
msg['To'] = ','.join(emailf)
user = cf.get("mail_server", "ADRR")
password = cf.get("mail_server", "PWD")
# send mail by SMTP server (SSL, 465)
#s = smtplib.SMTP_SSL('stbeehive.oracle.com')
s = smtplib.SMTP('ap6023fems.us.oracle.com')
#s.login(user, password)
s.sendmail(msg['From'], emailf, msg.as_string())
s.quit()
