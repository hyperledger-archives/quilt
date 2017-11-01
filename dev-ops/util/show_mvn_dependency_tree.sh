#!/bin/bash

# Description:
#   Show maven dependency graph for pom.xml in CWD

set -eu

if [ ! -f pom.xml ] ; then echo "Not pom.xml file found in current directory" ; exit 0 ; fi

if [ -z ${MVN_OPTS+x} ]; then MVN_OPTS=""; fi

# REF: Listing map phase (1 <-> N) goals:
#  https://stackoverflow.com/questions/1709625/maven-command-to-list-lifecycle-phases-along-with-bound-goals
#  mvn fr.jcgay.maven.plugins:buildplan-maven-plugin:list -Dbuildplan.tasks=install


mvn $MVN_OPTS dependency:tree
