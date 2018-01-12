import json
import smtplib
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
import sys
import os
import FindJSON
import ConfigParser


def sendhtmlemail():
    uow = sys.argv[1]
    exo = sys.argv[2]
    email = sys.argv[3]
    filepath = sys.argv[4]
    productsel = sys.argv[5]

    jsonfname = FindJSON.get_JSONFile(uow,exo,email,filepath)
    requester = sys.argv[3]
    jsonfpath = os.path.join(filepath, jsonfname)
    jsonfullpath = jsonfpath
    print jsonfullpath

#    print jsonfname
#    print jsonfullpath
    with open(jsonfullpath) as json_file:
        json_data = json.load(json_file)
#        print(json_data['Products'])
        prod_list = json_data['Products']
        if len(prod_list) > 0:
            email_content = """
            <html>
                <body text="#000000" bgcolor="#FFFFFF">
                <br>
                <br>
                <p class="MsoNormal"
                    style="mso-margin-top-alt:auto;mso-margin-bottom-alt:auto"><b><span
                    style="font-family:&quot;Verdana&quot;,sans-serif;color:red"
                    lang="EN-US">(This is an automated e-mail generated please do
                    not reply to this e-mail.)</span><span lang="EN-US"> </span></b><span
                    lang="EN-US"><o:p></o:p></span></p>
                <p class="MsoNormal"><b><span
                    style="font-family:&quot;Verdana&quot;,sans-serif;color:darkblue"
                    lang="EN-US">UOW</span></b><span lang="EN-US"> </span><span
                    style="font-family:&quot;Verdana&quot;,sans-serif" lang="EN-US">{},
                    <b><span style="color:darkblue">BugNo </span></b>{}
                    related PTF tests have been calculated and selected. <o:p></o:p></span>
                </p> """.format(json_data['UOW'], json_data['BugNo'])

            for prod in prod_list:
                email_content += """
                <p class="MsoNormal"><b><span
                    style="font-family:&quot;Verdana&quot;,sans-serif;color:darkblue"
                    lang="EN-US">Product</span></b><b><span
                    style="font-family:&quot;Verdana&quot;,sans-serif"
                    lang="EN-US"> </span></b><span
                    style="font-family:&quot;Verdana&quot;,sans-serif" lang="EN-US">{}<b>,
                    <span style="color:darkblue">Total</span></b> {} test;<o:p></o:p></span>
                </p> """.format(prod, len(json_data[prod]))

            email_content += """
            <br>
            <br>
            </body>
            </html>
            """
#            print(email_content)

            ownerDict = {"Expense": "ally.yu@oracle.com",
                         "Billing": "qi.dai@oracle.com",
                         "Accounts Receivable": "yue.xu@oracle.com",
                         "eBill Payment": "qi.dai@oracle.com",
                         "Purchasing": "aifeng.shi@oracle.com",
                         "Inventory": "xiuyi.du@oracle.com",
                         "Mobile Inventory": "xiuyi.du@oracle.com",
                         "Order Management": "shuang.he@oracle.com",
                         "Cost Management": "changqin.he@oracle.com",
                         "Manufacturing": "changqin.he@oracle.com",
                         "Supplier Scorecarding": "changqin.he@oracle.com", }

            owneremails = []
            if productsel == "ALL":
                owneremail = ownerDict[prod]
                if owneremail != 'NULL':
                    owneremails += owneremail.split(',')
            else:
                owneremail = ownerDict[productsel]
                if owneremail != 'NULL':
                    owneremails = owneremail.split(',')

    emails = []
    emails.append(requester)
    # Define the default product owner
    # ownerDict={"Expense": "ally.yu@oracle.com",
    #            "Billing": "qi.dai@oracle.com",
    #            "Accounts Receivable": "yue.xu@oracle.com",
    #            "eBill Payment": "qi.dai@oracle.com",
    #            "Purchasing": "aifeng.shi@oracle.com",
    #            "Accounts Receivable": "nina.zhao@oracle.com, dinga.du@oracle.com",
    #            "Billing": "qi.dai@oracle.com, dinga.du@oracle.com",
    #            "Inventory": "xiuyi.du@oracle.com",
    #            "Mobile Inventory":"xiuyi.du@oracle.com",
    #            "Order Management": "shuang.he@oracle.com",
    #            "Cost Management": "changqin.he@oracle.com",
    #            "Manufacturing": "changqin.he@oracle.com",
    #            "Supplier Scorecarding": "changqin.he@oracle.com",}


    # Email List Combine
    # emails=emails+owneremails
    emails.extend(owneremails)
    emails = list(set(emails))
    print emails

    cf = ConfigParser.ConfigParser()
    cf.read("config.conf")

    # me = "shuang.he@oracle.com.com"
    #    you = "shuang.he@oracle.com"

    msg = MIMEMultipart('alternative')
    msg['Subject'] = 'UOW %s (BugNo %s) Related PTF Test has been selected' % (json_data['UOW'], json_data['BugNo'])
#    msg['From'] = me
    msg['From'] = cf.get("mail_server", "From")
    msg['To'] = ','.join(emails)

    part1 = MIMEText(email_content, 'html')
    msg.attach(part1)
    s = smtplib.SMTP('ap6023fems.us.oracle.com')
    s.sendmail(msg['From'], emails, msg.as_string())
    s.quit()


if __name__ == "__main__":
    sendhtmlemail()




