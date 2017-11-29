#!/usr/bin/python
# -*- coding UTF-8 -*-

import getopt
import sys
import json

with open('C:/Users/yizhazha/Desktop/python-sendmails-EX/data.json', 'r') as f:
    data = json.load(f)
    print(json.dumps(data))




