#!/bin/bash

# Description:
# install to local maven repository 
#  executing first all tests , check-styles, exec javaDoc,...

set -eu

if [ ! -f pom.xml ] ; then echo "Not pom.xml file found in current directory" ; exit 0 ; fi

if [ -z ${MVN_OPTS+x} ]; then MVN_OPTS=""; fi

mvn $MVN_OPTS clean install
