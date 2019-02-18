# data generators for AxProf

import numpy as np
import random
import math
from AxProfUtil import writeDataToFile
import scipy


# Generates numbers of the form a*i+b for i in [0..length)
def linearGenerator(length, a, b):
  output = length * [0]
  for i in range(length):
    output[i] = a * i + b
  return output


# Generates uniformly chosen numbers within [_min,_max)
def uniformGenerator(length, _min, _max, seed=None):
  random.seed(seed)
  return [random.uniform(_min, _max) for _ in range(length)]


# Samples WITHOUT REPLACEMENT integers within [_min,_max]
def distinctIntegerGenerator(length, _min, _max, seed=None):
  random.seed(seed)
  return random.sample(range(_min,_max+1),length)


# Generates points in any number of dimensions
# dims specifies number of dimensions
# minCoord and maxCoord specify minimum and maximum values for each coordinate
def pointsGenerator(length, dims, minCoord, maxCoord, seed=None):
  random.seed(seed)
  output = length * [None]
  for i in range(length):
    output[i] = dims * [0]
    for j in range(dims):
      output[i][j] = random.randrange(minCoord, maxCoord)
  return output


# Generates integers in a zipf distribution with the given skew
# Keeps numbers below 2^31-1 to prevent overflow in C-style ints
def zipfGenerator(length, skew, seed=None):
  np.random.seed(seed)
  data_set = []
  generated = 0
  while(generated < length):
    draw = np.random.zipf(skew, 1)[0]
    if draw < 2147483647:
        data_set.append(draw)
        generated += 1
  return data_set


# Generates multiple matrices with the given specification list
# specs is a list of 3 tuples: (NUM,ROWS,COLS)
# each tuple generates NUM matrices of size ROWSxCOLS
def matrixGenerator(specs):
  output = []
  for spec in specs:
    howmany,rows,cols = spec
    for i in range(howmany):
      output.append(np.random.rand(rows,cols))
  return output


# Generates a single matrix of size l*m and flattens it into a list
def flattenedMatrixGenerator(l, m):
  A = np.random.rand(l, m)
  return list(np.matrix.flatten(A))


# Do not generate meaningful input
# used when script provides external input
def dummyGenerator(x):
  return [x]
