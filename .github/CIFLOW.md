# Development version and branch handling

## Branches

Versioning policy of JUDO NG modules are based on GitFlow: https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow.

Branches:

* **develop**: development branch contains latest development sources of the last active version
* **feature/JNG-NUMBER_short_summary**: feature branches are based on **develop** and contains sources of new features that will be included in last active version
* **(release/)1_0_beta1**: release branches of 1.0-beta1 (release/ prefix is still reserved for CI)
* **bugfix/JNG-NUMBER_short_summary**, **support/JNG-NUMBER_short_summary**: bugfix and support branches are based on release branches and must be applied to release and development branches of newer versions too
* **master**: contains latest released sources of the last active version

```mermaid
flowchart LR
    subgraph Legend
        M[master]:::master
        D[develop]:::develop
        F[feature/*]:::feature
        R[release/*]:::release
        B[bugfix/*]:::bugfix
        S[support/*]:::support
        H[hotfix/*]:::hotfix
    end

    classDef master fill:#7CFC00
    classDef develop fill:#6495ED
    classDef feature fill:#FFD700
    classDef release fill:#00FFFF
    classDef bugfix fill:#FF6347
    classDef support fill:#7FFFD4
    classDef hotfix fill:#FF4500
```

## Version numbers

Version numbers are increased using semantic versioning:

* do not change version numbers on starting feature/ branches
* 2nd number in version of **develop** branch is increased when a release branch started
* do not change version numbers on bugfix/ branches - that are applied on release branches during testing before releasing it (merging to master)
* 3rd number in version of support/ branches is increased when started - it is used to support a previous release including new (minor) changes; support/ branches are merged back to release branch when update is released (without merging changes to master)
* 4th number in version of hotfix/ branches is increased when started (that are applied on both release and master branches)

### GitHub action flows

#### build.yml

```mermaid
flowchart TD
    A[Push on develop branch<br/>or<br/>Pull request on develop, master,<br/>increment/*, release/* branch] --> B{Commit or PR base branch?}
    B -->|master, release/*| C[Set version from pom.xml<br/>version without '-SNAPSHOT']
    B -->|develop, increment/*| D[Set version<br/>major.minor.qualifier.date_commitId_branchName]
    C --> E[Build and deploy to nexus]
    D --> E
    E --> F[Create git tag v-version-]
    F --> G{PR or commit base branch?}
    G -->|increment/*, release/*| H[Create tag merge-pr/version]
    H --> I[Trigger merge-pr-tagged.yml]
    G -->|develop| J[Build change log]
    J --> K[Create github release prerelease<br/>with change log]
    K --> L[End]
    I --> L
```

#### merge-pr-tagged.yml

```mermaid
flowchart TD
    A[Push on merge-pr/* tag] --> B[Get version from tag name]
    B --> C{Check version format}
    C -->|major.minor.qualifier| D[Merge PR to master]
    D --> E[Trigger create-release-on-master.yml]
    C -->|other| F[Squash PR to develop]
    F --> G[Trigger build.yml]
    E --> H[Delete tag merge-pr/version]
    G --> H
    H --> I[End]
```

#### create-release-on-master.yml

```mermaid
flowchart TD
    A[Push on master branch] --> B[Get version from tag name]
    B --> C[Build change log]
    C --> D[Create github release last<br/>with change log]
    D --> E[End]
```

#### release.yml

```mermaid
flowchart TD
    A[Manually triggered with given version<br/>which is 'auto' or major.minor.qualifier] --> B{given version is?}
    B -->|auto| C[Set release version from pom.xml<br/>version without '-SNAPSHOT']
    B -->|other| D[Set release version to given version]
    C --> E[Set next version to<br/>release version qualifier + 1]
    D --> E
    E --> F[Create PR on master with release version]
    F --> G[Trigger build.yml]
    E --> H[Create PR on develop with next version]
    H --> I[Trigger build.yml]
    G --> J[End]
    I --> J
```

## How to develop

For issue tracking we are using [JIRA](https://blackbelt.atlassian.net/jira/dashboards). Golden rule:

> **IMPORTANT**: *There is no commit without ticket number*

So for pull request or commit `JNG-xxx` have to be presented in the commit.
