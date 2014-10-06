# Project Name: Sound Recognition
# Date: 1/29/2014
# Version: 2.1
# Summary: this code is ment to record
# a sample of data and predict the type
# of sound being recorded.'

import sys
from numpy import *
import scipy 
from sklearn import svm
from sklearn import preprocessing
import matplotlib.pyplot as plt
import matplotlib.mlab as mlab
import wave
import pyaudio 
import struct
import math
label = [0] * 1000 #increase as Saved samples increases
Freq = 44100.
NumSec = 4
NumOfSamples = 70
UpperLevel = 0
LowerLevel = 0


##################     Classes     ##################
#Description:
#SoundRecognitionAudio--
#SoundRecognitionSVM_Learning--
 
#####################################################
class SoundRecognitionSVM_Training(object):
  def SetupTraining(self):
    self.target = []
    self.countone = 0
    self.labelCount = 0
    
  def Add_Sample_Target(self, FFTData, Label):
    self.countone += 1 
    global stackarray
    global target
    if(self.countone == 1):
      stackarray = array([FFTData])
      self.target = self.AddLabel(Label)
      self.target = [self.target]
      target = array(self.target)
    if(self.countone > 1):
      self.stackarrays = array([FFTData]) 
      stackarray = vstack((stackarray,self.stackarrays))
      self.target = self.AddLabel(Label)
      self.target = [self.target]
      target = append(target,self.target)
	    
  def FitClassification(self):
    global clf
    clf = svm.LinearSVC()    
    clf.fit(stackarray,target)
    
    
  def Predict(self,SampleToPredict):
    SampleToPredict = [SampleToPredict]
    results = clf.predict(SampleToPredict)
    results = label[results]
    return results
   
  def AddLabel(self, Label):
    global label
    match = 'no match'
    if(self.labelCount == 0):
      label[self.labelCount] = Label
      self.labelCount += 1
      return 0
    if(self.labelCount >= 1):
      for i in range(self.labelCount):
	if(label[i]  == Label):
	  match = 'there is a match'
	  return i
      if(match  == 'no match'):
	  label[self.labelCount] = Label
	  self.labelCount += 1
	  return (self.labelCount-1)

###################################################################
class SoundRecognitionAudio(object):
  
  def SetupAudio(self):
    self.waveCount = 0
  
  def soundFilter(self, soundData):
    #print 'Iam in...', soundData
    global UpperLevel
    global LowerLevel
    UpperLevel = 0
    LowerLevel = 0
    #sec = 0.00226757369515
    sec = 0.4
    NumSamples = int(math.ceil(sec*Freq))
    for i in range(NumSamples):
      if(int(soundData[i]) > UpperLevel):
        UpperLevel = int(soundData[i])
      if(int(soundData[i]) < LowerLevel):
        LowerLevel = int(soundData[i])
      #print 'the Sound data is', soundData[i]
    #print 'LowerLevel is', LowerLevel
    #print 'UpperLevel is', UpperLevel
    filteredArray = [0] * len(soundData)
    counter = 0
    for i in range(len(filteredArray)):#8400 gives me 52 predictions correct
      if((UpperLevel+200) < int(soundData[i])):#change  the values to set how much to reduce/increase
        filteredArray[i] = soundData[i]
      elif((LowerLevel-200) > int(soundData[i])):#change  the values to set how much to reduce/increase
        filteredArray[i] = soundData[i]
      else:
        filteredArray[i] = '0\n'        
    #self.graph_sound(soundData,NumSec)
    #self.graph_sound(filteredArray,NumSec)
    return filteredArray
       
  
  
  def FFT(self, soundData):
    floatArray = [float(x.strip('\n')) for x in soundData]#\n
    FFT = abs(fft.fft(floatArray))
    Maximum = max(FFT)
    #print 'the max is',Maximum
    for i in range(len(FFT)):
      if(Maximum != 0.0):
        FFT[i] = (FFT[i]/Maximum)#*22050 #arbitrary number
      else:
        FFT[i] = (FFT[i]/1.0)      
    FFT = array(FFT)
    return FFT
     
  def WavToArray(self, WavFile, filecount):
    sampleWidth = WavFile.getsampwidth()
    fmts = (None, "=B", "=h", None, "=l")
    fmt = fmts[sampleWidth]
    dcs = (None, 128, 0, None, 0)
    dc = dcs[sampleWidth]
    self.soundData = [0] * WavFile.getnframes()
    filename = '/Users/taz/Desktop/PythonCode/chrisRecordings/soundtxt/'+filecount+'.txt'
    self.soundfile = open(filename, 'w')
    for i in range(WavFile.getnframes()):
      iframe = WavFile.readframes(1)
      iframe = struct.unpack(fmt,iframe)[0]
      iframe-= dc
      data = str(iframe)
      self.soundData[i] = data
    return self.soundData
  
  
  def EditSample(self, sounddata, SecToEdit):
    lenghtofdata = len(sounddata)
    sampleRate = int(math.ceil(Freq*SecToEdit))
    newarray = [0]*sampleRate
    if(lenghtofdata <= sampleRate):
      for i in range(len(newarray)):
        if(i <= (lenghtofdata)):
          newarray[i] = sounddata[i-1]
          self.soundfile.write(newarray[i] + "\n")
        if(i > (lenghtofdata)):
          newarray[i] = '0'
          self.soundfile.write(newarray[i] + "\n")
    if(lenghtofdata > sampleRate):
      for i in range(len(newarray)):
        newarray[i] = sounddata[i]
        self.soundfile.write(newarray[i] + "\n")
    data = [newarray,sampleRate]        
    self.soundfile.close
    return data
    
  def openFile(self,filecount):
    filecounter = filecount+1
    text = str(filecounter)
    text = '/Users/taz/Desktop/PythonCode/chrisRecordings/'+ text +'.wav'
    WaveFile = wave.open(text,'r')
    data = self.WavToArray(WaveFile,str(filecounter))
    return data

  def openText(self, filecount):
    filecounter = filecount+1
    text = str(filecounter)
    fileName = '/Users/taz/Desktop/PythonCode/chrisRecordings/soundtxt/' + text +'.txt'
    count = len(open(fileName,'r').readlines())
    DataArray = [0] * count
    Var1 = open(fileName,'r')
    for i in range(count):
      DataArray[i] = Var1.readline()
    return DataArray
  
  def graph_FFT(self,DataToGraph):
    mu = 100
    sigma = 15
    num_bins = 50
    n, bins, patches = plt.hist(DataToGraph, num_bins, normed = False, facecolor = 'green', alpha =0.5)
    y = mlab.normpdf(bins, mu, sigma)
    plt.plot(bins, y)
    plt.xlabel('Frequency bins')
    plt.ylabel('Magnitude')
    plt.title('Historam of frequency')
    plt.subplots_adjust(left = 0.15)
    plt.show()
    
  def graph_sound(self, DataToGraph, samplingTime):
    seconds = samplingTime
    rate = seconds*Freq
    Xspace = linspace(0,seconds,rate)
    plt.plot(Xspace,DataToGraph)
    plt.xlabel('time (s)')
    plt.ylabel('Magnitude')
    plt.title('Sound Signal')
    plt.show()
  ###Still working  on it  
  def playSound(self, numfile):
    numf= str(numfile)
    text = '/Users/taz/Desktop/PythonCode/chrisRecordings/'+ numf +'.wav'
    WaveFile = wave.open(text,'r')
    #p = pyaudio.
    
##############        Main        ###############
#Description:

def main():
  #create an instance of each class
  Train = SoundRecognitionSVM_Training()
  Audio = SoundRecognitionAudio()
  #initialize the variables used in training
  Train.SetupTraining()
  Audio.SetupAudio()
  
  lockCount = 0  
  lockkeyCount = 0  
  lockporchCount = 0
  nolockCount = 0
  runningwaterCount = 0
  unlockCount = 0
  unlockeyCount = 0
  unlockporchCount = 0
 
  # this is false if all wav files have been loaded
  # into text format or if the sample time has changed
  firstTime = False
  #make an array that will suport the number of wav files
  Samples = [0] * NumOfSamples
  SamplingRate = [0] * NumOfSamples
  # how many secods do you want to sample
  Sampling_time = NumSec
  label = ''

  for i in range(len(Samples)):
    
    if(firstTime):
      Samples[i] = Audio.openFile(i)
      Samples[i], SamplingRate[i] = Audio.EditSample(Samples[i],Sampling_time)
    else:
      # this opens the text data instead of wav witch takes longer
      Samples[i] = Audio.openText(i)
      if(i < 21):#or (i >=59 and i < 59)
        Samples[i] = Audio.soundFilter(Samples[i])
      
    if(i == (16-1) or i == (3-1) or i == (2-1)): # start:1 End:21 best outcome:2@3, 5@3 potentials::12, 13, 14, 15, 16, 17, 18, 19, 20, 21   
      label = 'Lock'
      _FFTdata = Audio.FFT(Samples[i])
      Train.Add_Sample_Target(_FFTdata, label)
      label = ''
    if(i == (23-1) or i == (25-1)): #start:22 End:26 max:
      label = 'Lock with Key'
      _FFTdata = Audio.FFT(Samples[i])
      Train.Add_Sample_Target(_FFTdata, label)
      label = ''
    if(i == (27-1) or i == (30-1)):#start:27 End:32 max: 27 and 31
      label = 'Lock Porch Door'
      _FFTdata = Audio.FFT(Samples[i])
      Train.Add_Sample_Target(_FFTdata, label)
      label = ''
    if(i == (34-1) or i == (33-1)):#start:33 End:35 max:    
      label = 'No Lock'
      _FFTdata = Audio.FFT(Samples[i])
      Train.Add_Sample_Target(_FFTdata, label)
      label = ''
    if(i == (36-1)):#start:36 End:38 max:    
      label = 'Running Water'
      _FFTdata = Audio.FFT(Samples[i])
      Train.Add_Sample_Target(_FFTdata, label)
      label = ''      
    if(i == (39-1)):#start:39 End:39 max:    
      label = 'Small Burner'
      _FFTdata = Audio.FFT(Samples[i])
      Train.Add_Sample_Target(_FFTdata, label)
      label = ''
    if(i == (55-1) or i == (43-1) or i == (48-1)):#start:40 End:59 max:  48@3 50@3
      label = 'Unlock'
      _FFTdata = Audio.FFT(Samples[i])
      Train.Add_Sample_Target(_FFTdata, label)
      label = ''
    if(i == (62-1) or i == (61-1)):#start:60 End:64 max:    
      label = 'Unlock with key'
      _FFTdata = Audio.FFT(Samples[i])
      Train.Add_Sample_Target(_FFTdata, label)
      label = ''
    if(i == (67-1) or i == (65-1)):#start:65 End:70 max:  66
      label = 'Unlock Porch Door'
      _FFTdata = Audio.FFT(Samples[i])
      Train.Add_Sample_Target(_FFTdata, label)
      label = ''       
  Train.FitClassification()
  

  for i in range(len(Samples)):
    _FFTdata = Audio.FFT(Samples[i])
        #Audio.graph_FFT(_FFTdata) #show the frequency range
    Audio.graph_sound(Samples[i],Sampling_time) # show the sound
    #Audio.play_sound(i)
    PredictedLabel = Train.Predict(_FFTdata)
    #print 'for',i+1,'the Prediction is:', PredictedLabel
    if(i<21 and PredictedLabel == 'Lock'):
      lockCount += 1
    if( i < 26  and i >= 21 and PredictedLabel == 'Lock with Key'):
      lockkeyCount += 1      
    if( i < 32  and i >= 26 and PredictedLabel == 'Lock Porch Door'):
      lockporchCount += 1     
    if( i < 35  and i >= 32 and PredictedLabel == 'No Lock'):
      nolockCount += 1    
    if( i < 38  and i >= 35 and PredictedLabel == 'Running Water'):
      runningwaterCount += 1      
    if( i < 59  and i >= 38 and PredictedLabel == 'Unlock'):
      unlockCount += 1  
    if( i < 64  and i >= 59 and PredictedLabel == 'Unlock with key'):
      unlockeyCount += 1  
    if( i >= 64 and PredictedLabel == 'Unlock Porch Door'):
      unlockporchCount += 1      
      
  #print 'the number of lock is:', lockCount  
  #print 'the number of lock with key is:', lockkeyCount  
  #print 'the number of lock porch is:', lockporchCount
  #print 'the number of no lock is:', nolockCount
  #print 'the number of running water is:', runningwaterCount
  #print 'the number of unlock is:', unlockCount 
  #print 'the number of unlock with key is:', unlockeyCount 
  #print 'the number of unlock porch is:', unlockporchCount
  #print 'total sum:', lockCount + lockkeyCount + lockporchCount + nolockCount + runningwaterCount  + unlockCount + unlockeyCount + unlockporchCount  
  
        
if __name__ == '__main__':
  main()
  
  
  
  
  
  
  
  
  
  
  
  