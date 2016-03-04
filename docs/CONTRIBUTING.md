# Contributing

scala-json is looking forward to receiving your feedback and pull requests!

#### Building and testing

1. Fork [scala-json](https://github.com/MediaMath/scala-json)
1. Clone your fork (we'll refer to your fork as `origin` hereafter)
1. Install [SBT](http://www.scala-sbt.org/)
1. Run `sbt test publishLocal`


    ```bash
    $ git clone git@github.com:[my-user-name]/scala-json.git
    $ cd scala-json/
    $ sbt test
    ```

## <a name="issue"></a> Found an Issue?
If you find a bug in the source code or a mistake in the documentation, help us by
submitting an [issue](https://github.com/MediaMath/scala-json/issues).

If you have a solution, you can submit a [Pull Request](#pr) with the fix, but please log the issue anyway for tracking purposes.

## <a name="pr"></a>Submitting a Pull Request
Before you submit your pull request consider the following guidelines:

* Search [GitHub](https://github.com/MediaMath/scala-json/pulls) for an open or closed Pull Request that relates to your submission.
* Make sure your fork is [synched](https://help.github.com/articles/syncing-a-fork/)
* Open an issue first to discuss any potential changes.
* Create a new branch from `develop`, the current stable version.
* Branch name should be in the format: `<github.id>-<issue#>-<iteration>` (ex: `colinrgodsey-1853-2`)
* Make your changes in a new git branch:

     ```bash
     git checkout -b mygithub-1234-1
     ```
* Create your patch
* Follow our [Coding Guidelines](#guidelines).
* Commit your changes using a descriptive commit message
* Push your branch to GitHub:

    ```bash
    git push origin mygithub-1234-1
    ```
* In GitHub, send a pull request to `scala-json:develop`

## <a name="cr"></a>Code Review
If code review suggests changes...

* Make the required updates
* [Rebase](https://help.github.com/articles/about-git-rebase/) your branch and force push to your GitHub repository (this will update your Pull Request):

    ```bash
    git fetch upstream
    git rebase -i upstream/develop
    git push -f origin mygithub-1234-1
    ```
* When you solution is approved, please [squash](https://help.github.com/articles/about-git-rebase/) your commits

Thanks for your contribution!

#### After your pull request is merged

After your pull request is merged, you can safely delete your branch and pull the changes
from the main (upstream) repository.

## <a name="compat"></a> API Compatibility

We follow the major version of the release to signify API compatibility. The `develop` branch is the staging area
for the next API-compatible release. Any other changes that may break compatibility will need a pull request made
to a specific development branch for that future major version. These semantics will need to be discussed in the issue
before any pull request is made.

## <a name="guidelines"></a> Coding Guidelines
To ensure consistency throughout the source code, keep these rules in mind as you are working:

* All features or bug fixes **must be tested**
* All public API methods **must be documented** with scaladocs
* Regarding code styling in general:
    * scala-json follows the standard [scala style guide](http://docs.scala-lang.org/style/)
    * Methods and properties should be named in a meaningful way
    * Please refer to any of the scala-json components or shared files for reference
