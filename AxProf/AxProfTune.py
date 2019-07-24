import math
import sys
import copy
import matplotlib.pyplot as plt

import opentuner
from opentuner import ConfigurationManipulator
from opentuner import MeasurementInterface
from opentuner import Result
from opentuner.search.objective import *
from opentuner.measurement.inputmanager import FixedInputManager

import AxProf
import AxProfUtil

lastThresholdBestresult = None

class AxProfTunerInterface(MeasurementInterface):

  def __init__(self, args, stableParams, adjParams, accThresh, tuneRuns, verifyRuns, inputGen, inputGenParams, runner, spec, accMetric):
    self.stableParams = stableParams
    self.adjParams = adjParams
    self.accThresh = accThresh
    self.tuneRuns = tuneRuns
    self.verifyRuns = verifyRuns
    self.inputGen = inputGen
    self.inputGenParams = inputGenParams
    self.runner = runner
    self.spec = spec
    self.accMetric = accMetric
    igParams = inputGenParams(stableParams, 1)
    self.inputData = inputGen(*igParams)
    AxProfUtil.writeDataToFile(self.inputData, AxProf.defaultInputFileName)
    objective = ThresholdAccuracyMinimizeTime(accThresh)
    input_manager = FixedInputManager()
    super(AxProfTunerInterface, self).__init__(args, objective=objective, input_manager=input_manager)

  def manipulator(self):
    manipulator = ConfigurationManipulator()
    for param in self.adjParams:
      manipulator.add_parameter(param)
    return manipulator

  #TODO seed config?
  #def seed_configurations(self):
  #  return [{'sketchEps':self.runArgs['checkEps']}]

  def run(self, desired_result, input, limit):
    allParamVals = copy.deepcopy(desired_result.configuration.data)
    allParamVals.update(self.stableParams)
    print(allParamVals)
    minAcc = math.inf
    maxTime = 0
    for run in range(self.tuneRuns):
      sys.stdout.write('.')
      sys.stdout.flush()
      output = self.runner(AxProf.defaultInputFileName, allParamVals)
      acc = self.accMetric(self.inputData,output['acc'],allParamVals)
      time = output['time']
      if acc<minAcc:
        minAcc = acc
      if time>maxTime:
        maxTime = time
    print(maxTime,minAcc)
    return Result(time=maxTime,size=1,accuracy=minAcc)

  def save_final_config(self, configuration):
    global lastThresholdBestresult
    bestresult = self.driver.results_query(config=configuration, objective_ordered=True)[0]
    adjParamVals = {}
    for param in self.adjParams:
      name = param.name
      adjParamVals[name] = configuration.data[name]
    allParamVals = copy.deepcopy(adjParamVals)
    allParamVals.update(self.stableParams)
    #verify result
    if self.spec is not None:
      print("Verifying result for optimal configuration")
      configDict = {a:[b] for a, b in allParamVals.items()}
      AxProf.checkProperties(configDict, self.verifyRuns, 1, self.inputGen, self.inputGenParams, self.runner, spec=self.spec)
      print("Verfication complete")
    else:
      print("Skipping verification")
    print("Optimal configuration for",self.stableParams,"and accuracy threshold",self.accThresh)
    print(adjParamVals)
    print("Optimal time:",bestresult.time)
    print("Optimal accuracy:",bestresult.accuracy)
    lastThresholdBestresult = (bestresult.accuracy,bestresult.time)
    sys.stdout.flush()

def AxProfTune(args, stableParams, adjParams, accThreshs, tuneRuns, verifyRuns, inputGen, inputGenParams, runner, spec, accMetric):
  global lastThresholdBestresult
  results = []
  for accThresh in accThreshs:
    print("Tuning for accuracy threshold",accThresh)
    AxProfTunerInterface.main(args, stableParams, adjParams, accThresh, tuneRuns, verifyRuns, inputGen, inputGenParams, runner, spec, accMetric)
    results.append(copy.deepcopy(lastThresholdBestresult))
  return results

def plotPareto(results):
  results = sorted(results)
  x = []
  y = []
  for pair in results:
    x.append(1.0-pair[0])
    y.append(pair[1])
  plt.plot(x,y,label='Pareto Curve')
  plt.xlabel('Error Tolerance')
  plt.ylabel('Time')
  plt.legend()
  plt.tight_layout()
  plt.show()
