# -*- coding: utf-8 -*-
# Author: ally.yu@oracle.com#
import json
import  os
import csv
path = "C:" + os.sep + "Python27" + os.sep + "EX" + os.sep + "84077_E92BISD2_daiqi@oracle.com_20171218221958.json"
file=open(path)
fileJson= json.load(file)
file.close()
products = fileJson["Products"]
uow = fileJson["UOW"]
fileobj=open('selectedtest.csv', 'wb')
fileobj.write('\xEF\xBB\xBF') #this is to resove the encoding issue of Chinese
writer=csv.writer(fileobj)

#write to csv one row by one row#
column_name={'Column1': 'Test Case','Column2':'Test Name'}
def writetocsv(row):
    values=[]
    keys=row.keys()
    for key in keys:
        values.append(row[key])
    return values

#write the header info to csv#
values = writetocsv(column_name)
writer.writerow(values)
# #get the content of the csv by reading the caculated ptf test info#
for product in products:

    test=fileJson[product]
    writer.writerow([product])
    for item in test:
        print item
        values = writetocsv(item)
        writer.writerow(values)














