import sys
import re
import matplotlib.pyplot as plt
import numpy as np

def plot_bar_x(label, TCPflows):
  # this is for plotting purpose
  index = np.arange(len(label))
  TCPflowsInt = []
  for flow in TCPflows:
    TCPflowsInt.append(len(flow))
  plt.bar(index, TCPflowsInt)
  plt.xlabel('Time of Day', fontsize=5)
  plt.ylabel('No of Connections', fontsize=5)
  plt.xticks(index, label, fontsize=5, rotation=30)
  # plt.title('Market Share for Each Genre 1995-2017')
  plt.show()

# NOTE: counted send and receive within the same TCP flow 

def fileReader(fileName):
  serverIPs = set()
  clientIPs = set()
  TCPflows = set()
  TCPflowsForGraph = [set() for _ in range(24)]
  label = [i for i in range(24)]
  # startTime denotes the time at which hour starts
  startTime = 0.0
  ''' Parses the input csv file '''
  with open(fileName) as fileIn:
    fileIn.readline()
    for line in fileIn:
      # getting the required information
      words = line.split(',')
      packetNumber = words[0].strip('\"')
      timeOfPacketCapture = words[1].strip('\"')
      sourceIP = words[2].strip('\"')
      destinationIP = words[3].strip('\"')
      protocol = words[4].strip('\"')
      packetLength = words[5].strip('\"')
      info = words[6].strip('\"')
      if(len(words) > 7):
        info += words[7].strip('\"')
      # If protocol is TCP
      if protocol == "TCP":
        info = info.strip()
        subWords = info.split()
        sourcePort = subWords[0]
        destinationPort = subWords[2]
        # If it is a SYN packet
        if "SYN" in subWords[3] and "ACK" not in subWords[4]:
          serverIPs.add(destinationIP)
          clientIPs.add(sourceIP)
          # counting a TCP flow only from the SYN packet
          flow = sourceIP + sourcePort + destinationIP + destinationPort
          TCPflows.add(flow)
          if (float(timeOfPacketCapture) - startTime) >= (60 * 60):
            startTime += 60 * 60
          TCPflowsForGraph[int(startTime / (60 * 60))] = flow
  plot_bar_x(label,TCPflowsForGraph)
  print(len(serverIPs)) 
  print(len(clientIPs))      
  print(len(TCPflows))

if __name__ == "__main__":
    fileReader("1.csv")