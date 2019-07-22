import math

import opentuner
from opentuner import ConfigurationManipulator
from opentuner import MeasurementInterface
from opentuner import Result
from opentuner.search.objective import *
from opentuner.measurement.inputmanager import FixedInputManager

import AxProf
import AxProfUtil

class OpentunerInterface(MeasurementInterface):

  def __init__(self, args, stableParams, adjParams, accParamNum, tuneRuns, verifyRuns, inputGen, inputGenParams, runner, spec, accMetric):
    self.stableParams = stableParams
    self.adjParams = adjParams
    self.tuneRuns = tuneRuns
    self.verifyRuns = verifyRuns
    self.inputGen = inputGen
    self.inputGenParams = inputGenParams
    self.runner = runner
    self.spec = spec
    self.accMetric = accMetric
    self.inputData = inputGen(*inputGenParams)
    AxProfUtil.writeDataToFile(self.inputData, AxProf.defaultInputFileName)
    objective = ThresholdAccuracyMinimizeTime(stableParams[accParamNum])
    input_manager = FixedInputManager()
    super(MyCMSTuner, self).__init__(args, objective=objective, input_manager=input_manager)

  def manipulator(self):
    manipulator = ConfigurationManipulator()
    for param in self.adjParams:
      manipulator.add_parameter(param)
    return manipulator

  def run(self, desired_result, input, limit):
    adjParamVals = desired_result.configuration.data
    allParamVals = adjParamVals.update(self.stableParams)
    minAcc = math.inf
    maxTime = 0
    for run in range(self.tuneRuns):
      sys.stdout.write('.')
      sys.stdout.flush()
      output = self.runner(AxProf.defaultInputFileName, allParamVals)
      acc = self.accMetric(self.inputData,output['acc'],allParamvals)
      time = output['time']
      if acc<minAcc:
        minAcc = acc
      if time>maxTime:
        maxTime = time
    return Result(time=maxTime,size=0,accuracy=minAcc)

  def save_final_config(self, configuration):
    #TODO verify result
    #adjParamVals = desired_result.configuration.data
    #allParamVals = adjParamVals.update(self.stableParams)
    #configDict = {a:[b] for a, b in allParamVals.items()}
    #AxProf.checkProperties(configDict, self.tuneRuns, 1, self.inputGen, self.inputGenParams, self.runner, spec=self.spec)
    bestresult = self.driver.results_query(config=configuration, objective_ordered=True)[0]
    print("Optimal configuration for",self.stableParams)
    adjParamVals = {}
    for param in self.adjParams:
      name = param.name
      adjParamVals[name] = configuration.data[name]
    print(adjParamVals)
    print("Optimal time:",bestresult.time)
    sys.stdout.flush()

