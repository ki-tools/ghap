/**
 * Created by Vlad on 26.05.2016.
 */

var Q = require('q');
var path = require('path');
var fs = require('fs');

var my = require('../Common/ghap-lib');
my.stepPrefix = 'DataSbmt';
my.logModuleName(module.filename);

var umsRequests = require('../ums/ums_requests');
var logRequests = require("../le/log-event_requests");

var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./../oauth/oauth_promise');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
    .then(umsRequests.validateToken)
    .then(runSuite)
    .catch(my.reportError)
    .finally(my.finalTest);

var dsRequests = require('./ds_requests');

var logoFileName = 'logo-frisby.png';
var logoPath = path.resolve(__dirname, logoFileName);
var testFileName;
var testFilePath;

function runSuite() {
    return Q()
        .then(duplicateLogo)
        .then(uploadTestFile)
        .then(downloadTestFile)
        .then(function (download_start) {
            return validateLog(download_start, oAuth.username, testFileName)
        })
        .then(uploadExistingFile)
        .then(deleteTestFile)
}

function duplicateLogo() {
    testFileName = "logo-" + my.dateTimeStr(new Date()) + ".png";
    testFilePath = path.resolve(__dirname, testFileName);
    return my.copyFile(logoPath, testFilePath);
}

function uploadTestFile() {
    return dsRequests.uploadFile(oAuth.header, testFilePath)
        .then(function (status) {
            expect(status).toBe(200);
            if (status != 200)
                console.error("Wrong response with status code %d", status)
        })
}

function uploadExistingFile() {
    return dsRequests.uploadFile(oAuth.header, testFilePath)
        .then(function (status) {
            expect(status).toBe(409);
            if (status != 409)
                console.error("Wrong response with status code %d", status)
        })
}

function downloadTestFile() {
    var download_start = Date.now();
    return dsRequests.downloadFile(oAuth, testFileName)
        .then(function(data) {
            var logo_file_size = fs.statSync(logoPath).size;
            console.log(" %d bytes downloaded.", data.length);
            expect(data.length).toBe(logo_file_size);
            return download_start;
        })
}

function deleteTestFile() {
    var deferred = Q.defer();
    dsRequests.deleteFile(oAuth.header, testFileName)
        .finally(function () {
            fs.unlink(testFilePath, function (err) {
                if (err) throw new Error(err);
                deferred.resolve();
                waits(200);
            })
        });
    return deferred.promise;
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
            return logRequests.getDsLogs(es_client, from);
        })
        .then(function (response) {
            return logRequests.validateLog(response, username, filename)
        })
}