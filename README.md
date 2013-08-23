# clojuredocs-analyzer

## Requirements

* Clojure 1.5.1
* Java 7 (needed to parse all the Clojure core files properly)

## Getting Started

NB this project is due to be replaced soon - please see https://github.com/clojuredocs for more information

To run a local version of Clojuredocs please follow the instructions at https://github.com/zk/clojuredocs

### Import Clojure library

To import a Clojure library into your local version:

    git clone git@github.com:zk/clojuredocs-analyzer.git
    cd clojuredocs-analyzer
    lein run "path/to/library"


### Import Clojure Core

To import Clojure core into your local version is more involved:

First, clone Clojure to somewhere on your machine

    git clone git@github.com:clojure/clojure.git
    cd clojure

Then checkout the tag that you want to import

    git checkout clojure-1.5.1

Then clone this analyzer as before:

    git clone git@github.com:zk/clojuredocs-analyzer.git
    cd clojuredocs-analyzer

Then make sure that the `parse-clojure-core` function in `src/cd_analyzer/core.clj` matches the version number you are importing.
eg - `https://github.com/zk/clojuredocs-analyzer/blob/master/src/cd_analyzer/core.clj#L82`

Then run `lein repl` to bring up a prompt and run the following commands

    (use 'cd-analyzer.core)
    (run-update-clojure-core "path/to/clojure/on/my/filesystem")

## License

EPL 1.0 http://www.eclipse.org/legal/epl-v10.html
