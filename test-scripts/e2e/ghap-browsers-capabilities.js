/**
 * Created by Vlad on 15.11.2015.
 */

var FirefoxProfile = require('firefox-profile');
var q = require('q');

// https://github.com/angular/protractor/blob/master/docs/referenceConf.js
// ...
// If you need to resolve multiCapabilities asynchronously (i.e. wait for
// server/proxy, set firefox profile, etc), you can specify a function here
// which will return either `multiCapabilities` or a promise to
// `multiCapabilities`.
// If this returns a promise, it is resolved immediately after
// `beforeLaunch` is run, and before any driver is set up.
// If this is specified, both capabilities and multiCapabilities will be
// ignored.

var optimist = require('optimist');
//console.log(optimist.argv);
//process.exit(1);
var params = optimist.argv;

var browserName;
if (params.hasOwnProperty('browser')) {
    browserName = params['browser'].toLowerCase();
} else {
    var configJson = require('./ghap-e2e-config.json');
    browserName = configJson.defaultBrowser;
}

if (browserName === 'ie')
    browserName = 'internet explorer';
else if (browserName === 'ff')
    browserName = 'firefox';

console.log("Protractor configured use '%s' browser.", browserName);

exports.browserName = browserName;

if (browserName === 'safari') {

    // ghapMultiCapabilities does not setup safari correctly for me...
    exports.ghapMultiCapabilities = null;

} else {
    exports.ghapMultiCapabilities = function() {
        var multi_capabilities =[];

        if (browserName === 'chrome')
            multi_capabilities.push( getChromeCapabilities() );
        else if (browserName === 'internet explorer')
            multi_capabilities.push(getIeCapabilities());
        else if (browserName === 'firefox')
            multi_capabilities = getFirefoxCapabilities();
        else
            throw new Error("Unsupported browser '" + browserName +"'");

        return multi_capabilities;
    };
}

function getChromeCapabilities() {
    return {
        browserName: 'chrome',
        chromeOptions: {
            args: ['--start-maximized'],
            prefs: {
                // this is already the default on Chrome but for completeness
                'download': {'prompt_for_download': false}
            }
        }
    }
}

function getIeCapabilities() {
    return {
        browserName: 'internet explorer'
    }
}

function getFirefoxCapabilities () {

// https://github.com/juliemr/protractor-demo/tree/master/howtos/setFirefoxProfile
// http://stackoverflow.com/questions/31526120/download-file-on-firefox-with-protractor
// http://stackoverflow.com/questions/13839544/using-selenium-webdrivers-method-browser-helperapps-neverask-savetodisk-how-ca
// firefox config entries:  http://kb.mozillazine.org/About:config_entries

    var deferred = q.defer();

    var firefoxProfile = new FirefoxProfile();
    firefoxProfile.setPreference("browser.download.folderList", 2);
    firefoxProfile.setPreference("browser.helperApps.neverAsk.saveToDisk", "application/octet-stream");
    firefoxProfile.setPreference("browser.helperApps.alwaysAsk.force", false);
    firefoxProfile.setPreference("browser.download.manager.showWhenStarting",false);
    firefoxProfile.encoded(function(encodedProfile) {
        var multiCapabilities = [{
            browserName: 'firefox',
            firefox_profile : encodedProfile
        }];
        deferred.resolve(multiCapabilities);
    });

    return deferred.promise;
}
