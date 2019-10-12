import sys
import re
import matplotlib.pyplot as plt
import numpy as np
from pylab import *

# Packet represented as array of dictionaries
parsedPackets = []

#Set of Server and client IPs, whether server or client recognised by seeing SYN packets
serverIPs = set()
clientIPs = set()

def plot_bar_x(label, TCPflows, name):
  # this is for plotting purpose
  index = np.arange(len(label))
  TCPflowsInt = []
  for flow in TCPflows:
    TCPflowsInt.append(len(flow))
  plt.bar(index, TCPflowsInt)
  plt.xlabel('Time of Day', fontsize=5)
  plt.ylabel('No of Connections', fontsize=5)
  plt.xticks(index, label, fontsize=5, rotation=30)
  plt.savefig('Graphs/'+ name +'/No_of_connections.png') 
  # plt.show()

# NOTE: counted send and receive within the same TCP flow 

def fileReader(fileName, name):
  TCPflows = set()
  TCPflowsForGraph = [set() for _ in range(24)]
  label = [i for i in range(24)]
  # startTime denotes the time at which hour starts
  startTime = 0.0

  global parsedPackets
  global serverIPs
  global clientIPs

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
      parsedPacket = {}
      parsedPacket["No."] = int(packetNumber)
      parsedPacket["Time"] = float(timeOfPacketCapture)
      parsedPacket["Source"] = sourceIP
      parsedPacket["Destination"] = destinationIP
      parsedPacket["Protocol"] = protocol
      parsedPacket["Length"] = int(packetLength)
      parsedPacket["Info"] = info
      parsedPackets.append(parsedPacket)
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
  plot_bar_x(label,TCPflowsForGraph, name)
  plotConnectionDurationCDF(name)
  # print(len(serverIPs)) 
  # print(len(clientIPs))      
  # print(len(TCPflows))

def plotScatterPlot(name, X, Y, xlabel, ylabel):
  plt.scatter(X, Y, color= "green",  
            marker= "o", s=30)
  xmax = 2000
  xmin = 0
  ymax = 2000
  ymin = 0          
  # x-axis label 
  plt.xlabel(xlabel) 
  # frequency label 
  plt.ylabel(ylabel)
  # zooming in on the graph
  plt.axis([xmin, xmax, ymin, ymax]) 
  # plot title 
  # plt.title() 
  plt.margins(0.1)
  plt.savefig('Graphs/' + name + '/' + ylabel + '.png')

  # function to show the plot 
  # plt.show()           


def plotCDF(name, X, xlabel, ylabel, xmin, xmax):
  x = np.sort(X)
  f = figure()
  ax = f.add_subplot(111)
  mark = mean(x)
  print(mark)
  # Getting y as the CDF
  y = np.arange(1, len(x) + 1) / len(x)
  # text(0.7, 0.9,'mean = ' + str(mean(x)), ha='center', va='center', transform=ax.transAxes,
  # bbox={'facecolor': 'red', 'alpha': 0.5, 'pad': 5})
  # text(0.7, 0.8,'median = ' + str(median(x)), ha='center', va='center', transform=ax.transAxes,
  # bbox={'facecolor': 'red', 'alpha': 0.5, 'pad': 5})
  plt.axis([xmin, xmax, 0, 1])
  plt.plot(x, y, markevery = mark, marker = '*', markerfacecolor = 'green')
  plt.xlabel(xlabel)
  plt.ylabel(ylabel)
  plt.margins(0.02)
  # Saving the plot
  plt.savefig('Graphs/' + name + '/' + xlabel + '.png')  
  # plt.show()


def plotConnectionDurationCDF(name):
  global parsedPackets
  global serverIPs
  global clientIPs

  # Dictionary of ((sourceIP + sourcePort + destinationIP + destinationPort) strings, start times) of flows which have been SYNned
  TCPFlowsStartedInTime = {}

  #NOTE: Bytes sent and received with respect to client

  # Dictionary of ((sourceIP + sourcePort + destinationIP + destinationPort) strings, bytes sent)
  TCPNumBytesSentOverConnection = {}

  # Dictionary of ((sourceIP + sourcePort + destinationIP + destinationPort) strings, bytes received)
  TCPNumBytesReceivedOverConnection = {}

  # Dictionary of (string, float) denoting the TCPFLow and its duration, here always client IP will come first in string
  TCPFlowConnectionDuration = {}

  ''' For part 6 '''
  # List of floats containing list of inter arrival times of SYN packets for new connections 
  interArrivalOpeningTimeList = []
  #Arrival Time of most recent SYN packet
  mostRecentSYNPacketArrivalTime = 0

  ''' For part 7 '''
  #Inter arrival time for incoming packets to server
  interArrivalIncomingPacketToServerTimeList = []
  #Arrival time of most recent packet to server
  mostRecentServerPacketArrivalTime = 0

  ''' For part 8 '''
  # List of incoming packet lengths to server
  incomingPacketLengthList = []
  # List of outgoing packet lengths to client
  outgoingPacketLengthList = []

  for parsedPacket in parsedPackets:
    # print(parsedPacket)
    
    info = parsedPacket["Info"].strip()
    sourceIP = parsedPacket["Source"]
    destinationIP = parsedPacket["Destination"]
    timeOfCurrentPacketCapture = parsedPacket["Time"]
    packetLength = parsedPacket["Length"]
    currentArrivalTime = parsedPacket["Time"]

    # if packet is incoming to server
    if {destinationIP}.issubset(serverIPs):
      currentServerPacketInterArrivalTime = currentArrivalTime - mostRecentServerPacketArrivalTime
      interArrivalIncomingPacketToServerTimeList.append(currentServerPacketInterArrivalTime)
      mostRecentServerPacketArrivalTime = currentArrivalTime

      #Updating incoming packet length list
      incomingPacketLengthList.append(packetLength)

    # if packet is outgoing to client
    if {destinationIP}.issubset(clientIPs):
      #Updating outgoing packet length list
      outgoingPacketLengthList.append(packetLength)

    if parsedPacket["Protocol"] == "TCP":
      subWords = info.split()
      sourcePort = subWords[0]
      destinationPort = subWords[2]
      flow = sourceIP + " " + sourcePort + " " + destinationIP + " " + destinationPort
      reverseFlow = destinationIP + " " + destinationPort + " " + sourceIP + " " + sourcePort

      if "SYN" in subWords[3] and "ACK" not in subWords[4]:
        
        #Reset the connection
        TCPNumBytesSentOverConnection[flow] = 0
        TCPNumBytesReceivedOverConnection[flow] = 0  

        #Updating the Connection start time
        TCPFlowsStartedInTime[flow] = currentArrivalTime

        #Updating the inter arrival times
        currentSYNInterArrivalTime = currentArrivalTime - mostRecentSYNPacketArrivalTime
        interArrivalOpeningTimeList.append(currentSYNInterArrivalTime)
        mostRecentSYNPacketArrivalTime = currentArrivalTime

      if "FIN" in subWords[3] or "RST" in subWords[3]:
        flowStartTime = TCPFlowsStartedInTime.get(flow)
        reverseFlowStartTime = TCPFlowsStartedInTime.get(reverseFlow)

        #Connection ended by client
        if flowStartTime is not None:
          TCPFlowConnectionDuration[flow] = timeOfCurrentPacketCapture - flowStartTime 

        #Connection ended by host
        if reverseFlowStartTime is not None:
          TCPFlowConnectionDuration[reverseFlow] = timeOfCurrentPacketCapture - reverseFlowStartTime

      flowStartTime = TCPFlowsStartedInTime.get(flow)
      reverseFlowStartTime = TCPFlowsStartedInTime.get(reverseFlow)

      if flowStartTime is not None:
        # Means packet sent from client to server
        TCPNumBytesSentOverConnection[flow] = TCPNumBytesSentOverConnection[flow] + packetLength

      if reverseFlowStartTime is not None:
        # Means packet sent from server to client
        TCPNumBytesReceivedOverConnection[reverseFlow] = TCPNumBytesReceivedOverConnection[reverseFlow] + packetLength

  # For plotting CDF of connection time and bytes sent      
  maxConnectionDuration = 0
  flowDurationPlotData = []
  bytesSentPlotData = []
  bytesReceivedPlotData = []

  for (flow, flowDuration) in TCPFlowConnectionDuration.items():
    maxConnectionDuration = max(maxConnectionDuration, flowDuration)
    flowDurationPlotData.append(flowDuration)
    bytesSentPlotData.append(TCPNumBytesSentOverConnection[flow])
    bytesReceivedPlotData.append(TCPNumBytesReceivedOverConnection[flow])
  '''plot for 4'''  
  plotCDF(name, flowDurationPlotData, 'Duration of connection(in s)', 'cdf', 0, 1000)

  '''plot for 5'''
  plotScatterPlot(name, flowDurationPlotData, bytesSentPlotData, "Duration of connection", "Bytes sent",)
  plotScatterPlot(name, bytesSentPlotData, bytesReceivedPlotData, "Bytes sent", "Bytes received")

  '''plot for 6'''
  plotCDF(name, interArrivalOpeningTimeList, 'Inter arrival connection time(in s)', 'cdf', 0, 500)
  
  '''plot for 7'''
  plotCDF(name, interArrivalIncomingPacketToServerTimeList, 'Inter arrival time of incoming packets(in s)', 'cdf', 0, 10)
  
  '''plot for 8'''
  plotCDF(name, incomingPacketLengthList, 'Incoming packet length', 'cdf', 0, 120)
  plotCDF(name, outgoingPacketLengthList, 'Outgoing packet length', 'cdf', 0, 200)

  '''for part 10'''
  # Connection inter arrival time
  file = open("R_csv's/" + name + "/interArrivalConnection.csv", "w") 
  file.write("X\n")
  for time in interArrivalOpeningTimeList:
    file.write(str(time) + '\n')
  file.close()
  # Packet inter arrival time
  file = open("R_csv's/" + name + "/interArrivalPacket.csv", "w")
  file.write("X\n")
  for time in interArrivalIncomingPacketToServerTimeList:
    file.write(str(time) + '\n')
  file.close()

  # TODO: Everything obtained, plotting remaining

if __name__ == "__main__":
    traceFolderName = "Packet_traces"
    name = sys.argv[1]
    fileReader(traceFolderName + "/" + name + ".csv", name)