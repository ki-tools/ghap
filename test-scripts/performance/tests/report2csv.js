/**
 * Created by Vlad on 16.09.2016.
 */

var cfg = require('./../../frisby-tests/spec/Common/ghap-config');
var fs = require('fs');

/**
 * Append new line to the report file
 * @param testName
 * @param execTime
 * @return void
 */
exports.appendLine = function (testName, execTime) {
    var logFileName = 'test-log';
    if (cfg.environment != 'prod') logFileName += '-'+cfg.environment;
    logFileName += '.csv';

    var results = [];
    results.push(new Date().toISOString());
    results.push(testName);
    results.push(execTime);
    fs.appendFileSync(logFileName, results.toString() + '\r\n');
};
