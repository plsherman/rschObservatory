# script to recompile all programs for observatory control system
# in correct order then move the class files to /usr/local/java
# if the move fails, do the move separately from root
#
# optional parameter "copy" will skip compiles 
#
#
# From a root command prompt
# cd /usr/local/java
# cp -pv /home/pi/gitRepos/rschObservatory/*.class .
rcsum=0
s1="a"$1
echo "Run this command ith the 'copy' parameter to skip the compiles"
echo "Copy works only if user pi owns the class files in /usr/local/java\n"

if [[ "acopy" != $s1 ]]; then			# copy only not requested
  echo "\nstarting compiles\n"
  pi4j -c ObsStatus.java
  rcsum=$?
  pi4j -c ObsRequest.java
  rc=$?
  rcsum=$((rcsum + rc))
  pi4j -c ObsClientGUI.java
  rc=$?
  rcsum=$((rcsum + rc))
  pi4j -c ObsControl.java
  rc=$?
  rcsum=$((rcsum + rc))
  pi4j -c ObsWorkerThread.java
  rc=$?
  rcsum=$((rcsum + rc))
  pi4j -c ObsServer.java
  rc=$?
  rcsum=$((rcsum + rc))
fi

if [[ $rcsum -eq 0 ]]; then
  cp -pv *.class /usr/local/java
  if [[ $? -eq 0 ]]; then
    echo "class files copied"
  else
    echo "\nfile copy failed, try copy using sudo or from root"
  fi
else
  echo "compile error(s) prevented .class file copies"
  echo "fix bad compile then rerun with 'copy' parameter"
fi
