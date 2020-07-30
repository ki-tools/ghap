/**
 * Created by Vlad on 17.11.2015.
 */

// implements steps 1-7 of
// https://tools.certara.com/jira/browse/BAP-571

"use strict";

var my = require('./ghap-e2e-lib');
var cfg = require('./ghap-e2e-config');

var ghapLoginCase = require('./ghap-login-case');
var ghapLogoutCase = require('./ghap-logout-case');

var programsListEl;
var firstProgramName;
var firstGrantName;
var addProgramAndGrantsForm = new AddProgramAndGrantsForm();
var newProgramName = "TestE2ENewProject";
var newGrantName = 'G1';

ghapLoginCase.openLoginPageSpec()
    .then(ghapLoginCase.loginAs.bind(this, cfg.adminName, cfg.adminPassword))
    .then(openProgramSetupPage)
    .then(selectFirstProgramAndGrant)
    .then(function(){
        return addExistingProgramAndGrant(firstProgramName, firstGrantName)
    })
    .then(function(){
        return addNewProgramAndGrant(newProgramName, newGrantName)
    })
    .then(function(){
        return deleteProgram(newProgramName)
    })
    .then(ghapLogoutCase.logOut)
    .thenCatch(my.reportError)
    .thenFinally(finished);

function AddProgramAndGrantsForm () {
    this.program_name_input = null;
    this.grants_inputs = [];
    this.reset_button = null;
    this.save_button = null;
}

AddProgramAndGrantsForm.prototype.update = function() {
    this.program_name_input = element(by.model('selectedProgram.name'));
    this.grants_inputs = element.all(by.repeater('grant in grants'));
    this.add_program_el = $('[ng-click="reset(true)"]');
    this.add_grant_el = $('[ng-click="addGrant()"]');
    this.reset_button = $('input[ng-click="reset()"]');
    this.save_button = $('input.submit-button[value="SAVE"]');
    this.err_message_div = $('div.error-message-cont');
    this.success_message_div = $('div.success-message-cont');
    return this;
};

AddProgramAndGrantsForm.prototype.getProgramName = function() {
    return this.program_name_input.getAttribute('value');
};

AddProgramAndGrantsForm.prototype.setProgramName = function(program_name) {
    return this.program_name_input.clear().sendKeys(program_name);
};

AddProgramAndGrantsForm.prototype.getGrantName = function(grant_num) {
    var self = this;
    return this.grants_inputs.count()
        .then(function(grants_count) {
            if ( (grants_count - grant_num) > 0) {
                return self.grants_inputs.get(grant_num).$('input').getAttribute('value')
            } else {
                console.log("Can not get grant %d of %d", grant_num, grants_count);
                return null;
            }
        });
};

AddProgramAndGrantsForm.prototype.setGrantName = function(grant_num, grant_name) {
    var self = this;
    return this.grants_inputs.count()
        .then(function(grants_count) {
            if ( (grants_count - grant_num) > 0) {
                return self.grants_inputs.get(grant_num).$('input').clear().sendKeys(grant_name);
            } else {
                console.log("Can not set grant %d of %d", grant_num, grants_count);
                return null;
            }
        });
};

AddProgramAndGrantsForm.prototype.clickReset = function() {
    return this.reset_button.click();
};

AddProgramAndGrantsForm.prototype.clickSave = function() {
    return this.save_button.click();
};

AddProgramAndGrantsForm.prototype.clickAddProgram = function() {
    return this.add_program_el.click();
};

AddProgramAndGrantsForm.prototype.clickAddGrant = function() {
    return this.add_grant_el.click();
};

AddProgramAndGrantsForm.prototype.getErrorMessage = function() {
    var self = this;
    return self.err_message_div.isDisplayed()
        .then(function(is_displayed) {
            if (is_displayed) {
                return self.err_message_div.getText()
                    .then(function(text){
                        console.log("Error message is displayed: '%s'", text);
                        return text;
                    })
            } else {
                return null;
            }
        })
};

AddProgramAndGrantsForm.prototype.getSuccessMessage = function() {
    var self = this;
    return self.success_message_div.isDisplayed()
        .then(function(is_displayed) {
            if (is_displayed) {
                return self.success_message_div.getText()
                    .then(function(text){
                        console.log("Success message is displayed: '%s'", text);
                        return text;
                    })
            } else {
                return null;
            }
        })
};

function openProgramSetupPage() {

    var deferred = protractor.promise.defer();
    var failFast = my.createFailFast(deferred);

    describe("Open Program Setup Page", function () {

        beforeEach(my.logSpecFullName);
        failFast.setAfterEach();

        it('- navigate to PROGRAM MANAGEMENT tab', function () {
            var programManagement_pattern = new RegExp(/^Program[.\s]+Management$/i);
            my.getTabLink(programManagement_pattern)
                .then(function(link){ link.click() });
        });

        it('- current page should be Manage Program page', function () {
            var h1_el = $("div.content h1");
            expect(h1_el.getText()).toMatch(/^Manage Program$/i);
        });

        it('- program list should not be empty', function () {
            programsListEl = element.all(by.repeater('program in programs'));
            expect(programsListEl.count()).toBeGreaterThan(0);
        });

        it('- fields in the form should be empty', function () {
            addProgramAndGrantsForm.update();
            addProgramAndGrantsForm.getProgramName()
              .then(function(program_name){
                  expect(program_name).toBe('');
              });
            addProgramAndGrantsForm.getGrantName(0)
              .then(function(grant_name){
                  expect(grant_name).toBe('');
              })
        });

        failFast.fulfillDeferSpec();

    });

    return deferred.promise;

}

function selectFirstProgramAndGrant() {
    var deferred = protractor.promise.defer();
    var failFast = my.createFailFast(deferred);

    describe("Get first program and grant names", function () {

        beforeEach(my.logSpecFullName);
        failFast.setAfterEach();

        it('- click on first program', function () {
            var fist_program_div_el = programsListEl.get(0).$('div[ng-click="getGrants(program)"]');
            fist_program_div_el.click();
        });

        it('- program name field should not be empty', function () {
            addProgramAndGrantsForm.update();
            addProgramAndGrantsForm.getProgramName()
                .then(function(program_name){
                    expect(program_name).toBeTruthy();
                    firstProgramName = program_name;
                })
        });

        it('- grant name field should not be empty', function () {
            addProgramAndGrantsForm.getGrantName(0)
                .then(function(grant_name){
                    expect(grant_name).toBeTruthy();
                    firstGrantName = grant_name;
                    console.log("First grant is '%s/%s'", firstProgramName, firstGrantName);
                })
        });

        it('- click on RESET button should reset program and grant names fields', function () {
            addProgramAndGrantsForm.setProgramName('New program');
            addProgramAndGrantsForm.setGrantName(0, 'New grant');
            addProgramAndGrantsForm.clickReset();
            expect(addProgramAndGrantsForm.getProgramName()).toBe(firstProgramName)
            expect(addProgramAndGrantsForm.getGrantName(0)).toBe(firstGrantName)
        });

        failFast.fulfillDeferSpec();

    });

    return deferred.promise;

}

function addExistingProgramAndGrant( program_name, grant_name) {
    var deferred = protractor.promise.defer();
    var failFast = my.createFailFast(deferred);

    describe("Add existing program/grant", function () {

        beforeEach(my.logSpecFullName);
        failFast.setAfterEach();

        var lastGrant_index;

        it('- click on (+)Add grant should add empty input for grant', function () {
            addProgramAndGrantsForm.clickAddGrant();
            addProgramAndGrantsForm.grants_inputs.count()
              .then(function (count) {
                  lastGrant_index = count-1;
                  expect(addProgramAndGrantsForm.getGrantName(lastGrant_index)).toBe('')
              });
        });

        it('- save of existing grant name should display error', function () {
            addProgramAndGrantsForm.setGrantName(lastGrant_index, grant_name);
            addProgramAndGrantsForm.clickSave();
            expect(addProgramAndGrantsForm.getErrorMessage()).toBeTruthy();
        });

        it('- click on (+)Add program should empty inputs', function () {
            addProgramAndGrantsForm.clickAddProgram();
            expect(addProgramAndGrantsForm.getProgramName()).toBe('');
            expect(addProgramAndGrantsForm.getGrantName(0)).toBe('');
            expect(addProgramAndGrantsForm.grants_inputs.count()).toBe(1);
        });

        it('- save of existing program name should display error', function () {
            addProgramAndGrantsForm.setProgramName(program_name);
            addProgramAndGrantsForm.setGrantName(0, 'New grant');
            addProgramAndGrantsForm.clickSave();
            expect(addProgramAndGrantsForm.getErrorMessage()).toBeTruthy();
        });

        failFast.fulfillDeferSpec();

    });

    return deferred.promise;
}

function addNewProgramAndGrant(program_name, grant_name) {
    var deferred = protractor.promise.defer();
    var failFast = my.createFailFast(deferred);

    describe("Add new program and grant '"+program_name+'/'+grant_name+"'", function () {

        beforeEach(my.logSpecFullName);
        failFast.setAfterEach();

        it('- update names', function () {
            addProgramAndGrantsForm.update();
            addProgramAndGrantsForm.clickReset();
            addProgramAndGrantsForm.setProgramName(program_name);
            addProgramAndGrantsForm.setGrantName(0,grant_name);
            expect(addProgramAndGrantsForm.getProgramName()).toBe(program_name);
            expect(addProgramAndGrantsForm.getGrantName(0)).toBe(grant_name)
        });

        it('- click on SAVE should display success message', function () {
            addProgramAndGrantsForm.clickSave();
            expect(addProgramAndGrantsForm.getErrorMessage()).toBeFalsy();
            expect(addProgramAndGrantsForm.getSuccessMessage()).toBeTruthy();
        });

        it("- '"+program_name+"' should appears to the program list", function () {
            filterPrograms(program_name);
            expect(getDisplayedProgramsNames().then(function(programs){
                return (programs.indexOf(program_name) > -1)
            })).toBeTruthy()
        });

        failFast.fulfillDeferSpec();

    });

    return deferred.promise;
}

function deleteProgram(program_name) {
    var deferred = protractor.promise.defer();
    var failFast = my.createFailFast(deferred);

    describe("Delete program '"+program_name, function () {

        beforeEach(my.logSpecFullName);
        failFast.setAfterEach();

        it("- executing of delete project script should remove '"+program_name+"' from the program list", function () {
            browser.wait(deleteGhapProject(program_name.toUpperCase()), 5000, "Delete script not finished in time");
            if (!browser.isSafari) {
                browser.refresh(1000);
            } else {
                // browser.refresh() and browser.get(current_url) do not work in Safari
                // https://github.com/angular/protractor/issues/2111
                // https://code.google.com/p/selenium/issues/detail?id=7176
                browser.getCurrentUrl()
                    .then(function(url){
                        browser.driver.get(cfg.ghapUrl);
                        browser.driver.get(url);
                        browser.waitForAngular();
                    })
            }
            expect(getDisplayedProgramsNames().then(function(programs){
                return (programs.indexOf(program_name) > -1)
            })).toBeFalsy()
        });


        failFast.fulfillDeferSpec();

    });

    return deferred.promise;
}

function deleteGhapProject(project_key) {
    var deferred = protractor.promise.defer();
    var child_process = require('child_process');
    var args = [];
    args.push('../frisby-tests/spec/utils/ghap-delete-project');
    args.push(project_key);
    args.push('--env');
    args.push(cfg.environment);
    var child = child_process.spawn('node', args, {
        stdio: 'inherit'
    }).once('close', function(code, signal){
        var error;
        if (code) {
            error = new Error('Fatal error on delete user.');
            deferred.reject(error);
        } else {
            deferred.fulfill()
        }
    });
    return deferred.promise;
}

function filterPrograms(filter_str) {
    return $('input[ng-model="field"]').sendKeys(filter_str);
};

function getDisplayedProgramsNames(){
    var programs = element.all(by.repeater('program in programs'));
    // programs.count().then(function (count) {
    //     console.log (">>> programs count %d", count)
    // });
    return programs.map(function(program_el){
        return program_el.$('div[ng-click="getGrants(program)"]').getText()
    })
}

function finished() {
    console.log("Program Setup E2E test case finished.")
}