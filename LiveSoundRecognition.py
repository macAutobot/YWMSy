# Project Name: Sound Recognition
# Date: 1/29/2014
# Version: 2.1
# Summary: this code is ment to record
# a sample of data and predict the type
# of sound being recorded.'

import sys
from PyQt4 import QtGui
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

##################     Classes     ##################
#Description:
#SoundRecognitionGUI--
#SoundRecognitionSVM_Training--
#SoundRecognitionAudio--
#SoundRecognitionSVM_Learning--


##################################
class SoundRecognitionGUI(object):
  def __SetupGUI(self):
    print "Initialize GUI variables"
  def SetWindow(self):
    print "Set window"
  def SetButtons(self):
    print "Set buttons"
  def SetTextBox(self):
    print "Set text boxes"
  def DisplayResults(self):
    print "Display GUI Results"
    
###########################################
class SoundRecognitionSVM_Learning(object):
  def __SetupLearning(self):
    print "Initialize variables for Learning"
  def UserInput(self):
    print "Ask user if prediction is correct"
  def predict(self):
    print "predict with Recorded sample data"
  def UpdateResults(self):
    print "Update the training set"
  def ErrorPercent(self):
    print "Keep track of errors and results"
 
 
 
 
 
###########################################
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
    results = self.__PrintLabel(results)
    print 'The Results are: ', results
    
  def __PrintLabel(self,results):
     return label[results]
   
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

  def UpdateLabel(self):
    print "Update Labels"
  def update_Sample_Target(self):
    print "Update sample data and target"
  def graph_Database(self):
    print "Graph groupings of samples and targets"

###################################
class SoundRecognitionAudio(object):
  def SetupAudio(self):
    self.waveCount = 0
  
  def GetAudioBuff(self):
    print "Get a buffer used for Audio"
  
  def FFT(self, soundData):
    floatArray = [float(x.strip('\n')) for x in soundData]
    FFT = abs(fft.fft(floatArray))
    Maximum = max(FFT)
    for i in range(len(FFT)):
      FFT[i] = (FFT[i]/Maximum)*22050 #arbitrary number
    FFT = array(FFT)
    return FFT
     
  def WavToArray(self, WavFile, filecount, Recorded):
    sampleWidth = WavFile.getsampwidth()
    fmts = (None, "=B", "=h", None, "=l")
    fmt = fmts[sampleWidth]
    dcs = (None, 128, 0, None, 0)
    dc = dcs[sampleWidth]
    self.soundData = [0] * WavFile.getnframes()
    if(Recorded == 'Yes'):
      filename = '/home/randy/Desktop/Audio/cmpr/data_1.txt'
    else:
      filename = '/home/randy/Desktop/Audio/My Place/Sounds/Soundtxt/'+filecount+'.txt'
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
    sampleRate = int(math.ceil(44100.*SecToEdit))
    if(lenghtofdata <= sampleRate):
      newarray = [0]*sampleRate
      for i in range(len(newarray)):
	if(i <= (lenghtofdata)):
	  newarray[i] = sounddata[i-1]
	  self.soundfile.write(newarray[i] + "\n")
	if(i > (lenghtofdata)):
	  newarray[i] = '0'
	  self.soundfile.write(newarray[i] + "\n")
    if(lenghtofdata > sampleRate):
      newarray = [0]*sampleRate
      for i in range(len(newarray)):
	newarray[i] = sounddata[i]
	self.soundfile.write(newarray[i] + "\n")
    data = [newarray,sampleRate]
    self.soundfile.close
    return data
    
  def openFile(self,filecount,Recorded):
    filecounter = filecount+1
    text = str(filecounter)
    if(Recorded == 'Yes'):
      text = '/home/randy/Desktop/Audio/cmpr/wave_1.wav'
    else:
      text = '/home/randy/Desktop/Audio/My Place/Sounds/'+ text +'.wav'
    
    WaveFile = wave.open(text,'r')
    data = self.WavToArray(WaveFile,str(filecounter),Recorded)
    WaveFile.close
    return data

  def openText(self, filecount):
    filecounter = filecount+1
    text = str(filecounter)
    fileName = '/home/randy/Desktop/Audio/My Place/Sounds/Soundtxt/' + text +'.txt'
    count = len(open(fileName,'r').readlines())
    DataArray = [0] * count
    Var1 = open(fileName,'r')
    for i in range(count):
      DataArray[i] = Var1.readline()
    Var1.close
    return DataArray
  
  def RecordSample(self, recordingTime):
    print "Start Recording"
    micChannel = pyaudio.PyAudio()
    micStream = micChannel.open(format = pyaudio.paInt16, channels = 1, rate = 44100, input = True, frames_per_buffer =  1024)
    frames = []
    for i in range(0, int(44100/(1024/recordingTime))):
      data = micStream.read(1024)
      frames.append(data)
    print 'End of Recording.'
    micStream.stop_stream()
    micStream.close()
    micChannel.terminate()
    samplewave = wave.open('/home/randy/Desktop/Audio/cmpr/wave_1.wav','wb')
    samplewave.setnchannels(1)
    samplewave.setsampwidth(micChannel.get_sample_size(pyaudio.paInt16))
    samplewave.setframerate(44100)
    samplewave.writeframes(b''.join(frames))
    samplewave.close()
    data = self.openFile(0,'Yes')
    return data
  
  
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
    
  def graph_sound(self, DataToGraph, samplingRate):
    seconds = samplingRate/44100.
    Xspace = linspace(0,seconds,samplingRate)
    plt.plot(Xspace,DataToGraph)
    plt.xlabel('time (s)')
    plt.ylabel('Magnitude')
    plt.title('Sound Signal')
    plt.show()
    
##############        Main        ###############
#Description:

def main():
  #create an instance of each class
  Train = SoundRecognitionSVM_Training()
  Audio = SoundRecognitionAudio()
  #initialize the variables used in training
  Train.SetupTraining()
  Audio.SetupAudio()
  
  
  # this is false if all wav files have been loaded
  # into text format
  firstTime = True
  #open the wav files and retrun the text interpretation 
  #of it there are 46 wav files
  Samples = [0] * 46
  SamplingRate = [0] * 46
  # how many secods do you want to sample
  Sampling_time = 1.088435374


  for i in range(len(Samples)):
    if(firstTime):
      Recorded = 'No'
      Samples[i] = Audio.openFile(i, Recorded)
      Samples[i], SamplingRate[i] = Audio.EditSample(Samples[i],Sampling_time)
      
    # this opens the text data instead of wav witch takes longer
    #Samples[i] = Audio.openText(i)
    if(i == 0):
      label = 'BT Closing'
    if(i == 5 or i == 7):
      label = 'BT Opening door'
    if(i == 9):
      label = 'BT fan'
    if(i == 15):
      label = 'BT Running Water'
    if(i == 23):
      label = 'Hello'
    if(i == 25 or i == 27):
      label = 'KT running water'
    if(i == 30 or i == 35 or i == 31 or i == 33 or i == 32):
      label = 'Lock Door'
    if(i == 38 or i == 43 or i == 39 or i == 41):
      label = 'Unlock Door'
    _FFTdata = Audio.FFT(Samples[i])
    Train.Add_Sample_Target(_FFTdata, label)
  Train.FitClassification()
  
  # this is a inf loop to ask user for sample and predict based on training
  while (True):
    UserResponce = raw_input("Press s to start: ")
    NumSample = UserResponce
    if(NumSample == 's'):
      rec_data = Audio.RecordSample(Sampling_time)
      data,samplingrate = Audio.EditSample(rec_data,Sampling_time)
      _FFTdata = Audio.FFT(data)
      Train.Predict(_FFTdata)
      UsrResp = raw_input("Iis this correct?")
      if(UsrResp == 'n'):
	label = raw_input("what would you like to call it?")
	Train.Add_Sample_Target(_FFTdata, label)
	Train.FitClassification()
    else:
      resp = raw_input('Do you want to quit?(y/n):')
      if(resp == 'y'):
	break
      
if __name__ == '__main__':
  main()