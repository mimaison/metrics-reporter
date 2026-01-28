# Strimzi Metrics Reporter release process

This document describes the steps needed to make a release of Strimzi Metrics Reporter.

## Create new release branch

The release branch should be named `release-M.N.x` where `M.N` is replaced with the major and minor versions of the new release.
For example:

```
git checkout -b release-0.2.x
```

## Set new version

Update the placeholder version, `1.0.0-SNAPSHOT`, to the version to be released (for example `0.2.0`) in the following files:

- client-metrics-reporter/pom.xml
- client-metrics-reporter/src/test/java/io/strimzi/kafka/metrics/prometheus/MetricsUtils.java
- pom.xml
- server-metrics-reporter/pom.xml

## Push the branch and tag it

Be sure to include the `-rcX` suffix for the tag, where `X` is the RC number (starting from 1), for release candidates.

For example:
```
git add .
git commit -s -m "Prepare for 0.2.0 release"
git push upstream release-0.2.x
git tag 0.2.0
git push upstream 0.2.0
```

## Run the Release GitHub Action

Run the [Release](https://github.com/strimzi/metrics-reporter/actions/workflows/release.yml) Action. Set the branch and tag for the release.
Once complete, notify a Strimzi Maintainer to publish the artifacts to Maven.

## Update the GitHub Milestone

Close the current GitHub [milestone](https://github.com/strimzi/metrics-reporter/milestones) and create the next one.

## Create the GitHub Release

From the tag, create a release. Attach the Maven artifacts and add the Strimzi sections.

Be sure to set "Set as a pre-release" for release candidates.

## Post announcements

Post the announcement in the #Strimzi channel on CNCF Slack and on the cncf-strimzi-users@lists.cncf.io mailing list.
