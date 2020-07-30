/**
 * Created by vruzov on 21.09.2015.
 */

// implements steps 1-9 of
// https://tools.certara.com/jira/browse/BAP-597

var my = require('./ghap-e2e-lib');
var cfg = require('./ghap-e2e-config');

var ghapLoginCase = require('./ghap-login-case');
var ghapLogoutCase = require('./ghap-logout-case');
var ghapEmails = require('./ghap-emails');
ghapEmails.startMailListener();

var userPassword = null;
var userWelcomeEmailPromise;
var oldUserPassword;

ghapLoginCase.openLoginPageSpec()
    .then(ghapLoginCase.loginAs.bind(this, cfg.adminName, cfg.adminPassword))
    .then(createNewUserAccount)
    .then(assignAnalystRole)
    .then(assignProgramsAndGrants)
    .then(ghapLogoutCase.logOut)
    .then(getUserPassword)
    .then(function(){
        return ghapLoginCase.loginAs(cfg.userName, userPassword)
    })
    .then(checkReportAProblemDialog)
    .then(checkSuggestAnImprovementDialog)
    .then(acceptTermsOfUse)
    .then(changePassword)
    .then(function(){
        return ghapLoginCase.expectFailedLogin(cfg.userName, oldUserPassword)
    })
    .then(function(){
        return ghapLoginCase.loginAs(cfg.userName, cfg.userPassword)
    })
    .then(ghapLogoutCase.logOut)
    .thenCatch(my.reportError)
    .thenFinally(finished);

function createNewUserAccount(){

    return my.createStdSuite('User Management ', function() {

        it('- navigate to USER MANAGEMENT tab', function(){
            var userManagement_pattern = new RegExp(/^User[.\s]+Management$/i);
            my.getTabLink(userManagement_pattern)
                .then(function(link){ link.click() });
        });

        it ('- navigate to Create Account page', function(){
            var pattern = new RegExp(/^Create Account$/i);
            my.getSubMenuLink(pattern)
                .then(function(link){ link.click() });
        });

        var  createAccount_form;

        it ('- validate Create Account form.', function(){

            createAccount_form = {
                username_input : element(by.model('user.name')),
                firstName_input : element(by.model('user.firstName')),
                lastName_input : element(by.model('user.lastName')),
                email_input : element(by.model('user.email')),
                genPsw_checkbox : element(by.model('user.generatePassword')),
                password_input : element(by.model('password')),
                pswConfirm_input : element(by.model('passwordConfirmation')),
                //usrStorage_checkbox : element(by.model('user.storage')),
                createUser_submit : element(by.className('submit-button'))
            };
            my.validateFields(createAccount_form);
        });

        it ('- fill Create Account form and submit', function(){
            createAccount_form.username_input.clear().sendKeys(cfg.userName);
            createAccount_form.firstName_input.clear().sendKeys('e2e');
            createAccount_form.lastName_input.clear().sendKeys('user');
            createAccount_form.email_input.clear().sendKeys(cfg.userEmail);

            userWelcomeEmailPromise = getWelcomeEmailPromise();

            createAccount_form.genPsw_checkbox.click().then(function(){
                createAccount_form.password_input.isDisplayed().then(function(res){
                    console.log("Input password field isDisplayed? %s", res, res ? 'FAIL':'Ok.');
                    expect(res).toBeFalsy();
                });
                createAccount_form.pswConfirm_input.isDisplayed().then(function (res) {
                    console.log("Input password confirmation field isDisplayed? %s", res, res ? 'FAIL' : 'Ok.');
                    expect(res).toBeFalsy();
                });
                createAccount_form.createUser_submit.click()
                  .then(my.validateSubmitResult.bind(this, false));  // noReject
            });
        });

    });
}

function getWelcomeEmailPromise() {
    var ghapEmailListener = ghapEmails.createGhapEmailListener();

    ghapEmailListener.startListening (function(mail, deferred ){
        var mail_domain = my.getDomainFromUrl(cfg.ghapUrl);
        if (mail.headers.from.indexOf(mail_domain) === -1) {
            console.log("Mail ignored since it is received not from '%s' domain.", mail_domain);
            return;
        }
        if (extractPassword(mail.text))
            deferred.fulfill(mail);
        else {
            console.log("Password not found in email.");
            console.log(mail.text);
            deferred.reject(new Error("Mail parsing failed - password not found."))
        }
    });

    return ghapEmailListener.deferred.promise

}

function extractPassword(text) {
    // use https://regex101.com/ to debug regex
    var pattern = new RegExp(/your password is (.{10})/);
    var res = text.match(pattern);
    if (res) return res[1];
    return null
}

function assignAnalystRole() {

    return my.createStdSuite('Assign Analyst Role ', function() {

        it ('- navigate to Manage permissions page of USER MANAGEMENT', function(){
            // if do not scroll the screen on top then....
            my.scrollToWindowTop();
            var pattern = new RegExp(/^Manage Permissions$/i);
            my.getSubMenuLink(pattern)
                .then(function(link){ link.click() });
        });

        it ("- select '"+cfg.userName+"' in users list", function(){
            my.filterUsers(cfg.userName);
            my.getUserLink(cfg.userNamePattern)
              .then(function(link){
                    // Q: protractor scroll to element
                    // A: https://github.com/angular/protractor/issues/1749
                    //browser.executeScript('arguments[0].scrollIntoView()', link.getWebElement())
                    //    .then(function(){
                    //        link.click();
                    //        browser.sleep(3000);
                    //    });
                    link.click();
                });
        });

        it('- select Data Analyst role', function () {
            // if do not scroll the screen on top then checkboxes were uncheckable....
            // my.scrollToWindowTop();
            var roles_list = element.all(by.repeater('role in roles')).all(by.tagName('label'));
            roles_list.count().then(function (num) {
                console.log("%d roles found.", num)
            });
            expect(roles_list.count()).toBeGreaterThan(3);
            var name_pattern = new RegExp(/^Data Analyst$/);
            my.getRepeaterLink(roles_list, name_pattern, "Data Analyst role not found.")
                .then(function (link) { link.click() });

        });

        it('- push save, validate message', function(){
            element(by.className('submit-button'))
                .click()
                .then(my.validateSubmitResult);
        });

    });
}

function assignProgramsAndGrants(){

    return my.createStdSuite('Assign Programs & Grants', function() {

        it('- navigate to PROGRAM MANAGEMENT tab', function () {
            // my.scrollToWindowTop();
            var programManagement_pattern = new RegExp(/^Program[.\s]+Management$/i);
            my.getTabLink(programManagement_pattern)
                .then(function (link) { link.click() })
        });

        it ('- navigate to Assignment by User page', function(){
            var pattern = new RegExp(/^Assignment by User$/i);
            my.getSubMenuLink(pattern)
              .then(function(link){ link.click() });
        });

        it ("- select '"+cfg.userName+"' in users list", function(){
            my.filterUsers(cfg.userName);
            my.getUserLink(cfg.userNamePattern)
              .then(function(link){ link.click() });
        });

        var programs_list;

        it('- select T1 project', function () {
            //my.scrollToWindowTop();
            my.filterPrograms('T1');
            programs_list = element.all(by.repeater('program in programs'));
            programs_list.count()
                .then(function (num) {
                    console.log("%d projects found.", num)
                });
            expect(programs_list.count()).toBeGreaterThan(0);
            var name_pattern = new RegExp(/^T1[\s]+Read-only$/);
            my.getRepeaterLink(programs_list, name_pattern, "T1 program not found.")
                .then(function (li_element) {
                    var link = li_element.all(by.tagName('input')).first();
                    link.click();
                });
            my.clearProgramsFilter();
        });

        var t2_grants;

        it('- expand T2 project', function () {
            var name_pattern = new RegExp(/^T2[\s]+Read-only$/);
            var li_element;
            var t2_link;
            my.getRepeaterLink(programs_list, name_pattern, "T2 program not found.")
                .then(function (li_el) {
                    li_element = li_el;
                    t2_link = li_element.all(by.tagName('div')).first();
                })
                .then(function () {
                    return my.scrollBy(0,100)
                        .then(function () {
                            return t2_link.click();
                        })
                })
                .then(function () {
                    // li_element.getInnerHtml().then(function(html){console.log(html)});

                    var ul_element = li_element.element(by.tagName('ul'));

                    var EC = protractor.ExpectedConditions;
                    // Waits for the UL element to be visible on the dom.
                    browser.wait(EC.visibilityOf(ul_element), 3000)
                        .then(function () {
                            //ul_element.getInnerHtml().then(function(html){console.log(html)});
                            t2_grants = ul_element.all(by.repeater('grant in program.grants'));
                            t2_grants.count().then(function (num) {
                                console.log("%d grants found.", num)
                            });

                            return my.scrollElementToView(t2_link)
                        });

                });

        });

        it('- grant full access on G2 grant', function(){
            var name_pattern = new RegExp(/^G2[\s]+Read-only$/);
            my.getRepeaterLink(t2_grants, name_pattern, "T2/G2 grant not found.")
              .then(function(li_element){
                  var link = li_element.all(by.tagName('input')).first();
                  return link.click();
              });
        });

        it('- grant read-only access on G3', function(){
            var name_pattern = new RegExp(/^G3[\s]+Read-only$/);
            my.getRepeaterLink(t2_grants, name_pattern, "T2/G3 grant not found.")
              .then(function(li_element){
                  var link = li_element.all(by.tagName('input')).get(1);
                  link.click();
                  return my.pauseTest(3000);
              });
        });

        var timeout_in_millis = 50*1000;
        it("- push ASSIGN, validate result", function(){
            var submit = element(by.className('submit-button'));
            submit.click()
                .then(my.validateSubmitResult.bind(this, true))  // noReject
                .then(function(result){
                    if (result.indexOf('No such users') > -1){
                        console.log("Created user does not appear in stash. Pause for %s sec and retry.", timeout_in_millis/1000-10);
                        browser.sleep(timeout_in_millis - 10000)
                            .then(submit.click)
                            .then(my.validateSubmitResult)
                    } else {
                        expect(result).not.toContain('ERROR:')
                    }
                })
        }, timeout_in_millis);

    });
}

function getUserPassword(){

    return my.createStdSuite("Get user password", function() {

        var timeout_in_millis = 32*1000;
        it('- receive & parse email', function(){
            browser.controlFlow()
                .wait(userWelcomeEmailPromise, timeout_in_millis-2000, 'Welcome email not received in time.')
                .then(function (email) {
                    userPassword = extractPassword(email.text);
                    expect(userPassword).not.toBeNull();
                    console.log("User password '%s' found in email", userPassword)
                });
        },timeout_in_millis);

    });
}

function acceptTermsOfUse() {

    return my.createStdSuite('Accept TermsOfUse', function() {

        it('- validate URL', function(){
            browser.getCurrentUrl().then(function(url){
                expect(url).toContain('terms');
            });
        });

        it('- CONTINUE button should be disabled', function(){
            var continue_button = element(by.buttonText('CONTINUE'));
            continue_button.isPresent().then(function(res){
                    expect(res).toBeTruthy();
                    if (res){
                        continue_button.isEnabled().then(function(res){
                            expect(res).toBeFalsy();
                            if (res)
                                console.log("'CONTINUE' button is enabled.")
                        })
                    } else
                        console.log("'CONTINUE' button is missed on page.")
                })
        });

        it('- click agree checkbox', function(){
            var agree_checkbox = element(by.model('agree'));
            agree_checkbox.isPresent().then(function(res){
                expect(res).toBeTruthy();
                if (res){
                    return agree_checkbox.click()
                }
            })
        });

        it("- click 'CONTINUE', validate URL", function(){
            element(by.buttonText('CONTINUE'))
                .click().then(function(){
                    browser.getCurrentUrl().then(function(url){
                        expect(url).not.toContain('terms');
                    });
                });

        });
    });
}

function checkReportAProblemDialog(){
    return checkModalDialog('Report a Problem', 'Report a Problem');
}

function checkSuggestAnImprovementDialog(){
    return checkModalDialog('Suggest an Improvement', 'Suggest an Improvement to the GHAP');
}

function checkModalDialog(button_text, win_header){

    return my.createStdSuite("Check '"+button_text+"' dialog", function() {

        it("- open dialog", function(){
            var report_button = element(by.buttonText(button_text));
            report_button.isPresent().then(function(res){
                expect(res).toBeTruthy();
                if (res){
                    report_button.click();
                    my.pauseTest(1000);
                    var default_buttons = element.all(by.className('default-button'));
                    expect(default_buttons.count()).toBe(1);
                } else
                    console.log("'%s' button is missed on page.", button_text)
            })
        });

        it("- validate modal dialog header", function(){
            element(by.className('modal-header')).getText()
                .then(function(text){
                    expect(text).toContain(win_header)
                })
        });

        it("- click default button", function(){
            var cancel_button = element(by.className('default-button'));
            cancel_button.isPresent().then(function(res){
                expect(res).toBeTruthy();
                if (res) {
                    expect(cancel_button.getAttribute('value')).toBe("Cancel");
                    cancel_button.click();
                    return my.pauseTest(1000);
                } else
                    console.log("Default button not found.")
            })
        });

    });
}

function changePassword(){
    oldUserPassword = 'fake-password';

    return my.createStdSuite("Change password", function() {

        var change_password_fields;
        it('- validate fields in the form', function(){
            change_password_fields = {
                //currentPassword_input : element(by.model('user.currentPassword')),
                password_input : element(by.model('password')),
                pswConfirm_input : element(by.model('passwordConfirmation')),
                updateAccount_submit : $('input[type=image]')
            };
            my.validateFields(change_password_fields);
        });

        it('- fill fields, click submit, validate result', function(){
            //change_password_fields.currentPassword_input.clear().sendKeys(userPassword);
            change_password_fields.password_input.clear().sendKeys(cfg.userPassword);
            change_password_fields.pswConfirm_input.clear().sendKeys(cfg.userPassword);
            change_password_fields.updateAccount_submit.click()
                //.then(my.validateSubmitResult)
                .then(function(){
                    browser.sleep(3000);
                    oldUserPassword = userPassword;
                    userPassword = cfg.userPassword;
                })
        });

    });
}

function finished(){
    ghapEmails.stopMailListener();
    console.log("E2E test case finished.")
}