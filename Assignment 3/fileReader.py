import sys
import re
import matplotlib.pyplot as plt
import numpy as np

# Packet represented as array of dictionaries
parsedPackets = []

#Set of Server and client IPs, whether server or client recognised by seeing SYN packets
serverIPs = set()
clientIPs = set()

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
  plot_bar_x(label,TCPflowsForGraph)
  plotConnectionDurationCDF()
  # print(len(serverIPs)) 
  # print(len(clientIPs))      
  # print(len(TCPflows))


def plotConnectionDurationCDF():
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
        #Means packet sent from client to server
        TCPNumBytesSentOverConnection[flow] = TCPNumBytesSentOverConnection[flow] + packetLength

      if reverseFlowStartTime is not None:
        #Means packet sent from server to client
        TCPNumBytesReceivedOverConnection[reverseFlow] = TCPNumBytesReceivedOverConnection[reverseFlow] + packetLength

  # For plotting CDF of connection time and bytes sent      
  maxConnectionDuration = 0

  for (_, flowDuration) in TCPFlowConnectionDuration.items():
    maxConnectionDuration = max(maxConnectionDuration, flowDuration)


  # For plotting CDF of inter arrival opening times
  


  # 

  # TODO: Everything obtained, plotting remaining

if __name__ == "__main__":
    traceFolderName = "Packet_traces"
    fileReader(traceFolderName + "/1.csv")