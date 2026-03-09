# Development Version and Branch Handling

This document describes the branching strategy, version numbering, CI/CD pipeline, and development workflow for this JUDO module.

## Branches

The versioning policy follows [GitFlow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow):

| Branch Pattern | Purpose |
|---|---|
| `develop` | Main development branch — latest sources for the current active version |
| `feature/JNG-NUMBER_short_summary` | Feature branches based on `develop` — new functionality for the active version |
| `(release/)X_Y_betaN` | Release branches (the `release/` prefix is reserved for CI) |
| `bugfix/JNG-NUMBER_short_summary` | Bug fixes on release branches — must also be applied to newer release and develop branches |
| `support/JNG-NUMBER_short_summary` | Support branches on release branches — minor changes for a previous release |
| `master` | Latest released sources of the active version |

### Branch Flow

```mermaid
gitGraph
    commit id: "initial"
    branch develop
    commit id: "dev-1"
    branch feature/JNG-1
    commit id: "feat-1"
    commit id: "feat-2"
    checkout develop
    merge feature/JNG-1 id: "merge-feat-1"
    branch feature/JNG-3
    commit id: "feat-3"
    checkout develop
    merge feature/JNG-3 id: "merge-feat-3"
    branch release/1.0-beta1
    commit id: "rc-1"
    branch bugfix/JNG-4
    commit id: "bugfix"
    checkout release/1.0-beta1
    merge bugfix/JNG-4 id: "merge-bugfix"
    checkout develop
    merge release/1.0-beta1 id: "merge-release"
    checkout master
    merge release/1.0-beta1 id: "release-1.0"
```

## Version Numbers

Versions follow semantic versioning with these rules:

| Action | Version Change |
|---|---|
| Starting a `feature/` branch | No version change |
| Starting a release branch from `develop` | 2nd number (minor) incremented on `develop` |
| Starting a `bugfix/` branch | No version change — applied to release branches during pre-release testing |
| Starting a `support/` branch | 3rd number (patch) incremented |
| Starting a `hotfix/` branch | 4th number incremented — applied to both release and master |

## GitHub Actions CI/CD Pipeline

The CI/CD system consists of four interconnected workflows:

### build.yml — Main Build Pipeline

```mermaid
flowchart TD
    trigger["Push on develop<br/>or PR on develop / master /<br/>increment/* / release/*"]
    trigger --> check{"Base branch?"}
    check -->|"master, release/*"| version_release["Set version from pom.xml<br/>(without -SNAPSHOT)"]
    check -->|"develop, increment/*"| version_dev["Set version:<br/>major.minor.qualifier.date_commitId_branch"]
    version_release --> build["Build & deploy to Nexus"]
    version_dev --> build
    build --> tag["Create git tag v<version>"]
    tag --> pr_check{"Branch type?"}
    pr_check -->|"increment/*, release/*"| merge_tag["Create tag merge-pr/<version>"]
    merge_tag --> trigger_merge["Trigger merge-pr-tagged.yml"]
    pr_check -->|"develop"| changelog["Build changelog"]
    changelog --> gh_release["Create GitHub pre-release"]
```

### merge-pr-tagged.yml — Pull Request Merge Automation

```mermaid
flowchart TD
    trigger["Push on merge-pr/* tag"]
    trigger --> extract["Extract version from tag"]
    extract --> check{"Version format?"}
    check -->|"major.minor.qualifier"| merge_master["Merge PR to master"]
    merge_master --> trigger_release["Trigger create-release-on-master.yml"]
    check -->|"other"| squash_develop["Squash PR to develop"]
    squash_develop --> trigger_build["Trigger build.yml"]
    merge_master --> cleanup["Delete merge-pr/* tag"]
    squash_develop --> cleanup
```

### create-release-on-master.yml — Release Publication

```mermaid
flowchart TD
    trigger["Push on master"]
    trigger --> get_version["Get version from tag"]
    get_version --> changelog["Build changelog"]
    changelog --> release["Create GitHub release (latest)"]
```

### release.yml — Manual Release Trigger

```mermaid
flowchart TD
    trigger["Manual trigger with version"]
    trigger --> check{"Version = 'auto'?"}
    check -->|"yes"| auto["Read version from pom.xml<br/>(without -SNAPSHOT)"]
    check -->|"no"| manual["Use provided version"]
    auto --> next["Set next version = qualifier + 1"]
    manual --> next
    next --> pr_master["Create PR on master<br/>with release version"]
    next --> pr_develop["Create PR on develop<br/>with next version"]
    pr_master --> build1["Trigger build.yml"]
    pr_develop --> build2["Trigger build.yml"]
```

## How to Develop

For issue tracking, the project uses [JIRA](https://blackbelt.atlassian.net/jira/dashboards).

> **Important:** There is no commit without a ticket number. Every pull request and commit message must include a `JNG-xxx` reference.
