Collection of utility scripts wrapping common task (mvn local install, test,...)

Example ussage:

     $ cd /home/interledger/workspace01/quilt/
     $ ./dev-ops/util/quick_local_install_mvn_package.sh
     Output must be similar to:
     ...
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
    [INFO] Total time: 5.098 s
    ..
    $ cd ilp-core
    $ ../dev-ops/util/unit_tests.sh
    Output must be similar to:
    ...
    [INFO] BUILD SUCCESS
    ...
    +--------------------------------------
    | Coverage reports available at:
    | /home/interledger/workspace01/quilt/ilp-core/target/site/jacoco/index.html
    +--------------------------------------


Environment Variables: 
`MVN_OPTS`: pass extra options to maven. 
