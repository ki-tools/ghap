/**
 * Created by Vlad on 16.10.2015.
 */

// implements steps 14-30 of
// https://tools.certara.com/jira/browse/BAP-899

var my = require('./ghap-e2e-lib');
var ctrlFlow = require('./control_flow');
var cfg = require('./ghap-e2e-config');

var ghapLoginCase = require('./ghap-login-case');
var ghapLogoutCase = require('./ghap-logout-case');

ghapLoginCase.openLoginPageSpec()
    .then(ghapLoginCase.loginAs.bind(this, cfg.userName, cfg.userPassword))
    .then(validateProgramsGrantsList)
    .then(validateProgramFilter)
    .then(validateToolTips)
    .then(ghapLogoutCase.logOut)
    .thenCatch(my.reportError)
    .thenFinally(finished);

const T1HashStr = 'T1:G1,G2,G3;';
const T2HashStr = 'T2:G2,G3x;';
const ProgramsDataHashStr = T1HashStr + T2HashStr;

function buildProgramsMaps(programs_el) {
    var programs_maps = [];
    return programs_el.map(function(program_el){
        var entry = {};
        return program_el.getText()
            .then(function(text){
                entry.name = text.split('\n')[0].trim();
            })
            .then(function(){
                return program_el.$('[ng-show="isReadonlyProgram(program)"]')
                    .isDisplayed()
                    .then(function(res){
                        entry.readOnly = res;
                    });
            })
            .then(function(){
                var grants_list = program_el.all(by.repeater('grant in program.grants'));
                var visible_grants = grants_list.filter(function(elem){
                    return elem.isDisplayed();
                });
                return buildGrantsMaps(visible_grants).then(function(grants_map){
                    entry.grants = grants_map;
                    programs_maps.push(entry);
                });
            });
    }).then(function(){
        return programs_maps;
    });
}

function buildGrantsMaps(grants_el){
    var grants_maps = [];
    return grants_el.map(function(grant_el){
        var entry = {};
        return grant_el.$('[ng-mouseover="getChangeInfo(grant)"]')
            .getText()
            .then(function(text){
                entry.name = text.trim();
                entry.grant_el = grant_el;
                return grant_el.$('[ng-show="isReadonlyGrant(grant)"]')
                    .isDisplayed()
                    .then(function(res){
                        entry.readOnly = res;
                    });
            })
            .then(function(){
                grants_maps.push(entry);
            })
    }).then(function(){
        return grants_maps;
    });
}


function ProgramData(){
    this.programs = [];
}

ProgramData.prototype.update = function() {

    var programs_list = element.all(by.repeater('program in programs'));
    programs_list.count().then(function (count) {
        console.log("programs_list.count() %d", count);
    });
    var visible_programs = programs_list.filter(function(elem){
        return elem.isDisplayed();
    });
    visible_programs.count().then(function (count) {
        console.log("visible_programs.count() %d", count);
    });

    var self = this;
    return buildProgramsMaps(visible_programs).then(function(maps){
        self.programs = maps;
        return self;
    });

};

ProgramData.prototype.getHashStr = function(){
    var hash_str = '';
    for( var i=0; i < this.programs.length; i++) {
        var program = this.programs[i];
        hash_str += program.name + (program.readOnly ? 'x:' : ':');
        for( var j=0; j < program.grants.length; j++) {
            var grant = program.grants[j];
            hash_str += grant.name + (grant.readOnly ? 'x' : '');
            if (j+1 < program.grants.length) hash_str += ',';
        }
        hash_str += ';'
    }
    return hash_str;
};

function validateProgramsGrantsList(){

    var deferred = protractor.promise.defer();
    var failFast = my.createFailFast(deferred);

    describe("Check Programs/Grants list", function() {
        beforeEach(my.logSpecFullName);
        failFast.setAfterEach();

        var program_data;
        it('- validate programs/grants names and attributes', function(){

            program_data = new ProgramData();
            program_data.update().then(function(){
                var hash_str = program_data.getHashStr();
                console.log("Program Data hash str: '%s'", hash_str);
                expect(hash_str).toBe(ProgramsDataHashStr)
            });

        });

        // see implementation of copyTextToClipboard
        // in web-frontend\app\scripts\directives\workspace.js
        // look also here http://help.dottoro.com/ljinpbdi.php
        // look also http://www.w3.org/TR/clipboard-apis/
        //
        // Q: javascript get text in clipboard
        // A: http://stackoverflow.com/questions/6413036/get-current-clipboard-content
        // see also http://stackoverflow.com/questions/233719/is-it-possible-to-read-the-clipboard-in-firefox-safari-and-chrome-using-javascr
        // Q: clipboarddata chrome
        // A: http://stackoverflow.com/questions/20509061/window-clipboarddata-getdatatext-doesnt-work-in-chrome

        var addCopyLitener = function(){
            window.myCopyListener = function (evt) {
                window.lastCopyValue = document.getElementsByTagName('textArea')[0].value;
            };
            document.addEventListener('copy', window.myCopyListener);
        };

        var removeCopyListener = function(){
            document.removeEventListener('copy', window.myCopyListener);
        };

        function getCopyUrl(){

            if (browser.isInternetExplorer)
                return browser.executeScript("return  window.clipboardData.getData('Text');");

            if (browser.isChrome)
                return browser.executeScript("return window.lastCopyValue;");

            if (browser.isSafari)
                return browser.executeScript("return 'SafariDriver cannot handle alerts';");

            var EC = protractor.ExpectedConditions;
            var alert_promise;
            var alert_text;
            return browser.wait(EC.alertIsPresent(), 500)
                .then(function() {

                    // i can not get prompt text in protractor
                    // http://stackoverflow.com/questions/32106457/how-to-get-javascript-prompt-value-with-protractor

                    // and can not send ctr+C while prompt is not accepted
                    // alert.sendKeys(protractor.Key.CONTROL, "C", protractor.Key.NULL);  -- DO NOT WORK send ctr+c key code to prompt
                    // browser.actions().keyDown(protractor.Key.CONTROL).sendKeys('').perform();  - cause unhandled alert ERROR

                    return browser.switchTo().alert()
                })
                .then(function(alert){
                    alert_promise = alert;
                    return alert.getText()
                })
                .then(function(text){
                    alert_text = text;
                    return alert_promise.accept()
                })
                .then(function(){
                    return alert_text
                })
        }

        function validateCopyUrl(program_map, grant_map, cb){
            grant_map.grant_el.$('.cloneUrl').click()
                .then(getCopyUrl)
                .then(function(copy_url){

                    var pattern_str;
                    if (browser.isSafari) {
                        pattern_str = 'SafariDriver cannot handle alerts$';
                    } else if (browser.isChrome || browser.isInternetExplorer ) {
                        pattern_str = "https?:\\/\\/" + cfg.userName + "@git\\..{0,4}ghap.io\\/stash\\/scm\\/"
                            + program_map.name.toLowerCase() + "\\/" + grant_map.name.toLowerCase() +"\\.git";
                    } else {
                        pattern_str = 'Press .+ to copy the text below$';
                    }

                    console.log("%s/%s copyUrl is '%s'",program_map.name, grant_map.name, copy_url);
                    // console.log(pattern_str);
                    var pattern = new RegExp(pattern_str);

                    expect(copy_url).toMatch(pattern);
                })
                .then(cb)
        }

        it('- validate copy url links against RegExp patterns', function(){

            browser.executeScript('('+addCopyLitener.toString()+')();')
                .then(function(){
                    return my.preventAlertErrorOnSafari();
                })
                .then(function(){
                    var calls=[];
                    program_data.programs.forEach(function(program_map){
                        program_map.grants.forEach(function(grant_map){
                            calls.push(
                                function(next) {validateCopyUrl(program_map, grant_map, next)}
                            );
                        });
                    });
                    ctrlFlow.series(calls, function(){
                        browser.executeScript('('+removeCopyListener.toString()+')();');
                    });
                })

        });

        failFast.fulfillDeferSpec();

    });

    return deferred.promise;

}

function validateProgramFilter(){

    var deferred = protractor.promise.defer();
    var failFast = my.createFailFast(deferred);

    describe("Check programs filtering", function() {

        beforeEach(my.logSpecFullName);
        failFast.setAfterEach();

        var program_data;
        it('- validate programs list for T1 filter', function(){

            //var filter_input = $('.field input');
            var filter_input = element(by.model('field'));
            filter_input.clear().sendKeys('T1');

            program_data = new ProgramData();
            program_data.update().then(function(){
                var hash_str = program_data.getHashStr();
                console.log("Program Data hash str: '%s'", hash_str);
                expect(hash_str).toBe(T1HashStr)
            });

        });

        it('- clear filter and validate programs list', function(){
            $(".filter img").click();
            program_data.update().then(function(){
                var hash_str = program_data.getHashStr();
                console.log("Program Data hash str: '%s'", hash_str);
                expect(hash_str).toBe(ProgramsDataHashStr)
            });

        });

        failFast.fulfillDeferSpec();

    });

    return deferred.promise;

}

function validateToolTips(){

    var deferred = protractor.promise.defer();
    var failFast = my.createFailFast(deferred);

    describe("Check tooltips", function() {

        beforeEach(my.logSpecFullName);
        failFast.setAfterEach();

        var program_data;

        it('- point to T1/G1 grant name; last commit tooltip should be displayed', function(){
            // Q: protractor move mouse to element
            // A: https://github.com/angular/protractor/issues/159
            //
            // Q: protractor mouse over element IE
            // http://stackoverflow.com/questions/32609491/protractor-testing-ui-bootstrap-tooltip-on-ie

            program_data = new ProgramData();
            program_data.update()
                .then(function(){
                    //return program_data.programs[0].grants[0].grant_el.$('li div div');
                    return program_data.programs[0].grants[0].grant_el.$('[popover-title="Last commit"]');
                })
                .then(function(div){
                    if (browser.isSafari)
                        return div.click();
                    else {
                        return browser.actions().mouseMove(div).perform();
                    }
                })
                .then(function(){
                    return browser.sleep(2000);
                })
                .then(function(){
                    // this artificial call required for IE
                    // NOTE: physical cursor pointer should be placed out of browser window.
                    // see https://github.com/SeleniumHQ/selenium/wiki/InternetExplorerDriver#hovering-over-elements
                    return browser.waitForAngular()
                })
                .then(function(){
                    //return element(by.css('[class="popover ng-isolate-scope left fade in"]'));
                    return $('.popover');
                })
                .then(function (pop_el) {
                    pop_el.getAttribute('title').then(function (text) {
                        expect(text).toContain('Last commit');
                    });
                    pop_el.getText().then(function (text) {
                        console.log("Tooltip: '%s'", text);
                    });
                });

        });

        it('- validate copyUrl link tooltip', function () {
            var clone_link = program_data.programs[0].grants[0].grant_el.$('.cloneUrl');
            clone_link.getAttribute('title').then(function (text) {
                console.log("Tooltip: '%s'", text);
                expect(text).toContain('for cloning');
            });
        });

        failFast.fulfillDeferSpec();

    });

    return deferred.promise;

}


function finished(){
    console.log("E2E test case finished.")
}