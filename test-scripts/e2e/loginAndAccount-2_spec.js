/**
 * Created by Vlad on 07.10.2015.
 */

// implements steps 10-19 of
// https://tools.certara.com/jira/browse/BAP-597

var my = require('./ghap-e2e-lib');
var cfg = require('./ghap-e2e-config');

var ghapLoginCase = require('./ghap-login-case');
var ghapLogoutCase = require('./ghap-logout-case');
var ghapEmails = require('./ghap-emails');
ghapEmails.startMailListener();

var userUpdateEmailPromise;
var start = new Date();
var newFirstName = 'e2e'+dateTimeStr(start);
var newLastName = 'user'+dateTimeStr(start);

var account_page_fields;

ghapLoginCase.openLoginPageSpec()
    .then(ghapLoginCase.loginAs.bind(this, cfg.userName, cfg.userPassword))
    .then(openAccountPage)
    .then(updateFirstLastName)
    .then(validatePasswordChange)
    .then(ghapLogoutCase.logOut)
    .thenCatch(my.reportError)
    .thenFinally(finished);

function openAccountPage(){

    var deferred = protractor.promise.defer();
    var failFast = my.createFailFast(deferred);

    describe("Open Account Page", function() {

        beforeEach(my.logSpecFullName);
        failFast.setAfterEach();

        it('- click on Account link', function(){
            var account_link = $('.top-nav-text').element(by.linkText('Account'));
            account_link.isPresent()
                .then(function(res){
                    expect(res).toBeTruthy();
                    if (res) account_link.click()
                })
        });

        it('- validate fields list on the page', function(){
            account_page_fields = {
                userName_input : element(by.model('user.name')),
                firstName_input : element(by.model('user.firstName')),
                lastName_input : element(by.model('user.lastName')),
                email_input : element(by.model('user.email')),
                currentPassword_input : element(by.model('user.currentPassword')),
                password_input : element(by.model('password')),
                pswConfirm_input : element(by.model('passwordConfirmation')),
                updateAccount_submit : element(by.className('submit-button'))
            };
            my.validateFields(account_page_fields);
        });

        failFast.fulfillDeferSpec();

    });

    return deferred.promise;

}

function updateFirstLastName(){

    var deferred = protractor.promise.defer();
    var failFast = my.createFailFast(deferred);

    describe("Update First and Last names", function() {

        beforeEach(my.logSpecFullName);
        failFast.setAfterEach();

        it('- update fields', function(){
            account_page_fields.firstName_input.clear().sendKeys(newFirstName);
            account_page_fields.lastName_input.clear().sendKeys(newLastName);

            userUpdateEmailPromise = getUserUpdateEmailPromise();

            account_page_fields.updateAccount_submit.click()
                .then(my.validateSubmitResult);
        });

        var timeout_in_millis = 32*1000;
        it('- receive & validate email', function(){
            browser.controlFlow()
                .wait(userUpdateEmailPromise, timeout_in_millis - 2000, 'Update account confirmation email not received in time.')
                .then(function (email) {
                    expect(email.text).toContain(newFirstName);
                    expect(email.text).toContain(newLastName);
                })
        }, timeout_in_millis);

        failFast.fulfillDeferSpec();

    });

    return deferred.promise;

}

function getUserUpdateEmailPromise() {
    var ghapEmailListiner = ghapEmails.createGhapEmailListener();

    ghapEmailListiner.startListening (function(mail, deferred ){
        var mail_domain = my.getDomainFromUrl(cfg.ghapUrl);
        if (mail.headers.from.indexOf(mail_domain) === -1) {
            console.log("Mail ignored since it is received not from '%s' domain.", mail_domain);
            return;
        }
        if (mail.subject.indexOf('Account Modified') > -1){
            deferred.fulfill(mail);
        } else {
            deferred.reject(new Error('Invalid subject \''+mail.subject+'\' in expected email.'))
        }
    });

    return ghapEmailListiner.deferred.promise

}

function validatePasswordChange(){

    var deferred = protractor.promise.defer();
    var failFast = my.createFailFast(deferred);

    describe("Validate password change", function() {

        beforeEach(my.logSpecFullName);
        failFast.setAfterEach();

        function putNewWrongPassword (password, password_confirmation, err_msg) {

            console.log("Validate password '%s'", password);

            if (!password_confirmation) password_confirmation = password;
            if (!err_msg) err_msg = 'Password is invalid';

            account_page_fields.password_input.clear().sendKeys(password);
            account_page_fields.pswConfirm_input.clear().sendKeys(password_confirmation);
            account_page_fields.updateAccount_submit.click()
                .then(my.validateSubmitResult.bind(this, true))  // noReject
                .then(function(text) {
                    expect(text).toContain('ERROR:');
                    expect(text).toContain(err_msg);
                })
        }

        function resetCurrentAndPutWrongPassword(password){
            account_page_fields.currentPassword_input.clear().sendKeys(cfg.userPassword);
            putNewWrongPassword(password);
        }

        it('- password should be minimum 8 character in length',
            resetCurrentAndPutWrongPassword.bind(this, '$%er7VS')
        );

        it('- password should contain uppercase letters',
            resetCurrentAndPutWrongPassword.bind(this,'$%er7vsf')
        );

        it('- password should contain lowercase letters',
            resetCurrentAndPutWrongPassword.bind(this,'$%ER7VSF')
        );

        it('- password should contain a digit',
            resetCurrentAndPutWrongPassword.bind(this,'$%erIVSF')
        );

        it('- password should contain non-alphanumeric characters',
            resetCurrentAndPutWrongPassword.bind(this,'S1er9VSF')
        );

        it('- should denied change if current password is invalid',
            function() {
                account_page_fields.currentPassword_input.clear().sendKeys('$%er7VSF');
                putNewWrongPassword('$%er7VSF', '$%er7VSF', 'Current Password is invalid');
            }
        );

        it('- should denied change if password confirmation is invalid',
            function() {
                account_page_fields.currentPassword_input.clear().sendKeys(cfg.userPassword);
                putNewWrongPassword('$%er7VSF', '$%er9VSF', 'Invalid password confirmation');
            }
        );

        failFast.fulfillDeferSpec();

    });

    return deferred.promise;

}


function dayStr(date){
    return date.toISOString()
        .substr(0,10)
        .replace(new RegExp('-', 'g'), '')
}

function timeStr(date){
    return date.toISOString()
        .substr(11,8)
        .replace(new RegExp(':', 'g'),'')
}

function dateTimeStr(date){
    return dayStr(date)+timeStr(date)
}

function finished(){
    ghapEmails.stopMailListener();
    console.log("Login & account (part 2) E2E test case have finished.")
}