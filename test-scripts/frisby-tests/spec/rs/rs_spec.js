/**
 * Created by Vlad on 15.01.2016.
 */

var Q = require('q');

var my = require('../Common/ghap-lib');
my.stepPrefix = 'RS';
my.logModuleName(module.filename);

var rsRequests = require('./rs_requests');
var userAccountsReport;
var downloadLogReport;

var umsRequests = require('../ums/ums_requests');

var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./../oauth/oauth_promise');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
    .then(function() {
        return umsRequests.pullUser(oAuth, testerAdmin)
            .then(function () {
                console.log(" user GUID '%s'", testerAdmin.getGuid())
            })
    })
    .then(runSuite)
    .catch(my.reportError)
    .finally(my.finalTest);

function runSuite() {
    return rsRequests.getAvailableReports(oAuth.header)
        .then(validateAvailableReports)
        .then(createUserAccountsReport)
        .then(waitUserAccountsReportCompleted)
        .then(checkReportsStatuses)
        .then(validateUserAccountsReportContent)
        .then(deleteUserAccountsReport)
        .then(createDownloadLogReport)
        .then(waitDownloadLogReportCompleted)
        .then(validateDownloadLogReportContent)
        .then(deleteDownloadLogReport)
}

function validateAvailableReports(available_reports) {
    console.log("\n%d reports are available.",available_reports.length);
    expect(available_reports.length).toBeGreaterThan(7);
    userAccountsReport = my.findElementInArray(available_reports,'type','USER_STATUS');
    expect(userAccountsReport).not.toBeNull();
    downloadLogReport = my.findElementInArray(available_reports,'type','DATASUBMISSION');
    expect(downloadLogReport).not.toBeNull();
    expect(downloadLogReport.constraintTypes).toContain('DATE_RANGE');
}

function createUserAccountsReport() {
    return rsRequests.createReport(oAuth.header, testerAdmin, userAccountsReport)
        .then(function(token){
            userAccountsReport.token = token;
            return rsRequests.getUserReports(oAuth.header, testerAdmin)
        })
        .then(function(user_reports){
            console.log(' %d reports received.', user_reports.length);
            expect(user_reports.length).toBeGreaterThan(0);
            var report = my.findElementInArray(user_reports, 'token', userAccountsReport.token);
            expect(report).not.toBeNull();
            if (report) {
                expect(report.owner).toBe(testerAdmin.getGuid());
            }
        })
}

function waitUserAccountsReportCompleted() {
    return waitReportCompleted(userAccountsReport.token);
}

function waitDownloadLogReportCompleted() {
    return waitReportCompleted(downloadLogReport.token);
}

function waitReportCompleted(report_token) {
    var deferred = Q.defer();

    var max_exec_time_ms = 60 * 1000;
    var delay_between_attempts_ms = 10 * 1000;
    var start_time_ms = new Date();
    var exec_time_ms;

    function is_completed(status) {
        return (status === 'COMPLETE');
    }

    function check() {
        rsRequests.getReportStatus(oAuth.header, report_token)
            .then(function (status) {
                exec_time_ms = new Date() - start_time_ms;
                if (is_completed(status)) {
                    console.log("\nReport build is completed within %s.", my.logTime(exec_time_ms));
                    deferred.resolve();
                } else {
                    console.log("\nWaiting time is %s", my.logTime(exec_time_ms));
                    if (exec_time_ms < max_exec_time_ms) {
                        my.pauseJasmine(delay_between_attempts_ms);
                        check();
                    } else {
                        console.error("The report building time exceeded %d minutes", max_exec_time_ms / 1000 / 60);
                        deferred.reject(new Error('Building of report timeout.'));
                    }
                }
            })
            .catch(my.reportError)
    }

    check();

    return deferred.promise;

}

function checkReportsStatuses() {
    var tokens = [userAccountsReport.token,'fake-id'];
    return rsRequests.getStatuses(oAuth.header, tokens)
        .then(function(statuses){
            //var tokens = token_list.split(',');
            expect(tokens.length).toBe(statuses.length);
            console.log(" %d statuses received.", statuses.length);
            for(var index=0; index < statuses.length; index++) {
                console.log("'%s' - '%s'",tokens[index],statuses[index])
            }
            expect(statuses[0]).toBe('COMPLETE');
            expect(statuses[1]).toBe('NOT_FOUND');
        })
}

function validateUserAccountsReportContent() {
    var all_users = [];
    return umsRequests.getAllUsers(oAuth.header, testerAdmin.getParentDn(),all_users)
        .then(function(){
            return rsRequests.getReportContent(oAuth, userAccountsReport.token)
        })
        .then(function(content) {
            console.log(" %d characters received.", content.length);
            var accounts = content.split(/\r\n|\r|\n/)
                .filter(function(value, index){
                    if (index === 0) return false;
                    if (value) return value;
                });
            console.log('  Included to the report: %d users', accounts.length);
            console.log('Registered in the system: %d users', all_users.length);
            expect(accounts.length).toBe(all_users.length)
        })
}

function deleteUserAccountsReport() {
    return rsRequests.deleteReport(oAuth.header, userAccountsReport.token)
        .then(function(){
            console.log(' Last created User Accounts report has been deleted.')
        })
}

/*------------------- Download Log Report ------------------------------------------*/

function createDownloadLogReport() {
    var constrains = [{type: "DATE_RANGE", constraint: {start: "2016-06-01", end: "2016-06-10"}}];
    return rsRequests.createConstrainedReport(oAuth.header, testerAdmin, downloadLogReport, constrains)
        .then(function(token){
            downloadLogReport.token = token;
            return rsRequests.getUserReports(oAuth.header, testerAdmin)
        })
        .then(function(user_reports){
            console.log(' %d reports received.', user_reports.length);
            expect(user_reports.length).toBeGreaterThan(0);
            var report = my.findElementInArray(user_reports, 'token', downloadLogReport.token);
            expect(report).not.toBeNull();
            if (report) {
                expect(report.owner).toBe(testerAdmin.getGuid());
            }
        })
}

function validateDownloadLogReportContent() {
    return rsRequests.getReportContent(oAuth, downloadLogReport.token)
        .then(function(content) {
            console.log(" %d characters received.", content.length);
            var lines = content.split(/\r\n|\r|\n/)
            console.log('  Lines included to the report: %d', lines.length);
            expect(lines.length).toBeGreaterThan(1);

        })
}

function deleteDownloadLogReport() {
    return rsRequests.deleteReport(oAuth.header, downloadLogReport.token)
        .then(function(){
            console.log(' Last created Dataset Download Log report has been deleted.')
        })
}

