/**
 * Created by Vlad on 29.08.2016.
 */

var cfg = require('./../../frisby-tests/spec/Common/ghap-config');
var umsUser = require('./../../frisby-tests/spec/ums/ums_user');

var tester = umsUser.makeUser();
var perf_testers = require('./performance-testers.json');
tester.setName(perf_testers[cfg.environment].username);
tester.setPassword(perf_testers[cfg.environment].password);

module.exports.getTester = function () {
    return tester;
};

module.exports.setTester = function (newTester) {
    tester = newTester;
};
