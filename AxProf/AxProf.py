import __main__
import math
import numpy as np
import os
import sys
from scipy.optimize import curve_fit
from scipy.stats import chisquare, binom_test, combine_pvalues, ttest_1samp, wilcoxon, norm
import itertools
import time
import queue
import scipy
import subprocess
from minepy import MINE
import random

from AxProfUtil import extractConfigsFromDict
from AxProfUtil import extractJobsFromConfigs
from AxProfUtil import extractAllConfigs
from AxProfUtil import writeDataToFile
from AxProfUtil import dumpObtainedData
from AxProfVis import visualizeOutput
from AxProfGenerators import * #nothing but generators

defaultInputFileName = '_AxProf_input.txt'
defaultOutputFileName = '_AxProf_output.txt'


def checkDist(observed, expected, DDoF, pvalue=0.05):
  obslist = []
  explist = []
  if type(observed) == dict and type(expected) == dict:
    keys = set(observed.keys()).union(set(expected.keys()))
    for key in keys:
      obslist.append(observed.get(key, 0))
      explist.append(expected.get(key, 0))
  else:
    obslist = observed
    explist = expected
  chisq, p = chisquare(obslist, explist, DDoF)
  return p >= pvalue


def checkFreq(observed, trials, expProb, pvalue=0.05, alternative='two-sided'):
  if observed > 0 and expProb == 0:
    return False
  p = binom_test(observed, trials, expProb, alternative)
  return p >= pvalue


def binomialTest(observed, trials, expProb, alternative='two-sided'):
  if observed > 0 and expProb == 0:
    return 0
  return binom_test(observed, trials, expProb, alternative)


def fitFuncToData(data, func, funcParams, paramNames):
  datalen = len(data)
  numParams = len(funcParams)
  arrays = (numParams + 1) * [None]
  for i in range(numParams + 1):
    arrays[i] = datalen * [0]

  arrayCtr = 0
  for config, val in data.items():
    for i in range(numParams):
      arrays[i][arrayCtr] = config[paramNames.index(funcParams[i])]
    arrays[numParams][arrayCtr] = val
    arrayCtr += 1

  for i in range(numParams + 1):
    arrays[i] = np.array(arrays[i])
  popt, pcov = curve_fit(func, arrays[:-1], arrays[-1])
  residuals = arrays[-1] - func(arrays[:-1], *popt)
  sum_sqd_residuals = np.sum(residuals**2)
  sum_sqd_total = np.sum((arrays[-1] - np.mean(arrays[-1]))**2)
  r_sqd = 1 - sum_sqd_residuals / sum_sqd_total
  return popt, r_sqd

def binomialSamplesReqd(alpha=0.05, beta=0.2, delta=0.1, tails=2, p0=0.5):
  adjAlpha = alpha/tails
  pa = p0-delta if p0 > 0.5 else p0+delta
  return math.ceil((((norm.ppf(1-adjAlpha)*math.sqrt(p0*(1-p0)))+(norm.ppf(1-beta)*math.sqrt(pa*(1-pa))))/delta)**2+(1/delta))

def generateFunctionsFromSpec(spec):
  # Writing spec to file to use with the java antlr backend
  tempSpecFile = open("/tmp/axprofspec", "w")
  tempSpecFile.write(spec)
  tempSpecFile.close()
  checkerGenPath = os.path.dirname(__file__)+'/checkerGen/'
  genCmd = ['java', '-ea', '-cp',
            checkerGenPath+'antlr-4.7.1-complete.jar:'+checkerGenPath,
            'MainClass', "/tmp/axprofspec"]
  pipes = subprocess.run(args=genCmd, stdout=subprocess.PIPE,
                         stderr=subprocess.PIPE)
  out, err = pipes.stdout, pipes.stderr
  out = out.decode("utf-8")
  err = err.decode("utf-8")
  if(err == ""):
    scriptFile = '.'.join(__main__.__file__.split('.')[:-1])
    out = "from __main__ import *\n\n" + out
    out = out.replace("%FILENAME%", scriptFile)
    newFunctions = {}
    exec(out, newFunctions)
    print("Generated following checker functions from spec:")
    print(out)
    return newFunctions
  else:
    print("Error while generating checker functions:")
    print(err)
    exit(1)


def checkProperties(configDict, runs, inputs, inputGen, inputGenParams, runner,
                    inpAgg=None, cfgAgg=None, perRunFunc=None, perInpFunc=None,
                    perConfigFunc=None, finalFunc=None, spec=None, skipAcc=False):

  if not os.path.isdir('outputs'):
    os.mkdir('outputs')
    print("Created 'outputs' directory for time and memory data")
  else:
    print("Using existing 'outputs' directory for time and memory data")

  if(spec is not None):
    newFunctions = generateFunctionsFromSpec(spec)
    # make the new functions local
    if (inpAgg is None):
      inpAgg = newFunctions['inpAgg'] if ('inpAgg' in newFunctions) else None
    if(cfgAgg is None):
      cfgAgg = newFunctions['cfgAgg'] if ('cfgAgg' in newFunctions) else None
    if(perRunFunc is None):
      perRunFunc = newFunctions['perRunFunc'] if ('perRunFunc' in newFunctions) else None
    if(perInpFunc is None):
      perInpFunc = newFunctions['perInpFunc'] if ('perInpFunc' in newFunctions) else None
    if(perConfigFunc is None):
      perConfigFunc = newFunctions['perConfigFunc'] if ('perConfigFunc' in newFunctions) else None
    if(finalFunc is None):
      finalFunc = newFunctions['finalFunc'] if ('finalFunc' in newFunctions) else None
    print(inpAgg, cfgAgg, perRunFunc, perInpFunc, perConfigFunc, finalFunc)
  else:
    print("No specification provided, using user-provided functions directly")

  samplesReqd = binomialSamplesReqd(alpha=0.05, beta=0.2, delta=0.1)

  if runs is None:
    if (perRunFunc is None) and (perInpFunc is not None):
      runs = samplesReqd
    elif (perRunFunc is not None) and (perInpFunc is None):
      runs = 320
    else:
      runs = 320  # cannot decide, be conservative
    print("Selected no. of required runs:", runs)
  else:
    print("Using user-provided no. of runs:", runs)

  if inputs is None:
    if perConfigFunc is None:
      inputs = 1
    else:
      inputs = samplesReqd
    print("Selected no. of required inputs:", inputs)
  else:
    print("Using user-provided no. of inputs:", inputs)

  # Build a list of configurations to be tested
  paramNames, configList = extractConfigsFromDict(configDict)
  outputList = dict.fromkeys(configList)

  # Run each configuration and run the checker functions
  allChecksPassed = True
  for config in configList:
    cfgAggregate = None
    thisConfigDict = {}
    for name in paramNames:
      thisConfigDict[name] = config[paramNames.index(name)]
    print("Running test program for configuration", thisConfigDict)

    for input_num in range(inputs):
      print("Input", input_num + 1)
      inpAggregate = None
      configIGParams = inputGenParams(thisConfigDict, input_num)
      inputData = inputGen(*configIGParams)
      writeDataToFile(inputData, defaultInputFileName)
      for run in range(runs):
        sys.stdout.write('.')
        sys.stdout.flush()
        output = runner(defaultInputFileName, thisConfigDict)
        if perRunFunc:
          if not skipAcc:
            allChecksPassed &= perRunFunc(thisConfigDict, inputData, output)
        if inpAgg:
          inpAggregate = inpAgg(inpAggregate, run, output)
      sys.stdout.write('\n')
      sys.stdout.flush()
      if perInpFunc:
        if not skipAcc:
          allChecksPassed &= perInpFunc(thisConfigDict, inputData, runs, inpAggregate)
      if cfgAgg:
          cfgAggregate = cfgAgg(cfgAggregate, input_num, inpAggregate)
      elif inputs == 1:
          cfgAggregate = inpAggregate
      else:
          # print("[Warning] Using the default configuration aggregator")
          if cfgAggregate is None:
              cfgAggregate = [inpAggregate]
          cfgAggregate.append(inpAggregate)
    if perConfigFunc:
      if not skipAcc:
        allChecksPassed &= perConfigFunc(thisConfigDict, runs, inputs, cfgAggregate)
    outputList[config] = cfgAggregate
  if allChecksPassed:
    print("All checks passed!")
  else:
    print("One or more checks failed.")
  if finalFunc:
    finalFunc(paramNames, outputList, runs, inputs)
  os.system("rm -f {} {} _axprof_temp_input".format(defaultInputFileName, defaultOutputFileName))


def selectInputFeatures(configs, inputGenerator, igparams,
                        tunedFeatures, error_function, runner, num_runs=5):

  permutation = [1, 2, 3, 4, 5]

  print('Starting the input feature selection process')
  # Build a set of configs that only change input parameters.
  # For the remaining parameters chose at random
  newConfigs = {}
  for key in configs.keys():
    if key in tunedFeatures:
      newConfigs[key] = configs[key]
    else:
      newConfigs[key] = [random.choice(configs[key])]
  configList = extractAllConfigs(newConfigs)

  tot_runs = num_runs * len(permutation) + num_runs * len(configList)
  print('Requires {} executions'.format(tot_runs))

  # Does permuting the data affect accuracy?
  result_set = []
  perm_feat = False
  tmp_config = random.choice(configList)._asdict()
  for perm in permutation:
    configIGParams = igparams(tmp_config, 0)
    inputData = inputGenerator(*configIGParams)
    new_inputs = inputData.copy()
    random.shuffle(new_inputs)
    writeDataToFile(new_inputs, "_axprof_temp_input")

    # Averaging over a set of runs.
    error_tot = 0
    for run in range(num_runs):
      results = runner("_axprof_temp_input", tmp_config)
      error_tot += error_function(new_inputs, results['acc'])
      sys.stdout.write('.')
      sys.stdout.flush()
    result_set.append(error_tot / num_runs)
  mine = MINE()
  mine.compute_score(permutation, result_set)
  perm_mic = mine.mic()
  if perm_mic > 0.9:
    perm_feat = True

  # Testing the other features
  result_set = {}
  for config in configList:
    # Setting the number to low value for now
    for input_num in range(5):
      inpAggregate = None
      configIGParams = igparams(config._asdict(), input_num)
      inputData = inputGenerator(*configIGParams)
      writeDataToFile(new_inputs, "_axprof_temp_input")
      error_tot = 0
      for run in range(num_runs):
        sys.stdout.write('.')
        sys.stdout.flush()
        results = runner("_axprof_temp_input", tmp_config)
        error_tot += error_function(new_inputs, results['acc'])
      result_set[config] = error_tot / num_runs

  sys.stdout.write('\n')
  sys.stdout.flush()

  mics = {}
  for key in tunedFeatures:
    agg_y = {}
    for config in result_set:
      config_dict = config._asdict()
      if config_dict[key] in agg_y:
        agg_y[config_dict[key]].append(result_set[config])
      else:
        agg_y[config_dict[key]] = [result_set[config]]

    unique_x = list(agg_y.keys())
    y = []
    for x in unique_x:
      y.append(np.mean(agg_y[x]))
    mine = MINE()
    mine.compute_score(unique_x, y)
    mics[key] = mine.mic()

  # Removing the variations in features that are not important
  for key in tunedFeatures:
    if mics[key] < 0.9:
      current = configs[key]
      configs[key] = [random.choice(current)]

  # Printing the report
  print('----------------------------------------')
  print('The results of input feature selection: ')
  print("Permuting the input: (MIC: {})".format(perm_mic))
  for key in tunedFeatures:
    print("{}: (MIC: {})".format(key, mics[key]))
  print("Updated config list: ", configs)
  print('----------------------------------------')
  return configs
