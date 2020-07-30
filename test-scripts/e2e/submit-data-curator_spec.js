/**
 * Created by Vlad on 13.11.2015.
 */

// steps 14-18 og BAP-619
// https://tools.certara.com/jira/browse/BAP-619

// This test does not work in IE
// because I did not find way how to disable question about save file in IE
// https://github.com/angular/protractor/issues/1356
var ghapBrowser = require('./ghap-browsers-capabilities');
if (ghapBrowser.browserName === 'internet explorer') {
    console.warn('This test does not work in the Internet Explorer because I have not found a way how to disable question about save file in IE.');
    return;
}

var fs = require('fs');
var path = require('path');

var my = require('./ghap-e2e-lib');
var cfg = require('./ghap-e2e-config');

var ghapLoginCase = require('./ghap-login-case');
var ghapLogoutCase = require('./ghap-logout-case');
var downloadFileSuite = require('./download-file-suite');

ghapLoginCase.openLoginPageSpec()
    .then(ghapLoginCase.loginAs.bind(this, cfg.curatorName, cfg.curatorPassword))
    .then(validateFileList)
    .then(downloadTstFile)
    .then(deleteTstFile)
    .then(ghapLogoutCase.logOut)
    .thenCatch(my.reportError)
    .thenFinally(finished);

function validateFileList() {

    return my.createStdSuite("Validate file list", function () {

        it('- navigate to SUBMIT DATASET tab', function(){
            var userManagement_pattern = new RegExp(/^Submit[.\s]+Dataset$/i);
            my.getTabLink(userManagement_pattern)
                .then(function(link){ link.click() });
        });

        var tstFile_tr_element;
        it('- list should contains records about ' + cfg.tstFileName + ' file', function(){
            var f_repeater = element.all(by.repeater('f in userData'));
            f_repeater.count().then(function(cnt){
                console.log("File list contains %d items.", cnt);
                expect(cnt).toBeGreaterThan(0);
            });
            getTstFileRowElement()
                .then(function (tr_element) {
                    tstFile_tr_element = tr_element;
                });
        });

        it('- file owner should be ' + cfg.contributorName, function(){
            var f_username = tstFile_tr_element.element(by.binding('f.userName'));
            expect(f_username.getText()).toBe(cfg.contributorName);
        });

        it('- file size should be ' + cfg.tstFileSizeMb + ' MB', function(){
            var f_size = tstFile_tr_element.element(by.binding('f.size'));
            expect(f_size.getText()).toBe(cfg.tstFileSizeMb + ' MB');
        });

    });

}

function downloadTstFile () {

    var it_start_downloading = function() {
        it('- click on link should start file downloading', function(){
            getTstFileRowElement().then(function(tr_el){
                var link = tr_el.$('a');
                expect(link.getAttribute('title')).toMatch(/Download file/i);
                link.click().then(function(){
                    console.log('Download link is clicked.')
                });
            });
        });
    };

    const KB_SIZE = 1024;
    const MB_SIZE = 1024 * KB_SIZE;
    return downloadFileSuite.testSuite("Download test file",it_start_downloading,
        cfg.tstFileName, cfg.tstFileSizeMb * MB_SIZE )

}

function deleteTstFile () {

    return my.createStdSuite("Delete test file", function () {

        var tstFile_tr_element;
        it('- click on delete sign should remove test file from the list', function(){
            getTstFileRowElement().then(function(tr_el){
                tstFile_tr_element = tr_el;
                var link = tr_el.$('td[ng-click="deleteFile(f)"]');
                expect(link.getAttribute('title')).toMatch(/Delete file/i);
                link.click().then(function(){
                    console.log('Delete link is clicked.')
                });
            });
        });

        var max_remove_time_ms = 5 * 1000;
        it('- file should be removed from list within '+max_remove_time_ms/100+'s', function(){
            var start = new Date();
            var EC = protractor.ExpectedConditions;
            browser.wait(EC.stalenessOf(tstFile_tr_element), max_remove_time_ms)
                .then(function(){
                    var remove_time_ms = new Date() - start;
                    console.log('Record is removed within %d s.', remove_time_ms/1000);
                    expect(remove_time_ms).toBeLessThan(max_remove_time_ms);
                    browser.sleep(2000);
                })
        });

    });

}

function getTstFileRowElement() {
    var f_repeater = element.all(by.repeater('f in userData'));
    var name_pattern = new RegExp(cfg.tstFileName);
    return my.getRepeaterLink(f_repeater, name_pattern, cfg.tstFileName + " file not found.")
}

function finished() {
    console.log("\nDownload as Curator the data that were submitted. E2E test case have finished.")
}