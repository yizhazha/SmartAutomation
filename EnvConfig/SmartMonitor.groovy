import hudson.model.*
import hudson.node_monitors.*
import hudson.slaves.*
import java.util.concurrent.*
 
jenkins = Hudson.instance

import javax.activation.*

def getEnviron(computer) {
   def env
   def thread = Thread.start("Getting env from ${computer.name}", { env = computer.environment })
   thread.join(2000)
   if (thread.isAlive()) thread.interrupt()
   env
}
 
def slaveAccessible(computer) {
    getEnviron(computer)?.get('PATH') != null
}

def offline = new File("C:/SmartAutomation/offline.txt");
if (offline.exists())
    offline.delete()
def numberOfflineNodes = 0
def numberNodes = 0
for (slave in jenkins.slaves) {
   def computer = slave.computer
   numberNodes ++
   println ""
   println "Checking computer ${computer.name}:"
   def isOK = (slaveAccessible(computer) && !computer.offline)
   if (isOK) {
     println "\t\tOK, got PATH back from slave ${computer.name}."
     println('\tcomputer.isOffline: ' + slave.getComputer().isOffline()); 
     println('\tcomputer.isTemporarilyOffline: ' + slave.getComputer().isTemporarilyOffline());
     println('\tcomputer.getOfflineCause: ' + slave.getComputer().getOfflineCause());
     println('\tcomputer.offline: ' + computer.offline);        
   } else {
     numberOfflineNodes ++
     println "  ERROR: can't get PATH from slave ${computer.name}."
     println('\tcomputer.isOffline: ' + slave.getComputer().isOffline()); 
     println('\tcomputer.isTemporarilyOffline: ' + slave.getComputer().isTemporarilyOffline());
     println('\tcomputer.getOfflineCause: ' + slave.getComputer().getOfflineCause());
     println('\tcomputer.offline: ' + computer.offline);      
     offline.append("${computer.name}\n");
     if (slave.getComputer().isTemporarilyOffline()) {
      if (!slave.getComputer().getOfflineCause().toString().contains("Disconnected by")) {
         computer.setTemporarilyOffline(false, slave.getComputer().getOfflineCause())        
      }
     } else {
         computer.connect(true)  
     }
   }
 }
println ("Number of Offline Nodes: " + numberOfflineNodes)
println ("Number of Nodes: " + numberNodes)  