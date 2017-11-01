#!/bin/bash

# Description:
#   Exec Unit-Test mvn phase only

set -eu

if [ ! -f pom.xml ] ; then echo "Not pom.xml file found in current directory" ; exit 0 ; fi

if [ -z ${MVN_OPTS+x} ]; then MVN_OPTS=""; fi

mvn $MVN_OPTS test

CWD=`pwd`
cat << EOF



 +--------------------------------------
 | Coverage reports available at:
 | $CWD/target/site/jacoco/index.html
 +--------------------------------------


EOF
