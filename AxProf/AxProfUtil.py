#utility functions for AxProf

import numpy as np
import itertools
from collections import namedtuple

# Write a list of real numbers, lists, or matrices to a file
# Used to write the inputs to a file
def writeDataToFile(data, fileName):
  dummyMatrix = np.array([[0]])
  dataFile = open(fileName, 'w+')
  for datum in data:
    if type(datum) == list:
      print(*datum, file=dataFile)
    elif type(datum) == type(dummyMatrix):
      assert(len(np.shape(datum))==2)
      print(*np.shape(datum), file=dataFile)
      for row in datum:
        print(*row, file=dataFile)
    else:
      print(datum, file=dataFile)
  dataFile.close()


# Get all configs that need testing
# Also return the list of configuration parameter names
def extractConfigsFromDict(configDict):
  paramNames = []
  paramValues = []
  for name, values in configDict.items():
    paramNames.append(name)
    paramValues.append(values)
  configList = list(itertools.product(*paramValues))
  return paramNames, configList


# Get all configs that need testing
# Independent of inputs
def extractAllConfigs(configDict):
  Config = namedtuple('Config', list(configDict.keys()) + ['id'])
  product = itertools.product(*configDict.values())
  return [Config(*p, i) for i, p in enumerate(product)]


# For a given list of algorithmic parameters and
# a number of inputs to be tested for each config
# return the list of jobs that need to be run
def extractJobsFromConfigs(configList, inputs, runs):
  Job = namedtuple('Job', ['config', 'input_id', 'run_id'])
  jobs = itertools.product(configList, range(inputs), range(runs))
  return [Job(*i) for i in jobs]


# Write time or space data for each configuration to a file
def dumpObtainedData(data,fileName,paramNames,dataName=''):
  numParams = len(paramNames)
  outputFile = open(fileName,'w')
  for i in range(numParams):
    outputFile.write(paramNames[i]+'\t')
  outputFile.write(dataName+'\n')
  for config, val in data.items():
    for i in range(numParams):
      outputFile.write(str(config[i])+'\t')
    outputFile.write(str(val)+'\n')
  outputFile.close()
