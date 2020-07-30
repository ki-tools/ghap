/**
 * Created by Vlad on 26.09.2015.
 */

var gulp = require('gulp');
var protractor = require("gulp-protractor").protractor;
var webdriver_update = require("gulp-protractor").webdriver_update;
var webdriver_standalone = require("gulp-protractor").webdriver_standalone;
var webdriver_update_specific = require("gulp-protractor").webdriver_update_specific;
var util = require('util');

var child_process = require('child_process');

// Q: protractor command line params
// http://stackoverflow.com/questions/23135649/how-can-i-use-command-line-arguments-in-angularjs-protractor
//
// Q: gulp command line args
// http://stackoverflow.com/questions/23023650/is-it-possible-to-pass-a-flag-to-gulp-to-have-it-run-tasks-in-different-ways
// https://github.com/gulpjs/gulp/blob/master/docs/CLI.md

var configJson = require('./ghap-e2e-config.json');

var defaultEnv = configJson.defaultEnvironment;
var defaultBrowser = 'chrome';

// https://nodejs.org/api/process.html#process_process_platform
if (process.platform.indexOf('darwin') > -1) {
    defaultBrowser = 'safari';
} else if (process.platform.indexOf('win') > -1) {
    defaultBrowser = 'ie';
} else if (process.platform.indexOf('linux') > -1) {
    defaultBrowser = 'ff';
}
//defaultBrowser = 'chrome';


var optimist = require('optimist').
    usage('Usage: gulp <task name> [options]\n' +
       'handle webdriver or run GHAP e2e protractor test').
    describe('tasks', 'display tasks list.').
    describe('env', "specify environment to run tests (the possible values are 'prod', 'qa')").
    describe('browser', "specify browser that should be using to launch tests (the possible values are 'ie', 'ff', 'safari', 'chrome')").
    default('env', defaultEnv).
    default('browser', defaultBrowser);

var argv = optimist.argv;

function areArgvKeysValid(argv, valid_argvKeys) {
    //console.log(argv);
    //console.log(Object.keys(argv));
    if (argv.tasks || argv['tasks-json']){
        // run 'gulp --tasks' to list tasks for the loaded gulpfile
        return true;
    }

    if (argv._.length > 1) {
        console.error("Error '%s': parallel tasks do not supported.", argv._);
        return false;
    }

    valid_argvKeys.push('_', '$0', 'color', 'no-color', 'gulpfile');

    return Object.keys(argv).every(function(argv_key){
        var res = valid_argvKeys.indexOf(argv_key) > -1;
        if (!res) console.error("Invalid gulp key '%s'",argv_key);
        return res
    });
}

if (!areArgvKeysValid(argv, ['help', 'browser', 'env'])) {
    displayHelp();
    process.exit(1);
}

if (argv.help) {
    displayHelp();
    process.exit(0);
}

var cfg = configJson['common'];
var env_cfg = configJson[argv.env];
util._extend(cfg, env_cfg);

function runTest(spec, done) {
    var gulp_args = [];

    if (argv.env) {
        if ( (argv.env !== 'prod') && (argv.env !== 'qa')&& (argv.env !== 'samba')) {
            console.error("Invalid gulp --env key value '%s'",argv.env);
            process.exitCode = 1; done();
            return;
        }
        gulp_args.push('--params.environment', argv.env);
    }

    if (argv.browser) {
        switch (argv.browser) {
            case 'ie':
                gulp_args.push('--browser', "internet explorer");
                break;

            case 'ff':
                gulp_args.push('--browser', 'firefox');
                break;

            case 'safari':
                gulp_args.push('--browser', 'safari');
                break;

            case 'chrome':
                gulp_args.push('--browser', 'chrome');
                break;

            default:
                console.error("Invalid gulp --browser key value '%s'", argv.browser);
                process.exitCode = 1; done();
                return;
        }
    }

    gulp.src([spec])
        .pipe(protractor({
            configFile: "ghap-protractor_conf.js",
            args: gulp_args
        }))
        //.on('error', function(e) { done(e) })
        .on('close', function(code, signal){
            var error;
            if (code) error = new Error('Fatal error on ' + spec );
            done(error);
        });
}

function displayHelp(done) {
    optimist.showHelp();
    if (done) done();
}

function _default(done) {
    displayHelp(done);
}

function deleteE2Euser(done){
    var args = [];
    args.push('../frisby-tests/spec/utils/ghap-delete-user');
    args.push(argv.env);
    args.push(cfg.userName);
    var child = child_process.spawn('node', args, {
        stdio: 'inherit'
    }).once('close', function(code, signal){
        var error;
        if (code) error = new Error('Fatal error on delete user.');
        done(error);
    });
}

gulp.task('webdriver_update', webdriver_update);
gulp.task('webdriver_update_ie', webdriver_update_specific({browsers:['ie32']}));
gulp.task('webdriver_standalone', webdriver_standalone);
gulp.task('delE2Euser', deleteE2Euser);
gulp.task('logAcc1', runTest.bind(this, 'loginAndAccount-1_spec.js'));
gulp.task('logAcc2', runTest.bind(this, 'loginAndAccount-2_spec.js'));
gulp.task('forgotPsw', runTest.bind(this, 'forgot-password_spec.js'));
gulp.task('prgmSetup', runTest.bind(this, 'program-setup_spec.js'));
gulp.task('prgmAccess', runTest.bind(this, 'programDataAccess_spec.js'));
gulp.task('sbmtContrib', runTest.bind(this, 'submit-data-by-contributor_spec.js'));
gulp.task('sbmtCurator', runTest.bind(this, 'submit-data-curator_spec.js'));
gulp.task('help',displayHelp);
gulp.task('default', _default);

// Q: gulp run tasks in series
// https://github.com/gulpjs/gulp/blob/4.0/docs/API.md#gulpseriestasks

gulp.task('allTests', gulp.series('delE2Euser', 'logAcc1', 'logAcc2', 'prgmSetup', 'prgmAccess'), function(done){done()});
gulp.task('sbmtDataTests', gulp.series('sbmtContrib','sbmtCurator'), function(done){done()});

