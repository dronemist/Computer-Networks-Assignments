import sys
import re

# NOTE: counted send and receive within the same TCP flow 

def fileReader(fileName):
  serverIPs = set()
  clientIPs = set()
  TCPflows = set()
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
          TCPflows.add(sourceIP + sourcePort + destinationIP + destinationPort)
  print(len(serverIPs)) 
  print(len(clientIPs))      
  print(len(TCPflows))

if __name__ == "__main__":
    fileReader("1.csv")