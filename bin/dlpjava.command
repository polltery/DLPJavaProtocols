#!/bin/sh
if [ $# -eq 0 ]
then
	echo Usage: $0 [-b] protocol [parameter file]
	exit 1
fi
if [ "$1" = "-x" ]
then
    java -jar /dlpjava.jar
    exit 0
fi
DLPBATCH=
if [ "$1" = "-b" ]
then
	shift
	DLPBATCH=-b
fi
DLPIMPL=`basename $1 .java`
shift
exec make  -r -f dlpjava.mk DLP=$DLPIMPL ARG="$*" BATCH=$DLPBATCH  all