import ConfigParser
import datetime
import smtplib
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
# file_dir="C:\Python27\EX"
cfe=ConfigParser.ConfigParser()
# file_ename=file_dir+"/test.txt"
cfe.read("WebPar.txt")

mails= cfe.get("send_email","email")
emails=[]
emails.append(mails.split(','))
emails=tuple(emails)

#Get the UOW and Product info from JSON file

#Get the default product owner
cfdp=ConfigParser.ConfigParser()
cfdp.read("productowner.txt")
# uownum=cfdp.get("UOW","UOW")
# msg['Subject'] = 'UOW'+uownum+' '+'Impacted Automation Test Result'

#sendemail
msg['From'] =cf.get("mail_server","From")
msg['To'] = ','.join(emails[0])
user = cf.get("mail_server", "ADRR")
password = cf.get("mail_server", "PWD")
# send mail by SMTP server (SSL, 465)
#s = smtplib.SMTP_SSL('stbeehive.oracle.com')
s = smtplib.SMTP('ap6023fems.us.oracle.com')
#s.login(user, password)
s.sendmail(msg['From'], emails[0], msg.as_string())
s.quit()
