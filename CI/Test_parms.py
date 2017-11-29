#!/usr/bin/python
# -*- coding UTF-8 -*-

import getopt
import sys
import json

options, args = getopt.getopt(sys.argv[1:], "", ['UOW='])

if options != []:
    for name, value in options:
        if name == '--UOW':
            print 'UOW is ----', value
            data = {'UOW': value};
            with open('C:/Users/yizhazha/Desktop/python-sendmails-EX/data.json', 'w') as f:
                json.dump(data, f)

