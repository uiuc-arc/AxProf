import matplotlib
import matplotlib.pyplot as plt

def visualizeOutput(data,fileName,paramNames,xVar,dataName=None):
  numParams = len(paramNames)
  plt.rcParams['axes.labelsize'] = 12
  plt.rcParams['axes.labelweight'] = 'bold'
  plt.rcParams["font.size"] = 12
  plt.rcParams["font.weight"] = "bold"
  plt.rcParams["axes.labelweight"] = "bold"
  matplotlib.rcParams.update({'legend.fontsize': 12})
  matplotlib.rcParams.update({'font.weight': 'bold'})
  xVarIdx = paramNames.index(xVar)
  xVarValues = sorted({cfg[xVarIdx] for cfg in data})
  numXVarValues = len(xVarValues)
  arrays = {}
  for config, val in data.items():
    cfg = list(config)
    cfg[xVarIdx] = 0
    cfg = tuple(cfg)
    if not cfg in arrays:
      arrays[cfg] = numXVarValues*[0]
    idx = xVarValues.index(config[xVarIdx])
    arrays[cfg][idx] = val
  for config, dataValues in arrays.items():
    labelStr = ''
    for i in range(numParams):
      if i == xVarIdx:
        continue
      labelStr += paramNames[i] + '=' + str(config[i]) + ' '
    plt.plot(xVarValues, dataValues, label=labelStr)
  if dataName==None:
    plt.ylabel('Output')
  else:
    plt.ylabel(dataName)
  plt.xlabel(xVar)
  plt.legend(loc=0)
  plt.savefig(fileName, dpi=300)
  plt.close('all')
