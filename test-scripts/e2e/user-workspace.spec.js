/**
 * Created by Vlad on 10.02.2016.
 */

var fs = require('fs');
var my = require('./ghap-e2e-lib');
var cfg = require('./ghap-e2e-config');
var tstFile = require('./tstfile.js').createNew(cfg.tstFileName, 10);
var downloadFileSuite = require('./download-file-suite');

var ghapBrowser = require('./ghap-browsers-capabilities');
if (ghapBrowser.browserName === 'internet explorer') {
    console.warn('This test fails in the Internet Explorer due issue with file upload using protractor.');
    return;
}

var ghapLoginCase = require('./ghap-login-case');
var ghapLogoutCase = require('./ghap-logout-case');

ghapLoginCase.openLoginPageSpec()
    .then(ghapLoginCase.loginAs.bind(this, cfg.userName, cfg.userPassword))
    .then(loadUserWorkspaceElements)
    .then(uploadTstFile)
    .then(downloadTstFile)
    .then(deleteTstFile)
    .then(createFolder)
    .then(deleteFolder)
    .then(ghapLogoutCase.logOut)
    .thenCatch(my.reportError)
    .thenFinally(finished);

// user workspace meta object
var ws = {};
var newFolderName = 'TTT';

function loadUserWorkspaceElements() {

    return my.createStdSuite("Load User Workspace Elements", function() {

        it("- header 'TEMPORARY WORKSPACE' should exist", function(){
            var workspace_header = $$('workspace h4').first();
            expect(workspace_header.getText()).toBe('TEMPORARY WORKSPACE')
        });

        it("- breadcrumb link 'Home', 'New Folder' and 'Upload' buttons, 'Actions' drop-down list should exist", function(){
            ws.workspace_area = $('workspace div.grid-col.col-8-12.separator-padded-right');

            ws.breadcrumb_links = ws.workspace_area.$$('ol.breadcrumb a');
            expect(ws.breadcrumb_links.count()).toBeGreaterThan(0);
            expect(ws.breadcrumb_links.first().getText()).toBe('Home');

            ws.input_uploader = ws.workspace_area.$('[uploader=uploader]');

            ws.upload_button = ws.input_uploader.element(by.xpath('..')).$('button');
            expect(ws.upload_button.getText()).toBe('Upload');

            ws.new_folder_button = ws.workspace_area.$('button[ng-click="toggleNewFolderDialog()"]');
            expect(ws.new_folder_button.getText()).toBe('New Folder');

            ws.actions_select = ws.workspace_area.$('select');
            expect(ws.actions_select.getText()).toContain('Actions');
        });

    });
}

function getWorkSpaceFileRow(file_name) {
    var f_rows = ws.workspace_area.all(by.repeater('f in userData'));
    var name_pattern = new RegExp(file_name);
    return my.getRepeaterLink(f_rows, name_pattern, file_name + " not found in the user workspace files list.")
}

function uploadTstFile() {

    return my.createStdSuite("Upload TstFile", function() {

        it("- create file if it does'nt exist or has wrong size", function(){
            tstFile.prepareTstFile()
        });

        it("- start file uploading", function(){
            var stat = fs.statSync(tstFile.fullFileName);
            expect(stat.isFile()).toBe(true);

            ws.input_uploader.sendKeys(tstFile.fullFileName);

        });

        var upload_timeout = tstFile.fileSizeMb * 10 * 1000;
        it('- upload should finish within ' + upload_timeout / 1000 + ' s', function () {
            var start = new Date();
            var uploading_msg_div = ws.workspace_area.$('div.uploading-message');
            /**
             * @typedef {ExpectedConditions} protractor.ExpectedConditions
             * @type {ExpectedConditions}
             */
            var EC = protractor.ExpectedConditions;
            //browser.wait(EC.visibilityOf(uploading_msg_div), 200);
            browser.wait(EC.invisibilityOf(uploading_msg_div), upload_timeout-250);
            var upload_time = new Date() - start;
            expect(upload_time).toBeLessThan(upload_timeout);

        }, upload_timeout + 1000);

        it('- '+tstFile.fileName+' file should be in the user workspace files list', function() {
            getWorkSpaceFileRow(tstFile.fileName)
        });

    });
}

function downloadTstFile() {

    var it_start = function() {
        it("- select file in the list should enable 'Actions' drop-down ", function(){
            getWorkSpaceFileRow(tstFile.fileName)
                .then(function(tr){
                    tr.$('input').click();
                    expect(ws.actions_select.isEnabled()).toBeTruthy();
                })
        });

        it("- click on 'Download' option should show start downloading", function () {
            ws.actions_select.click();
            ws.actions_select.$('option[label=Download]').click();
        });
    };

    return downloadFileSuite.testSuite('Download Test File', it_start, tstFile.fileName, tstFile.fileSize);
}

function deleteTstFile() {

    return my.createStdSuite("Delete TstFile",
        it_deleteWorkspaceFile.bind(null, tstFile.fileName)
    );
}

function it_deleteWorkspaceFile(file_name) {

    var file_tr;
    it("- '"+file_name+"' file should be in the user workspace files list", function() {
        getWorkSpaceFileRow(file_name)
            .then(function (table_row) {
                file_tr = table_row;
            });
    });

    it("- select of '"+file_name+"' file should enable 'Actions' the drop-down list", function () {
        file_tr.$('input').isSelected()
            .then(function(is_selected){
                if (!is_selected) file_tr.$('input').click();
                var EC = protractor.ExpectedConditions;
                browser.wait(EC.elementToBeClickable(ws.actions_select), 1000);
            });
    });

    var modal_dialog;
    it("- click on 'Delete' option should show confirmation dialog", function () {
        ws.actions_select.$('option[label=Delete]').click();
        modal_dialog = $('div.modal-dialog');
        expect(modal_dialog.isDisplayed()).toBeTruthy();
        expect(modal_dialog.$('button.default-button').getText()).toBe('CANCEL');
        expect(modal_dialog.$('button.submit-button').getText()).toBe('DELETE');
    });

    it("- click on 'Delete' button should close modal dialog and delete file from the list", function () {
        modal_dialog.$('button.submit-button').click();
        var EC = protractor.ExpectedConditions;
        browser.wait(EC.stalenessOf(modal_dialog), 200);
        browser.wait(EC.stalenessOf(file_tr), 2000);
    });

}

function createFolder() {

    return my.createStdSuite("Create new folder", function() {

        var folder_name_input;
        it("- click on 'New Folder' button should show input popup", function(){
            folder_name_input = element(by.model('newFolderName'));
            expect(folder_name_input.isDisplayed()).toBeFalsy();
            ws.new_folder_button.click();
            expect(folder_name_input.isDisplayed()).toBeTruthy();
        });

        it("- input new folder name + ENTER should create folder in the files list", function(){
            folder_name_input.sendKeys(newFolderName);
            browser.actions().sendKeys(protractor.Key.ENTER).perform();

            var f_rows = ws.workspace_area.all(by.repeater('f in userData'));
            var name_pattern = new RegExp(newFolderName);
            my.getRepeaterLink(f_rows, name_pattern, newFolderName + " folder not found in the list.")
                .then(function (table_row) {
                    ws.new_folder_tr = table_row;
                    expect(table_row.$('td.table-col-type').getText()).toBe('Folder');
                });
        });

        it("- click on new folder name should change breadcrumb links and show empty files list", function(){
            var new_folder_link = ws.new_folder_tr.element(by.binding('f.name'));
            expect(new_folder_link.getText()).toBe(newFolderName);
            new_folder_link.click();
            ws.breadcrumb_links.count()
                .then(function(count){
                    expect(count).toBe(2);
                    if (count === 2) {
                        expect(ws.breadcrumb_links.get(1).getText()).toBe(newFolderName);
                    }
                });
            var f_rows = ws.workspace_area.all(by.repeater('f in userData'));
            expect(f_rows.count()).toBe(0);

        });

    });
}

function deleteFolder() {

    return my.createStdSuite("Delete folder", function() {

        it("- click on 'Home' should list root folder", function(){
            ws.breadcrumb_links.first().click();
            expect(ws.breadcrumb_links.count()).toBe(1);
        });

        it_deleteWorkspaceFile(newFolderName);

    });
}

function finished(){
    console.log("\nUser Workspace E2E test case have finished.")
}