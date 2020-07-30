# HBGDki REST services automated tests.

The Frisby framework is used for testing REST services.

## Installation

### Install node.js with NPM on the server if required.

[Official pre-build installer](https://nodejs.org/download/)

### Install jasmine-node as global module

    npm install -g jasmine-node

### Install frisby-tests package
    cd user-management/frisby-tests
    npm install

NPM will install Frisby as local dependency.

## Running Tests

Frisby is built on top of the jasmine BDD spec framework, and uses the excellent [jasmine-node test runner](https://github.com/mhevery/jasmine-node) to run spec tests in a specified target directory.  

### File naming conventions

Files must end with `spec.js` to run with jasmine-node.

Suggested file naming is to append the filename with `_spec`, like `mytests_spec.js` and `moretests_spec.js`

### Example: run user management service tests from the CLI

    jasmine-node /spec/ums --junitreport

Test log will be sent to console and JUnit reports will be saved to `reports` folder.