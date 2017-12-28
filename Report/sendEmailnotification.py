import json
import smtplib
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
import sys
import os
import FindJSON
import ConfigParser


def sendhtmlemail():
    # Get the product selection from Web UI
    cfe = ConfigParser.ConfigParser()
    cfe.read("WebPar.txt")
    productsel = cfe.get("products", "product")
    print productsel

    jsonfname = FindJSON.json_name
    filepath = sys.argv[4]
    requester = sys.argv[3]
    jsonfpath = os.path.join(filepath, jsonfname)

    with open(jsonfpath) as json_file:
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

            ownerDict = {"Expense": "shuang.he@oracle.com",
                         "Billing": "shuang.he01@oracle.com",
                         "Accounts Receivable": "shuang.he@oracle.com",
                         "eBill Payment": "shuang.he@oracle.com",
                         "Purchasing": "shuang.he@oracle.com",
                         "Inventory": "shuang.he@oracle.com",
                         "Mobile Inventory": "shuang.he@oracle.com",
                         "Order Management": "shuang.he@oracle.com",
                         "Cost Management": "shuang.he@oracle.com",
                         "Manufacturing": "shuang.he@oracle.com",
                         "Supplier Scorecarding": "shuang.he@oracle.com", }

            owneremails = []
            if productsel == "*":
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

    me = "shuang.he@oracle.com.com"
    #    you = "shuang.he@oracle.com"

    msg = MIMEMultipart('alternative')
    msg['Subject'] = 'UOW %s (BugNo %s) Related PTF Test has been selected' % (json_data['UOW'], json_data['BugNo'])
    msg['From'] = me
    msg['To'] = ','.join(emails)

    part1 = MIMEText(email_content, 'html')
    msg.attach(part1)
    s = smtplib.SMTP('ap6023fems.us.oracle.com')
    s.sendmail(me, emails, msg.as_string())
    s.quit()


if __name__ == "__main__":
    sendhtmlemail()




