/**
 * Created by Vlad on 18.07.2016.
 */

var assert = require('chai').assert;
var parallel = require('mocha.parallel');

var Q = require('q');

var my = require('./../../frisby-tests/spec/Common/ghap-lib');
var report2csv = require('./report2csv');

var ghapRq = require('./../../frisby-tests/spec/utils/ghap-requests');
ghapRq.sendCorsRequests = true;

var tester;

describe("Computing Environments page", function () {
    this.timeout(10000);
    var start_time_ms;

    before(function (done) {
        tester = require("./test_user").getTester();

        if (!tester.authHeader.Value) {
            my.log("\nComputing Environments page tests are skipped due authorization issue.");
            report2csv.appendLine('Computing Environments page tests skipped (auth issue)', 0);
            this.skip();
            return done(new Error("Authorization was failed."));
        } else {
            my.log("\nComputing Environments page tests are started.");
            done();
        }
    });

    before(function () {
        ghapRq.resetRequestsCount();
        start_time_ms = new Date();
    });

    after(function () {
        var exec_time_ms = new Date() - start_time_ms;
        my.log("\nComputing Environments page load statistic:");
        my.log("%d http requests have been processed;", ghapRq.getRequestsCount());
        my.log("total processing time is %s", my.logTime(exec_time_ms));
        report2csv.appendLine('Computing Environments page load time (ms)', exec_time_ms);
    });

    parallel("simultaneous requests", function () {
        it("get user roles and activities", function () {
            return assert.isFulfilled(getActivities())
        });

        it("get environments", function () {
            return assert.isFulfilled(getEnvironments())
        });

        it("get workspace files", function () {
            return assert.isFulfilled(getUserdata())
        });

        it("get programs and grants", function () {
            return assert.isFulfilled(getProgramsAndGrants())
        })
    });
});

function getEnvironments() {
    var vpg_array = [];
    var ce_array = [];
    return ghapRq.multiVpgGetStacks4User(tester.authHeader, tester, vpg_array)
        .then(function () {
            return ghapRq.multiVpgGetComputeResources4User(tester.authHeader, tester, ce_array)
        })
}

function getUserdata() {
    return ghapRq.dir(tester.authHeader, tester, '');
}

function getActivities() {
    var roles = tester.getRoles();
    var activities = [];
    return ghapRq.getUserRoles(tester.authHeader, tester)
        .then(function () {
            var promises = [];
            for(var i = 0; i < roles.length; i++)
                promises.push(ghapRq.getARAssociationsForRole(tester.authHeader, roles[i]));
            return Q.allSettled(promises)
        })
        .then(function () {
            for(var i = 0; i < roles.length; i++) {
                var ar = roles[i].ar_associations;
                for(var j =0; j < ar.length; j++) {
                    var activityId = ar[j].activityId;
                    if (!my.findElementInArray(activities, activityId))
                        activities.push(activityId);
                }
            }
        })
        .then(function () {
            var promises = [];
            for(var i = 0; i < activities.length; i++)
                promises.push(ghapRq.getActivityById(tester.authHeader, activities[i]));
            return Q.allSettled(promises)
        })
}

function getProgramsAndGrants() {
    var all_projects = [];
    return ghapRq.getAllProjects(tester.authHeader, all_projects)
        .then(function () {
            return ghapRq.getAllProjects4User(tester.authHeader, tester);
        })
        .then(function () {
            var promises = [];
            for (var i = 0; i < all_projects.length; i++) {
                promises.push(ghapRq.getAllGrants(tester.authHeader, all_projects[i]));
                promises.push(ghapRq.getAllGrantsOfProject4User(tester.authHeader, all_projects[i],tester))
            }
            return Q.allSettled(promises)
        })
}
