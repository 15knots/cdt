#!/bin/sh
#*******************************************************************************
# Copyright (c) 2006 Wind River Systems, Inc.
# All rights reserved. This program and the accompanying materials 
# are made available under the terms of the Eclipse Public License v1.0 
# which accompanies this distribution, and is available at 
# http://www.eclipse.org/legal/epl-v10.html 
# 
# Contributors: 
# Martin Oberhuber - initial API and implementation 
#*******************************************************************************
#Bootstrapping script to perform N-builds on build.eclipse.org

#nothing we do should be hidden from the world
umask 22

#Use Java5 on build.eclipse.org
#export PATH=/shared/dsdp/tm/ibm-java2-ppc64-50/bin:$PATH
#export PATH=/shared/webtools/apps/IBMJava2-ppc64-142/bin:$PATH
#export PATH=/shared/webtools/apps/IBMJava2-ppc-142/bin:$PATH
export PATH=${HOME}/ws2/IBMJava2-ppc-142/bin:$PATH

curdir=`pwd`

#Remove old logs and builds
echo "Removing old logs and builds..."
cd $HOME/ws2
rm log-N*.txt
if [ -d working/build ]; then
  rm -rf working/build
fi
if [ -d working/package ]; then
  rm -rf working/package
fi

#Do the main job
echo "Updating builder from CVS..."
cd org.eclipse.rse.build
stamp=`date +'%Y%m%d-%H%M'`
log=$HOME/ws2/log-N$stamp.txt
touch $log
cvs -q update -RPd >> $log 2>&1
daystamp=`date +'%Y%m%d-%H'`

echo "Running the builder..."
./nightly.sh >> $log 2>&1
tail -50 $log

#update the main download and archive pages: build.eclipse.org only
if [ -d /home/data/httpd/archive.eclipse.org/dsdp/tm/downloads ]; then
  cd /home/data/httpd/archive.eclipse.org/dsdp/tm/downloads
  cvs -q update -RPd >> $log 2>&1
  chgrp dsdp-tmadmin * CVS/*
  cd /home/data/httpd/download.eclipse.org/dsdp/tm/downloads
  cvs -q update -RPd >> $log 2>&1
  chgrp dsdp-tmadmin * CVS/*

  #Fixup permissions and group id on download.eclpse.org (just to be safe)
  chgrp -R dsdp-tmadmin drops/${buildType}*${daystamp}*
  chmod -R g+w drops/${buildType}*${daystamp}*
fi

#Copy latest SDK in order to give access to DOC server
cd $HOME/ws2/publish
if [ -d N.latest ]; then
  FILES=`ls N${daystamp}*/RSE-SDK-N${daystamp}*.zip 2>/dev/null`
  echo "FILES=$FILES"
  if [ "$FILES" != "" ]; then
    rm N.latest/RSE-SDK-N*.zip
    cp N${daystamp}*/RSE-SDK-N${daystamp}*.zip N.latest
    cd N.latest
    mv -f RSE-SDK-N${daystamp}*.zip RSE-SDK-latest.zip
    chgrp dsdp-tmadmin RSE-SDK-latest.zip
    chmod g+w RSE-SDK-latest.zip
  fi
fi

#Cleanup old nightly builds (leave only last 5 in place)
cd $HOME/ws2/publish
ls -d N200* | sort | head -n-5 | xargs rm -rf

