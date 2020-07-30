/**
 * Created by Vlad on 03.11.2015.
 */

// steps 1-12 og BAP-619
// https://tools.certara.com/jira/browse/BAP-619

var fs = require('fs');

var my = require('./ghap-e2e-lib');
var cfg = require('./ghap-e2e-config');
var tstFile = require('./tstfile.js').createNew(cfg.tstFileName, cfg.tstFileSizeMb);

var ghapLoginCase = require('./ghap-login-case');
var ghapLogoutCase = require('./ghap-logout-case');

ghapLoginCase.openLoginPageSpec()
    .then(ghapLoginCase.loginAs.bind(this, cfg.contributorName, cfg.contributorPassword))
    .then(selectFiles)
    .then(ghapLogoutCase.logOut)
    .thenCatch(my.reportError)
    .thenFinally(finished);

function selectFiles() {

    return my.createStdSuite("Select files", function () {

        var datasubmit_form;
        it('- validate form', function () {
            datasubmit_form = {
                input_checkbox: element(by.model('enableUploader')),
                submit_div: $('div.submit-button'),
                input_uploader: $('[uploader=uploader]'),
                error_div: $('div .error-message-cont')
            };
            my.validateFields(datasubmit_form);
            expect(datasubmit_form.error_div.isDisplayed()).toBe(false);
        });

        it('- click on submit button, expect error message appears', function () {
            datasubmit_form.submit_div.click();
            expect(datasubmit_form.error_div.isDisplayed()).toBe(true);
        });

        it('- click on check box, expect error message will be hidden', function () {
            datasubmit_form.input_checkbox.click();
            expect(datasubmit_form.error_div.isDisplayed()).toBe(false);
        });

        it("- create file if it does'nt exist or has wrong size", function () {
            tstFile.prepareTstFile()
                .then(function () {
                    expect(tstFile.fileSizeMb).toBe(cfg.tstFileSizeMb)
                })
        });

        // Q: protractor test file upload
        // http://stackoverflow.com/questions/21305298/how-to-upload-file-in-angularjs-e2e-protractor-testing
        it('- start file uploading', function () {

            // http://stackoverflow.com/questions/4482686/check-synchronously-if-file-directory-exists-in-node-js
            var stat = fs.statSync(tstFile.fullFileName);
            expect(stat.isFile()).toBe(true);

            datasubmit_form.input_uploader.sendKeys(tstFile.fullFileName);

            safariFileUploadHelper(15000);
        });

        var upload_timeout = tstFile.fileSizeMb * 10 * 1000;
        it('- upload should finish within ' + upload_timeout / 1000 + ' s', function () {
            var start = new Date();
            var progress_div = $('div.progress');
            // expect(progress_div.isDisplayed()).toBe(true);
            // in the Firefox progress bar use setTimeout calls
            // so, while bar is in progress angular qualifies page state as loading mode
            // browser.waitForAngular();
            var EC = protractor.ExpectedConditions;
            browser.wait(EC.stalenessOf(progress_div), upload_timeout);
            var upload_time = new Date() - start;
            expect(upload_time).toBeLessThan(upload_timeout);

        }, upload_timeout + 1000);

        it("- 'upload completed successfully' message should be displayed.", function () {
            var msg_el = $('h3.color-primary.upload-message');
            msg_el.isPresent().then(function (res) {
                if (!res) console.log('ERROR: no successful result message is displayed.');
                expect(res).toBeTruthy();
            });
            msg_el.getText().then(my.consoleLog);
            expect($('img.checkmark').isDisplayed()).toBe(true);
        });

        var select_file_time = 0;
        it("- try upload file again, error message should be displayed.", function () {
            tstFile.fileSizeMb = 0.1;
            browser.wait(tstFile.prepareTstFile(), 5000)
                .then(function () {
                    datasubmit_form.input_uploader.sendKeys(tstFile.fullFileName);

                    select_file_time = safariFileUploadHelper(15000) + 1000;

                    var progress_div = $('div .progress');
                    var EC = protractor.ExpectedConditions;
                    browser.wait(EC.stalenessOf(progress_div), upload_timeout);
                    var err_el = $('h3.error-message-cont.upload-message');
                    err_el.isPresent().then(function (res) {
                        if (!res) console.log('ERROR: No error message displayed.');
                        expect(res).toBe(true);
                    });
                    err_el.getText()
                        .then(function (text) {
                            console.log(text);
                            expect(text).toContain('is already exists');
                        });
                })
        }, upload_timeout + select_file_time);

    });
}

function safariFileUploadHelper(delay_ms) {
    // http://stackoverflow.com/questions/24247860/select-file-in-safari-in-protractor
    // The SafariDriver does not support file uploads
    // https://code.google.com/p/selenium/issues/detail?id=4220
    // with Safari webdriver 2.45 it is possible to choice file manually
    if (browser.isSafari) {
        console.log("File upload does not work in safari."+
            " Please upload '%s' file manually by clicking on 'SELECT FILES' button.", tstFile.fullFileName);
        browser.sleep(delay_ms);  // to allow choice file manually
        return delay_ms
    }
    return 0
}

function finished() {
    console.log("\nSubmit data by contributor E2E test case have finished.")
}