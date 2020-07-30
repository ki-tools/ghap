/**
 * Created by vruzov on 21.09.2015.
 */

// Q: How can I stop jshint errors in my web file for globals?
// (the actual .jshintrc configuration for protractor)
// http://stackoverflow.com/questions/24696096/how-can-i-stop-jshint-errors-in-my-web-file-for-globals

var protractorCfg = require('./node_modules/protractor/config.json');
// Q: protractor issue with firefox 43
// Unable to navigate to site after firefox update to V43 (18 Dec 2015)
// https://github.com/SeleniumHQ/selenium/issues/1395
// A: Selenium 2.48.0 does work well with Firefox 43.

var ghapBrowser = require('./ghap-browsers-capabilities');

// Look at config example for details
// https://github.com/angular/protractor/blob/master/docs/referenceConf.js
//
exports.config = {
    seleniumServerJar: 'node_modules/protractor/selenium/selenium-server-standalone-' + protractorCfg.webdriverVersions.selenium + '.jar',
    seleniumArgs: ['-Dwebdriver.ie.driver=node_modules/protractor/selenium/IEDriverServer.exe'],
    //seleniumAddress: 'http://localhost:4444/wd/hub',
    getPageTimeout: 30000,
    allScriptsTimeout: 30000,

    //capabilities: {
        //browserName: 'phantomjs',
        ///*
        // * Can be used to specify the phantomjs binary path.
        // * This can generally be ommitted if you installed phantomjs globally.
        // */
        //'phantomjs.binary.path': require('phantomjs').path,
        ///*
        // * Command line args to pass to ghostdriver, phantomjs's browser driver.
        // * See https://github.com/detro/ghostdriver#faq
        // */
        //'phantomjs.ghostdriver.cli.args': ['--loglevel=DEBUG'],
        //version: '',
        //platform: 'ANY'
    //},

    getMultiCapabilities : ghapBrowser.ghapMultiCapabilities,

    framework: 'jasmine',

    onPrepare: function () {

        // Q: protractor get browser name
        browser.getCapabilities().then(function(s){
            browser.browserName = s.caps_.browserName;
            console.log("Browser name is '%s'", browser.browserName);
            browser.isInternetExplorer = /i.*explore/.test(browser.browserName);
            browser.isChrome = /chrome/.test(browser.browserName);
            browser.isSafari = /safari/.test(browser.browserName);
            browser.isFirefox = /firefox/.test(browser.browserName);
        });

    },

    onCleanUp: function (exit_code) {
        console.log('Test have finished. No user cleanUp procedure assigned. Exit code %d', exit_code);
    }

};