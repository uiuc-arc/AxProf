# AxProf Tutorial : checking if a coin is fair

# This tutorial shows how to test a simple program with AxProf while also
# discussing other features available in AxProf.

# This script tests a simple program that flips a specified number of coins.
# Each head is worth 1 and each tail is worth 0. It returns the sum as the
# output. We use AxProf to check that the program is using a fair coin. If so,
# the sum must be approximately equal to half of the number of coins.

# ==============================================================================

# Some Python modules required for this tutorial. You might need others for your
# test script.

import random
import sys
import time

# ==============================================================================

# The script must import AxProf. One way to achieve this is given below. A
# better way would be to add the AxProf directory to the PATH environment
# variable of your system.

sys.path.append('../AxProf')
import AxProf

# ==============================================================================

# We specify a list of configurations that will be tested. For this program the
# only configuration parameter is the number of coins. The list of
# configurations is provided as a dictionary with the name of the configuration
# parameter as the key and the list of values for that parameter as the value.
# AxProf will test all combinations of configuration parameters. For example, if
# there are two parameters A and B where A can be 1 or 2 and B can be 3 or 4,
# AxProf will test a total of four configurations:
# [{A:1,B:3},{A:1,B:4},{A:2,B:3},{A:2,B:4}]

configList = {'coins':[10,100,1000]}

# ==============================================================================

# The specification gives AxProf the information necessary to check that the
# program output is correct. The specification consists of a list of type
# declarations, followed by a TIME specification, SPACE specification, and
# ACCuracy specification. The specifications are optional, but they must be
# present in this order. Each type declaration or specification must be
# separated by a semicolon. The type of the Input and the Output must always
# be declared. The return type of any external functions used must also be
# declared.

spec='''
Input list of real;
Output real;
TIME coins;
ACC Expectation over runs [Output] == coins/2
'''

# The above specification states that the input is a list of real numbers. We
# will be generating an input for illustrative purposes, but we will not use it.
# The output is a real number (the sum of the coin face values). We specify that
# the time required should be proportional to the number of coins flipped.
# AxProf will generate code to check that this is true. Finally we specify that
# the expected value of the sum is half the number of coins. This expectation is
# over runs: an individual run might give a skewed value, but the average over
# multiple runs should be equal to coins/2. AxProf recognizes that `coins` is a
# variable from the configuration list. A SPACE specification is not given here.

# This specification is very simple compared to some of the specifications for
# the other benchmarks. See the benchmark script files for more examples of
# specifications, including probability specifications and specifications with
# universal quantification.

# ==============================================================================

# AxProf requires a function that it uses to generate parameters for the input
# generators. This function takes the current configuration being tested and the
# input number, and returns a list of parameters to be passed to the input
# generator. While we won't actually use the input for this program we will
# still invoke the distinctIntegerGenerator for illustrative purposes. This
# generator takes 3 parameters: the number of integers to generate, the minimum
# integer value, and the maximum integer value. The full list of generators is
# available in AxProf/AxProfGenerators.py in the artifact.

def inputParams(config, inputNum):
  return [config['coins'], 0, 1000000]

# This tells AxProf's distinctIntegerGenerator to generate distinct integers
# between 0 and 1000000. The number of integers generated is equal to the number
# of coins.

# ==============================================================================

# The runner is the interface to the implementation being tested. It accepts
# the input file name and the current configuration being tested as parameters.
# It then runs the implementation with the given input and configuration. In
# doing so it often invokes an external program using Python's subprocess
# module. Finally it returns a dictionary to AxProf containing three things: the
# output of the program that will be used to test the implementation accuracy,
# the amount of time consumed, and the amount of memory used. The task of time
# and memory usage measurement is left to the runner.

# In this case we do not need to call an external program. We flip the coins,
# add their face values, measure the time taken to do so, and return the output.

def runner(inputFileName, config):
  numCoins = config['coins']
  random.seed() # Seed RNG
  coinSum = 0
  startTime = time.time() # Start measuring time
  for i in range(numCoins):
    # Randomly pick 0 or 1 and add it to the sum
    # Adding more 0s or 1s will skew the results to simulate an unfair coin
    # AxProf should be able to detect this skew
    coinSum += random.choice([0,1])
  endTime = time.time() # Stop measuring time
  # Prepare result; we don't measure memory but must specify it, so set it to 0
  result = {'acc': coinSum, 'time': (endTime-startTime), 'space': 0}
  return result

# ==============================================================================

# Finally, we invoke AxProf's checkProperties function. It takes the following
# parameters:
# 1) The list of configurations to test.
# 2) The number of times the implementation should be run. Setting this to None
#    will allow AxProf to choose the number of runs automatically based on the
#    statistical test used.
# 3) The number of different inputs to test the implementation with.
# 4) The type of input generator to use.
# 5) The function used to give parameters to the input generator.
# 6) The runner i.e. the application interface.
# Apart from this, it takes multiple optional parameters. Usually, only the
# specification needs to be provided. AxProf will generate all necessary code
# from the specification and use it to test the program.
# It is important to use `if __name__ == '__main__':` when invoking AxProf. We
# also measure the total time taken to use AxProf.

if __name__ == '__main__':
  startTime = time.time() # Start measuring time
  AxProf.checkProperties(configList, None, 1, AxProf.distinctIntegerGenerator,
                         inputParams, runner, spec=spec)
  endTime = time.time() # Stop measuring time
  print('Total time required for checking:',endTime-startTime,'seconds.')

# ==============================================================================

# To run this script, run `python3 tutorial.py` from this directory. First,
# AxProf prints the code generated from the spec. This includes code to check
# accuracy, some utility code, code to write time data to a file, and fit a
# curve to the time data (in this case a linear function over the number of
# coins). Next, it prints the chosen number of runs (in this case 200). Finally
# it runs the implementation for each configuration. If any errors are detected,
# it prints 'Checker detected a possible error'. Otherwise it silently completes
# execution. Lastly, it prints the optimal curve fit parameters and the R^2
# metric of the fit. The time usage data is also written to the cutputs
# directory that is created in this directory.

# Try editing the line above that invokes random.choice by adding more 0s or 1s
# to skew the results. AxProf should be able to detect the skew (unless it is
# very mild)
