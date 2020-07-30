/**
 * Created by Vlad on 15.12.2015.
 */

var Q = require('q');
var fs = require('fs');
var mod_path = require('path');

var my = require('../Common/ghap-lib');
my.stepPrefix = 'UserData';
my.logModuleName(module.filename);

var umsRequests = require('../ums/ums_requests');
var umsUser = require('../ums/ums_user');
var Tester;

var logRequests = require("../le/log-event_requests");

var umsRole = require('../ums/ums_role');
var roleRequests = require('../ums/ums_role_crud');
var allRoles = [];
var dataCuratorRole = null;

var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./../oauth/oauth_promise');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
    .then(function(){
        testerAdmin.setAuthHeader(oAuth.header);
    })
    .then(runSuite)
    .catch(my.reportError)
    .finally(finalCase);

var udsRequests = require('./uds_requests');

var testFileName = 'logo-frisby.png';
var startDownloadTime;

function runSuite() {
    return createTester()
        .then(createFolder)
        .then(uploadFile)
        .then(function () {
            startDownloadTime = new Date();
        })
        .then(downloadFile)
        .then(function () {
            return validateLog(startDownloadTime, Tester.getName(), testFileName)
        })
        .then(deleteFolderTest)
        .catch(function () {
            return udsRequests.deleteFile(oAuth.header, Tester, '', 'T/')
        })
}

function createTester() {
    Tester = umsUser.makeUser('vlad.ruzov.uds.test');
    var new_password = "";
    return roleRequests.getAllRoles(testerAdmin.authHeader, allRoles)
        .then(findDataCuratorRole)
        .then(function(){
            return umsRequests.createUser(testerAdmin.authHeader, Tester)
                .then(function(){
                    return roleRequests.setRoleToUser(testerAdmin.authHeader, Tester, dataCuratorRole)
                })
        })
        .then(function(){
            console.log();
            return oAuth.login(Tester.getName(), Tester.getPassword())
                .then(function(){
                    return umsRequests.resetUserPassword(oAuth.header, Tester, new_password)
                })
                .then(function(){
                    Tester.setPassword(new_password);
                    console.log();
                    return oAuth.login(Tester.getName(), Tester.getPassword());
                })
        })
}

function findDataCuratorRole(){
    var deferred = Q.defer();
    describe('Check if Data Curator role defined.', function(){
        dataCuratorRole = umsRole.findRoleByName(allRoles, 'Data Curator');
        it('The role should be present in allRoles array', function(){
            console.log(my.endOfLine + this.getFullName()+' [' + my.stepNum++ + ']');
            expect(dataCuratorRole).not.toBeNull();
            if (dataCuratorRole) deferred.resolve(); else deferred.reject();
            waits(200); // make a delay to allow promise to be handled
        });
    });
    return deferred.promise;
}

function createFolder() {
    return udsRequests.createFolder(oAuth.header, Tester, 'T')
        .then(function(status){
            expect(status).toBe(200);
            if (status == 409)
                console.log("Folder 'T' already exists in user workspace");
            return udsRequests.dir(oAuth.header, Tester, '');
        })
        .then(function(data_array){
            expectDataList(data_array,[{ path: '', name: 'T', isDirectory: true }]);
        })
}

function deleteFolderTest() {
    return udsRequests.deleteFile(oAuth.header, Tester, '', 'T/')
        .then(function(){
            return udsRequests.dir(oAuth.header, Tester, '');
        })
        .then(function(data_array){
            expectDataList(data_array,[]);
        })
}

function expectDataList(data_array, expected_array) {
    console.log(' Validate data list.');
    //console.log(data_array);
    expect(data_array).not.toBeNull();
    if (data_array) {
        expect(data_array.length).toBe(expected_array.length);
        for(var i=0; i<data_array.length; i++) {
            expect(data_array[i]).toContainJson(expected_array[i]);
        }
    } else {
        console.log('ERROR: data list not defined.')
    }
}

function uploadFile() {
    var testFilePath = mod_path.resolve(__dirname, testFileName);
    return udsRequests.uploadFile(oAuth.header, Tester, 'T', testFilePath)
        .then(function (status) {
            expect(status).toBe(200);
            if (status == 409)
                console.log("File '%s' already exists in folder 'T'", testFileName);
        })
}

function downloadFile() {
    return udsRequests.downloadFile(oAuth, Tester, 'T', testFileName)
        .then(function(data) {
            var logoPath = mod_path.resolve(__dirname, testFileName);
            var logo_file_size = fs.statSync(logoPath).size;
            console.log(" %d bytes downloaded.", data.length);
            expect(data.length).toBe(logo_file_size);
        })
}

function validateLog(from, username, filename) {

    // skip test if elasticsearch url is not defined in current environment
    var cfg = require('./../Common/ghap-config');
    if (cfg.awsEsUrl == "none") return Q();

    // subtract 3 min. to adjust time difference between CI server and AWS
    from -=  3*60*1000;
    var es_client;
    return logRequests.getAwsEsClient()
        .then(function (client) {
            es_client = client;
            return logRequests.flush(client);
        })
        .then(function () {
            return logRequests.getUwsLogs(es_client, from);
        })
        .then(function (response) {
            return logRequests.validateLog(response, username, filename)
        })
}

function finalCase() {
    return umsRequests.deleteUser(testerAdmin.authHeader, Tester)
        .then(my.finalTest);
}
