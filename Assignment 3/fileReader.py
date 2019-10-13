import sys
import re
import matplotlib.pyplot as plt
import numpy as np
from pylab import *
import math

# Packet represented as array of dictionaries
parsedPackets = []

#Set of Server and client IPs, whether server or client recognised by seeing SYN packets
serverIPs = set()
clientIPs = set()


def calculatePearsonCoefficient(x, y):
  assert len(x) == len(y)
  n = len(x)
  assert n > 0
  avg_x = mean(x)
  avg_y = mean(y)
  diffprod = 0
  xdiff2 = 0
  ydiff2 = 0
  for idx in range(n):
      xdiff = x[idx] - avg_x
      ydiff = y[idx] - avg_y
      diffprod += xdiff * ydiff
      xdiff2 += xdiff * xdiff
      ydiff2 += ydiff * ydiff

  return diffprod / math.sqrt(xdiff2 * ydiff2)

def plot_bar_x(label, TCPflows, name):
  # this is for plotting purpose
  index = np.arange(len(label))
  TCPflowsInt = []
  for flow in TCPflows:
    TCPflowsInt.append(flow)
    # print(len(flow))
  plt.bar(index, TCPflowsInt)
  plt.xlabel('Time of Day', fontsize=5)
  plt.ylabel('No of Connections', fontsize=5)
  plt.xticks(index, label, fontsize=5, rotation=30)
  plt.savefig('Graphs/'+ name +'/No_of_connections.png') 
  plt.close()
  # plt.show()

# NOTE: counted send and receive within the same TCP flow 

def fileReader(fileName, name, toAnalyseFlow):
  TCPflows = set()
  TCPflowsPending = set()
  TCPflowsForGraph = [0 for _ in range(24)]
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
        flow = sourceIP + sourcePort + destinationIP + destinationPort
        reverseFlow = destinationIP + destinationPort + sourceIP + sourcePort
        # If it is a SYN packet
        if "SYN" in subWords[3] and "ACK" not in subWords[4]:
          serverIPs.add(destinationIP)
          clientIPs.add(sourceIP)         
          TCPflows.add(flow)
          TCPflowsPending.add(flow)
        
        if "ACK" in subWords[3]:
          if {flow}.issubset(TCPflowsPending):
            if (float(timeOfPacketCapture) - startTime) >= (60 * 60):
              startTime += 60 * 60
            TCPflowsForGraph[int((float)(startTime) / (float)(60 * 60))] += 1
            TCPflowsPending.remove(flow)
  plot_bar_x(label,TCPflowsForGraph, name)
  # print(TCPflowsForGraph)
  plotConnectionDurationCDF(name, toAnalyseFlow)
  print('Number of server Ip\'s: ' + str(len(serverIPs))) 
  print('Number of client Ip\'s: ' + str(len(clientIPs)))      
  print('Number of distinct TCP flows: ' + str(len(TCPflows)))

def plotScatterPlot(name, X, Y, xlabel, ylabel, xmax = 2000, ymax = 2000):
  plt.scatter(X, Y, color= "green",  
            marker= "o", s=10)
  xmin = 0
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
  plt.close()

  # function to show the plot 
  # plt.show()           


def plotCDF(name, X, xlabel, ylabel, xmin, xmax, isBounded = True):
  x = np.sort(X)
  f = figure()
  ax = f.add_subplot(111)
  mark = mean(x)
  # Getting y as the CDF
  y = np.arange(1, len(x) + 1) / len(x)
  text(0.7, 0.2,'mean = ' + str(mean(x)), ha='center', va='center', transform=ax.transAxes,
  bbox={'facecolor': 'red', 'alpha': 0.5, 'pad': 5})
  text(0.7, 0.1,'median = ' + str(median(x)), ha='center', va='center', transform=ax.transAxes,
  bbox={'facecolor': 'red', 'alpha': 0.5, 'pad': 5})
  if isBounded:
    plt.axis([xmin, xmax, 0, 1])
  plt.plot(x, y)
  plt.xlabel(xlabel)
  plt.ylabel(ylabel)
  plt.margins(0.02)
  # Saving the plot
  plt.savefig('Graphs/' + name + '/' + xlabel + '.png')  
  plt.close()
  # plt.show()


def plotConnectionDurationCDF(name, toAnalyseFlow):
  global parsedPackets
  global serverIPs
  global clientIPs

  # Dictionary of ((clientIP + clientPort + serverIP + serverPort) strings, start times) of flows which have been SYNned
  TCPFlowsStartedInTime = {}

  #NOTE: Bytes sent and received with respect to client

  # Dictionary of ((clientIP + clientPort + serverIP + serverPort) strings, bytes sent)
  TCPNumBytesSentOverConnection = {}

  # Dictionary of ((clientIP + clientPort + serverIP + serverPort) strings, bytes received)
  TCPNumBytesReceivedOverConnection = {}

  # Dictionary of (string, float) denoting the TCPFLow and its duration, here always client IP will come first in string
  TCPFlowConnectionDuration = {}

  ''' For part 6 '''
  # List of floats containing list of inter arrival times of SYN packets for new connections 
  interArrivalOpeningTimeList = []
  # Arrival Time of most recent SYN packet
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

  ''' For part 9 '''
  #Dictionary of (flow name, (sequence number, time)) for packets sent by server
  sequenceNumberSendingTime = {}
  #Dictionary of (flow name, (acknowledgment number, time)) for packets ACKed by client
  sequenceNumberACKedTime = {}


  for parsedPacket in parsedPackets:
    # print(parsedPacket)
    
    info = parsedPacket["Info"].strip()
    sourceIP = parsedPacket["Source"]
    destinationIP = parsedPacket["Destination"]
    timeOfCurrentPacketCapture = parsedPacket["Time"]
    packetLength = parsedPacket["Length"]
    currentArrivalTime = parsedPacket["Time"]

    # if packet is incoming to server
    if {destinationIP}.issubset(serverIPs) and parsedPacket["Protocol"] == "TCP":
      currentServerPacketInterArrivalTime = currentArrivalTime - mostRecentServerPacketArrivalTime
      interArrivalIncomingPacketToServerTimeList.append(currentServerPacketInterArrivalTime)
      mostRecentServerPacketArrivalTime = currentArrivalTime

      # Updating incoming packet length list
      incomingPacketLengthList.append(packetLength)
    # if packet is outgoing to client
    if {destinationIP}.issubset(clientIPs) and parsedPacket["Protocol"] == "TCP":
      #Updating outgoing packet length list
      outgoingPacketLengthList.append(packetLength)

    if parsedPacket["Protocol"] == "TCP":
      subWords = info.split()
      sourcePort = subWords[0]
      destinationPort = subWords[2]
      flow = sourceIP + " " + sourcePort + " " + destinationIP + " " + destinationPort
      reverseFlow = destinationIP + " " + destinationPort + " " + sourceIP + " " + sourcePort
      sequenceNumberAttribute = "Seq="
      ACKNumberAttribute = "Ack="

      if "SYN" in subWords[3] and "ACK" not in subWords[4]:
        # Source is client and destination is server

        #Reset the connection
        TCPNumBytesSentOverConnection[flow] = 0
        TCPNumBytesReceivedOverConnection[flow] = 0  

        #Updating the Connection start time
        TCPFlowsStartedInTime[flow] = currentArrivalTime

        #Updating the inter arrival times
        currentSYNInterArrivalTime = currentArrivalTime - mostRecentSYNPacketArrivalTime
        interArrivalOpeningTimeList.append(currentSYNInterArrivalTime)
        mostRecentSYNPacketArrivalTime = currentArrivalTime

        #Initialising sequence numbers
        sequenceNumberACKedTime[flow] = []
        sequenceNumberSendingTime[flow] = []
        

      if "FIN" in subWords[3] or "RST" in subWords[3]:
        flowStartTime = TCPFlowsStartedInTime.get(flow)
        reverseFlowStartTime = TCPFlowsStartedInTime.get(reverseFlow)

        #Connection ended by client
        if flowStartTime is not None:
          TCPFlowConnectionDuration[flow] = timeOfCurrentPacketCapture - flowStartTime 

        #Connection ended by server
        if reverseFlowStartTime is not None:
          TCPFlowConnectionDuration[reverseFlow] = timeOfCurrentPacketCapture - reverseFlowStartTime

      flowStartTime = TCPFlowsStartedInTime.get(flow)
      reverseFlowStartTime = TCPFlowsStartedInTime.get(reverseFlow)

      if flowStartTime is not None:
        # Means packet sent from client to server
        TCPNumBytesSentOverConnection[flow] = TCPNumBytesSentOverConnection[flow] + packetLength
        for attribute in subWords:
          ACKAttributePosition = attribute.find(ACKNumberAttribute)
          if ACKAttributePosition != -1:
            ACKNumberString = attribute[len(ACKNumberAttribute) :] 
            if sequenceNumberACKedTime.get(flow) is not None:
              sequenceNumberACKedTime[flow].append( (int(ACKNumberString), float(timeOfCurrentPacketCapture)) )
            else:
              sequenceNumberACKedTime[flow] = [ (int(ACKNumberString), float(timeOfCurrentPacketCapture)) ]


      if reverseFlowStartTime is not None:
        # Means packet sent from server to client
        TCPNumBytesReceivedOverConnection[reverseFlow] = TCPNumBytesReceivedOverConnection[reverseFlow] + packetLength
        for attribute in subWords:
          sequenceAttributePosition = attribute.find(sequenceNumberAttribute)
          if sequenceAttributePosition != -1:
            sequenceNumberString = attribute[len(sequenceNumberAttribute) :] 
            if sequenceNumberSendingTime.get(reverseFlow) is not None:
              sequenceNumberSendingTime[reverseFlow].append( (int(sequenceNumberString), float(timeOfCurrentPacketCapture)) )
            else:
              sequenceNumberSendingTime[reverseFlow] = [ (int(sequenceNumberString), float(timeOfCurrentPacketCapture)) ]

  # For plotting CDF of connection time and bytes sent      
  maxConnectionDuration = 0
  flowDurationPlotData = []
  bytesSentPlotData = []
  bytesReceivedPlotData = []
  print(median(incomingPacketLengthList))
  for (flow, flowDuration) in TCPFlowConnectionDuration.items():
    maxConnectionDuration = max(maxConnectionDuration, flowDuration)
    flowDurationPlotData.append(flowDuration)
    bytesSentPlotData.append(TCPNumBytesSentOverConnection[flow])
    bytesReceivedPlotData.append(TCPNumBytesReceivedOverConnection[flow])  
  '''plot for 4'''  
  plotCDF(name, flowDurationPlotData, 'Duration of connection(in s)', 'cdf', 0, 1000)

  print("Correlation Coefficients")
  print(calculatePearsonCoefficient(flowDurationPlotData, bytesSentPlotData))
  print(calculatePearsonCoefficient(bytesSentPlotData, bytesReceivedPlotData))

  '''plot for 5'''
  plotScatterPlot(name, flowDurationPlotData, bytesSentPlotData, "Duration of connection", "Bytes sent", 50, 1000)
  plotScatterPlot(name, bytesSentPlotData, bytesReceivedPlotData, "Bytes sent", "Bytes received", 1000, 1000)

  '''plot for 6'''
  plotCDF(name, interArrivalOpeningTimeList, 'Inter arrival connection time(in s)', 'cdf', 0, 500, False)
  print('Max connection inter arrival: ' + str(max(interArrivalOpeningTimeList)))
  print('Min connection inter arrival: ' + str(min(interArrivalOpeningTimeList)))

  '''plot for 7'''
  plotCDF(name, interArrivalIncomingPacketToServerTimeList, 'Inter arrival time of incoming packets(in s)', 'cdf', 0, 10, False)
  print('Max packet inter arrival: ' + str(max(interArrivalIncomingPacketToServerTimeList)))
  print('Min packet inter arrival: ' + str(min(interArrivalIncomingPacketToServerTimeList)))
  print('Mean: ' + str(mean(interArrivalIncomingPacketToServerTimeList)))
  
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

  # PART 9
  if toAnalyseFlow:
    print("Enter the flow you want to analyse in order: source IP destination IP source port destination port")

    flowToAnalyse = input()
    flowToAnalyseParameters = flowToAnalyse.split(" ")
    serverIP = flowToAnalyseParameters[0]
    clientIP = flowToAnalyseParameters[1]
    serverPort = flowToAnalyseParameters[2]
    clientPort = flowToAnalyseParameters[3]

    flowToAnalyseFormatted = clientIP + " " + clientPort + " " + serverIP + " " + serverPort

    sequenceTimeList = []
    sequenceNumberList = []
    ACKTimeList = []
    ACKList = []

    if sequenceNumberSendingTime.get(flowToAnalyseFormatted) is None:
      print("Wrong flow number")
      return

    for (sequenceNumber, time) in sequenceNumberSendingTime[flowToAnalyseFormatted]:
      sequenceNumberList.append(sequenceNumber)
      sequenceTimeList.append(time)

    for (ACKNumber, time) in sequenceNumberACKedTime[flowToAnalyseFormatted]:
      ACKList.append(ACKNumber)
      ACKTimeList.append(time)

    plt.scatter(sequenceTimeList, sequenceNumberList, color= "red",  
                marker= "o", s=10)
    plt.scatter(ACKTimeList, ACKList, color= "green",  
                marker= "o", s=10)
    xmin = min(sequenceTimeList + ACKTimeList)
    ymin = min(sequenceNumberList + ACKList) - 100
    # x-axis label 
    plt.xlabel("Time") 
    # frequency label 
    plt.ylabel("SequenceNumber")
    # zooming in on the graph
    plt.gca().set_xlim(left=xmin)
    plt.gca().set_ylim(bottom=ymin) 
    # plot title 
    # plt.title() 
    plt.margins(0.1)
    # plt.savefig('Graphs/' + name + '/' + ylabel + '.png')
    # plt.close()

    # function to show the plot 
    plt.show()           

  
def part11Plot(x, y, ylabel):
  plt.plot(x, y)
  plt.xlabel('lambda')
  plt.ylabel(ylabel)
  plt.margins(0.05)
  plt.savefig(ylabel + '.png')
  plt.title(ylabel + ' vs lambda')
  # plt.show()
  plt.close()

def doPart11():
  ''' For part 11'''
  # Rate is average rate of all three
  rate = (0.882 + 1.05 + 0.85) / 3 
  print("rate: " + str(rate))
  meanOutGoingPacketLength = (57.25 + 56.609 + 58.086) / 3
  # Assuming packet length is in bits
  mu = (128 * 1000) / (meanOutGoingPacketLength * 8)
  print("mu: " + str(mu))
  utilisationFactor = rate / mu
  queueSize = (rate) / (mu - rate)
  averageWaitingTime = (1/(mu - rate)) - (1 / mu)
  print("Utilization Factor: " + str(utilisationFactor))
  print("Average queue size: " + str(queueSize))
  print("Average waiting time: " + str(averageWaitingTime))
  x = np.arange(0, mu)
  queueSizePlot = (x) / (mu - x)
  part11Plot(x, queueSizePlot, 'Queue size')
  waitingTimePlot = (1/(mu - x)) - (1 / mu)
  part11Plot(x, waitingTimePlot, 'Waiting time')


if __name__ == "__main__":
  traceFolderName = "Packet_traces"
  toAnalyseFlow = False

  name = sys.argv[1]

  # Whether user wants to see 9 part also
  try:
    if sys.argv[2] == "Yes":
      toAnalyseFlow = True
  except:
    pass

  fileReader(traceFolderName + "/" + name + ".csv", name, toAnalyseFlow)
  doPart11()
