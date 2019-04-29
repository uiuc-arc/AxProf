# AxProf example for testing the HyperLogLog implementation in ekzhu/datasketch

# NOTICE:
# This example requries the datasketch library
# Please follow instructions for downloading the datasketch library in the
# 'Example' section of README.md in the root directory of this repository.

import sys
import time
import pickle
import subprocess
from numpy import sqrt

sys.path.append('../AxProf')
import AxProf

sys.path.append('./datasketch')

try:
  from datasketch import hyperloglog
except ModuleNotFoundError:
  print(
"""
Error: datasketch library not found!
Please follow instructions for downloading the datasketch library in the
'Example' section of README.md in the root directory of this repository.
""")
  exit(-1)
  

configlist = {'k': [8, 10, 12, 14],
              'datasize': range(10000, 110000, 10000)}


def input_params(config, inputNum):
    return config['datasize'], 0, 1000000

spec = '''
Input list of real;
Output real;
abs real;
sqrt real;
TIME k*datasize;
SPACE 2^k;
ACC Probability over inputs[ abs(datasize-Output) < (datasize*1.04)/sqrt(2^k) ] > 0.65
'''

def runner(ifname, config):
    h = hyperloglog.HyperLogLog(p=int(config['k']))
    data = []
    for line in open(ifname, "r"):
        data.append(line[:-1])

    i_start = time.time()
    for d in data:
        h.update(d.encode('utf8'))
    i_end = time.time()
    time_diff = i_end - i_start

    output = {}
    output['time'] = time_diff

    outfile = "_AXPROF_MEMDUMP"
    filehandler = open(outfile, "wb")
    pickle.dump(h, filehandler)

    query_str = "ls -l {} | cut -d' ' -f5".format(outfile)
    result_test = subprocess.check_output(query_str, shell=True)
    memory_used = int(result_test)

    output['space'] = memory_used
    output['acc'] = h.count()

    return output


if __name__ == "__main__":
  subprocess.run(['date'])
  AxProf.checkProperties(configlist, 1, None,
                         AxProf.distinctIntegerGenerator,
                         input_params, runner, spec=spec)
  subprocess.run(['date'])
  subprocess.run(args=['rm', '-f', '_AXPROF_MEMDUMP'])
