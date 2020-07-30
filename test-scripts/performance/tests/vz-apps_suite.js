/**
 * Created by Vlad on 29.08.2016.
 */

var assert = require('chai').assert;

var Q = require('q');

var my = require('./../../frisby-tests/spec/Common/ghap-lib');
var report2csv = require('./report2csv');

var ghapRq = require('./../../frisby-tests/spec/utils/ghap-requests');
ghapRq.sendCorsRequests = true;

var tester;

describe("Vz View Apps page", function () {
    var start_time_ms;

    before(function (done) {
        tester = require("./test_user").getTester();

        if (!tester.authHeader.Value) {
            my.log("\nVisualization - View Applications page are skipped due authorization issue.");
            report2csv.appendLine('Visualization - View Applications page tests skipped (auth issue)', 0);
            this.skip();
            return done(new Error("Authorization was failed."));
        }
        else {
            my.log("\nVisualization - View Applications page tests are started.");
            done();
        }
    });

    before(function () {
        ghapRq.resetRequestsCount();
        start_time_ms = new Date();
    });

    after(function () {
        var exec_time_ms = new Date() - start_time_ms;
        my.log("\nVisualization - View Applications page statistic:");
        my.log("%d http requests have been processed;", ghapRq.getRequestsCount());
        my.log("total processing time is %s", my.logTime(exec_time_ms));
        report2csv.appendLine('Visualization - View applications page load time (ms)', exec_time_ms);
    });

        it("get published application list", function () {
            return assert.isFulfilled(getApps())
        });

});

function getApps() {
    return ghapRq.getRegisteredApps(tester.authHeader)
}
