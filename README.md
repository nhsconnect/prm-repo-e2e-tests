# continuity-service-e2e-tests

These are end to end tests that run across all microservices that comprise the continuity service.

## Top level test types

* EndToEndTest

The functional tests from putting test events into our mesh mailbox, and ensuring
that the events are processed through to the intended output/outcome queues.

* PerformanceTest

Long running load tests of the end to end system, starting with test events generated
and posted into our mesh mailbox and checking arrival on mof-updated queue, with some
performance analysis charts and anomaly output generated.

* PdsAdaptorTest

A small test to verify the test process can talk to PDS adaptor

## Running locally

Local runs require authentication first. Examples use `dev` as intended environment.

### Authentication

Local runs authenticated by local user - you - are done by you assuming role into
the appropriate environment.

### Running the tests once authenticated

Can be run in intellij or dojo (or directly through local gradle but that is not covered here).

#### Running in intellij - fastest

Issue your assume role command to get access to the appropriate values for:

```
AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_SECURITY_TOKEN AWS_SESSION_TOKEN
```

Then set these in the run configuration for the tests, the JUnit run type is faster
that the gradle one (this can be set in the intellij settings for `Build Tools - Gradle`).

Getting these values depends on your assume role method, for example i use `assume-role`
which outputs `export` statements, so i use this command to filter into a format
i can paste into intellij Run Test config environment variables:

```
assume-role dev | sed 's/export //g' | sed 's/\"//g'
```

The tests can then be run as normal in IDE, until the assume role expires and its
environment will need to be updated

*NB* This puts a limit on how long tests can be run for locally.  Longer running tests
will need to be run in the pipeline, using the underlying AWS instance identity.

#### Full run in dojo - closest to pipeline

For end to end functional tests:

```
NHS_ENVIRONMENT=dev ./tasks test_e2e
```

Performance tests:

```
NHS_ENVIRONMENT=dev ./tasks test_performance_local
```

#### Full run in persistent dojo - faster

First start a dojo container (in authenticated terminal) using for example for `dev`:

```
NHS_ENVIRONMENT=dev dojo
```

Then, within the dojo container session:

For end to end functional tests:

```
./tasks _test_e2e
```

## Running in pipeline

Please see `gocd.yml` files in `gocd/`.