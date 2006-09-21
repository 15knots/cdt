#!/bin/sh
# Shell script to start an RSE communications server
# This script will start the datastore server listening on an available socket
serverpath=.;
CLASSPATH=.:dstore_extra_server.jar:dstore_core.jar:dstore_miners.jar:clientserver.jar:$CLASSPATH;
export serverpath CLASSPATH
if [ $1 ]                     
then java -DA_PLUGIN_PATH=$serverpath -DDSTORE_TRACING_ON=false -Dclient.username=$1 -DDSTORE_SPIRIT_ON=true org.eclipse.dstore.core.server.Server 0 60000 &
else java -DA_PLUGIN_PATH=$serverpath -DDSTORE_TRACING_ON=false -DDSTORE_SPIRIT_ON=true org.eclipse.dstore.core.server.Server 0 60000 &
fi
