# Executing the code
Store the csv files in the folder `<woking_directory>/Packet_traces/`. The code is written in python3 and uses libraries like `matplotlib` and `pylab`. To run the code use the following command: <br/>
```
python3 fileReader.py <Name_of_csv>
```
The code also takes an optional argument of Yes/No which allows the user to analyse a particular flow.<br/>
For example to run the csv file 1.csv use the following command<br/>
```
python3 fileReader.py 1
```
and to run it and also analyse packets use the command
```
python3 fileReader.py 1 Yes
```
