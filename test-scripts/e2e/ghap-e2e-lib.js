/**
 * Created by vruzov on 21.09.2015.
 */

var util = require("util");
var fs = require('fs');
var path = require('path');

exports = module.exports = {};

function FailFast(deferred) {
    this.defer = deferred;
    this.err_count = 0;
}


// http://stackoverflow.com/questions/14679951/how-can-i-make-jasmine-js-stop-after-a-test-failure
// http://stackoverflow.com/questions/28893436/how-to-stop-protractor-from-running-further-testcases-on-failure
FailFast.prototype.cancelCaseOnError = function(){
    // check if any have failed
    this.err_count += jasmine.getEnv().currentSpec.results().failedCount;
    if(this.err_count > 0) {
        // if so, change the function which should move to the next test
        jasmine.Queue.prototype.next_ = function () {
            // to instead skip to the end
            this.onComplete();
        };
        var current_spec_name = jasmine.getEnv().currentSpec.getFullName();
        console.log("Following specs will be skipped. %d errors happen in the current spec '%s'.",
            this.err_count, current_spec_name);
        this.defer.reject(new Error(current_spec_name + ' spec have failed.'));
    }
};

FailFast.prototype.setAfterEach = function(){
    // 'afterEach' - is a global defined by jasmine
    afterEach(this.cancelCaseOnError.bind(this));
};

/**
 * Add a spec to the suite that falls if a errors happen in the previous specs and rej
 */
FailFast.prototype.fulfillDeferSpec = function(){
    var self =this;
    // 'it' - is a global defined by jasmine
    it( '- errors should not happen in previous specs', function(){
        expect(self.err_count).toBe(0);
        if (self.err_count === 0) self.defer.fulfill();
    } );
};

exports.createFailFast = function(deferred) {return new FailFast(deferred)};

exports.logSpecFullName=function(){
    console.log(jasmine.getEnv().currentSpec.getFullName());
};

/**
 * Create my standard Jasmine suite.
 * A test suite begins with a call to the global Jasmine function describe.
 * @param {string} title_str - suite title
 * @param {Function} create_its_func -  the function create all specs.
 * @returns {Promise} Return a promise resolved with empty value if no error happens in suite or rejected with error message.
 */
exports.createStdSuite = function(title_str, create_its_func) {
    var deferred = protractor.promise.defer();
    var failFast = exports.createFailFast(deferred);

    describe(title_str, function() {

        console.log();
        beforeEach(exports.logSpecFullName);
        failFast.setAfterEach();

        create_its_func();

        failFast.fulfillDeferSpec();

    });

    return deferred.promise;
}

exports.reportError = function(err){
    if (err instanceof Error) {
        console.error(err);
        console.error(err.stack)
    }
};

// http://stackoverflow.com/questions/23571852/protractor-scroll-down
// http://forum.ionicframework.com/t/solved-protractor-testing-issue/8581/2
exports.scrollElementToView = function(el){
    return browser.executeScript('arguments[0].scrollIntoView()', el.getWebElement())
};

exports.scrollToWindowTop = function(){
    return browser.executeScript('window.scrollTo(0,0);')
};

exports.scrollBy = function(x, y){
    return browser.executeScript('window.scrollBy(arguments[0],arguments[1]);', x, y);
};

exports.pauseTest = function(ms){
    return browser.sleep(ms)
};

exports.consoleLog = function consoleLog(){console.log.apply(this, arguments)};

exports.getRepeaterLink = function(repeater_links, name_pattern, err_message, debug){
    var deferred = protractor.promise.defer();

    repeater_links
        .filter(function (elem, index) {
            return elem.getText().then(function (text) {
                var is_match = (text.match(name_pattern) !== null);
                if (debug)
                    console.log("Checked link '%s' : %s", text, is_match ? 'Ok.' : 'Fail.');
                return is_match;
            });
        })
        .then(function (filteredElements) {
            expect(filteredElements.length).toBe(1);
            if (filteredElements[0])
                deferred.fulfill( filteredElements[0]);
            else
                deferred.reject(new Error(err_message))
        });

    return deferred.promise;
};

exports.getTabLink = function(name_pattern){
    var links = element.all(by.repeater('item in Nav.menus')).all(by.tagName('a'));
    expect(links.count()).toBeGreaterThan(4);
    return exports.getRepeaterLink(links, name_pattern, "Tab link not found.")
};

exports.getSubMenuLink = function (name_pattern) {
    var links = element.all(by.repeater('submenu in Nav.submenus')).all(by.tagName('a'));
    expect(links.count()).toBeGreaterThan(1);
    return exports.getRepeaterLink(links, name_pattern, "SubMenu link not found.")
};

exports.getUserLink = function(name_pattern) {
    var links = element.all(by.repeater('user in users'));
    expect(links.count()).toBeGreaterThan(0);
    return exports.getRepeaterLink(links, name_pattern, "User not found.")
};

exports.filterUsers = function(filter_str) {
    var filter_input = $('input[placeholder="Search by user name"]');
    expect(filter_input.isPresent()).toBe(true);
    return filter_input.sendKeys(filter_str);
};

exports.filterPrograms = function(filter_str) {
    var filter_input = $('input[placeholder="Search by program name"]');
    expect(filter_input.isPresent()).toBe(true);
    return filter_input.sendKeys(filter_str);
};

exports.clearProgramsFilter = function() {
    var filter_input = $('input[placeholder="Search by program name"]');
    var parent_of_parent = filter_input.element(by.xpath('../..'));
    return parent_of_parent.$('img').click();
};

exports.validateSubmitResult = function(bool_noReject) {
    var deferred = protractor.promise.defer();

    var err_div = element(by.className('error-message-cont'));
    var success_div = element(by.css('[ng-bind-html="success"]'));

    err_div.isDisplayed().then(function(res){
        console.log("err_div isDisplayed? %s %s", res, res ? 'FAIL':'Ok.');
        if (res) {
            err_div.getText().then(function(text){
                console.log('Error:',text);
                if (bool_noReject)
                    deferred.fulfill('ERROR:'+text);
                else
                    deferred.reject(new Error(text))
            })
        } else {
            success_div.isDisplayed().then(function(res){
                console.log("success_div isDisplayed? %s %s", res, res ? 'Ok.':'FAIL');
                if (res) {
                    success_div.getText().then(function(text){
                        console.log('Success:',text);
                        deferred.fulfill(text);
                    })
                } else {
                    if (bool_noReject)
                        deferred.fulfill('ERROR: Success message not displayed.');
                    else
                        deferred.reject(new Error('Success message not displayed.'))
                }
            });
        }
    });

    return deferred.promise;
};

exports.validateFields = function(form_object){
    // http://stackoverflow.com/questions/7440001/iterate-over-object-keys-in-node-js
    Object.keys(form_object).forEach(function(field){
        form_object[field].isPresent().then(function(result){
            if (!result) console.log("%s field is missed in form.", field);
            expect(result).toBe(true);
        });
    })
};

//
// Parse body as JSON, ensuring not to re-parse when body is already an object (thanks @dcaylor)
//
exports.jsonParse = function (body) {
    var json = "";
    try {
        json = (typeof body === "object") ? body : JSON.parse(body);
    } catch(e) {
        throw new Error("Error parsing JSON string: " + e.message + "\n\tGiven: " + body);
    }
    return json;
};

exports.getInspectObjStr = function(obj){
    return util.inspect(obj, {showHidden: false, depth: null})
};

exports.logJSON = function (json){
    console.log(exports.getInspectObjStr(exports.jsonParse(json)));
};

exports.preventAlertErrorOnSafari = function() {
    // https://code.google.com/p/selenium/issues/detail?id=3862
    if (browser.isSafari) {
       return browser.executeScript("confirm = function(message){return true;};")
           .then(function(){
               return browser.executeScript("prompt = function(message){return true;};");
           })
           .then(function(){
               return browser.executeScript("alert = function(message){return true;};");
           })
    } else {
        var d = protractor.promise.defer();
        d.fulfill();
        return d.promise;
    }
};

exports.getDownloadPath = function() {
    var base_path = process.env.HOME;
    if ( !(process.platform.indexOf('darwin') > -1) &&
         (process.platform.indexOf('win') > -1)) {
        base_path = process.env.USERPROFILE;   // on WINDOWS
    }
    var download_path = path.join( base_path, 'Downloads');

    // http://stackoverflow.com/questions/4482686/check-synchronously-if-file-directory-exists-in-node-js
    try {
        var stats = fs.lstatSync(download_path);
        if (stats.isDirectory()) {
            console.log("Download path is '%s'", download_path)
        } else {
            console.log("'%s' is not directory.", download_path);
            download_path = null;
        }
    }
    catch (e) {
        console.log("Can not set download path to'%s'", download_path);
        console.log(e.toString());
    }
    return download_path;
};

exports.deleteFileIfExists = function(file_path) {
    try {
        var stats = fs.lstatSync(file_path);
        if (stats.isFile()) {
            fs.unlinkSync(file_path);
            console.log("'%s' is deleted.", file_path);
        }
    } catch (e) {
        if (e.code !== 'ENOENT') console.log(e.toString());
    }
};

exports.getDomainFromUrl = function(url_str) {
    // http://stackoverflow.com/questions/25703360/regular-expression-extract-subdomain-domain
    var pattern = new RegExp(/^(?:https?:\/\/)?(?:www\.)?([^:\/\n]+)/im)
    var res = url_str.match(pattern);
    if (res) return res[1];
    return null
};

function dayStr(date){
    return date.toISOString()
        .substr(2,8)
        .replace(new RegExp('-', 'g'), '')
}

function timeStr(date){
    return date.toISOString()
        .substr(11,8)
        .replace(new RegExp(':', 'g'),'')
}

exports.dateTimeStr = function(date){
    return dayStr(date)+timeStr(date)
};
