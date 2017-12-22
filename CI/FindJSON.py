# _*_ coding:UTF-8 _*_
'''
@author: zhniu/xiangll
'''
import os
import re
import time
import sys

# Return JSON file name, including extension
def get_JSONFile(uow, exo, email, filePath):
    timeStamp = 0
    prefix = uow + "_" + exo + "_" + email
    filelist = os.listdir(filePath)
    size = len(filelist)
    result=[]
    while size > 0:
        size = size - 1
        if (re.match(prefix,filelist[size])):
            result.append(filelist[size])
        else:
            continue
    print str(len(result)) + " JSON files found with the same perfix."

    if(len(result)==1):
        fileName = result[0]
    elif(len(result)==0):
        print "There is no JSON file found by current criteria."
    elif(len(result)>1):
        length = len(result)
        while length>0:
            length = length - 1
            content = result[length].split('_')
            file_json = content[3].split('.')
            if(timeStamp==0):
                timeStamp = long(file_json[0])
                #print type(timeStamp)
                fileName = result[length]
            elif (long(file_json[0]) - long(timeStamp)) > 0:
                timeStamp = file_json[0]
                fileName = result[length]
            #print fileName
    print "JSON file found: " + fileName
    return fileName

uow = sys.argv[1]
exo = sys.argv[2]
email = sys.argv[3]
filePath = sys.argv[4]

json_name = get_JSONFile(uow,exo,email,filePath)

#json_name = get_JSONFile("84077","E9200TS1","email@123.com","C:\D\SmartAutomation\CI\JSON_files")
#json_file = os.path.join(filePath, json_name)
