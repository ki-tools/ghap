/**
 * Created by Vlad on 03.09.2015.
 */

var cfg = require('./ghap-e2e-config');

var my = require('./ghap-e2e-lib');
var ghapLoginCase = require('./ghap-login-case');
var ghapEmails = require('./ghap-emails');
ghapEmails.startMailListener();

ghapLoginCase.openLoginPageSpec()
	.then(clickForgotPasswordLink)
	.then(putUsernameAndSubmit)
	.then(handlePasswordResetEmail)
	.then(openPasswordResetPage)
	.then(putNewPasswordAndSubmit)
	.then(function(){
		return ghapLoginCase.loginAs(cfg.userName, cfg.userPassword)
	})
	.then(handlePasswordModifiedConfirmationEmail)
	.thenFinally(finished);

var err_count = 0;
// http://stackoverflow.com/questions/14679951/how-can-i-make-jasmine-js-stop-after-a-test-failure
function cancelCaseOnError(deferred){
	// check if any have failed
	err_count += jasmine.getEnv().currentSpec.results().failedCount;
	if(err_count > 0) {
		// if so, change the function which should move to the next test
		jasmine.Queue.prototype.next_ = function () {
			// to instead skip to the end
			this.onComplete();
		};
		console.log('Following specs will be skipped.');
		deferred.reject(new Error('Spec failed.'));
	}
}

var awaitEmailPromise;

function logSpecFullName(){
	console.log(this.getFullName());
}

var fulfillDeferSpecStr = '- errors should not happen in previous specs';
function fulfillDefer(deferred){
	expect(err_count).toBe(0);
	if (err_count === 0) deferred.fulfill();
}

function clickForgotPasswordLink(){

	var forgotPasswordLink;
	var deferred = protractor.promise.defer();

	describe('Click Forgot Password link', function(){

		beforeEach(logSpecFullName);
		afterEach(cancelCaseOnError.bind(this, deferred));

		it('- link should be present on login page', function(){

			browser.driver.isElementPresent(By.partialLinkText('Forgot your password'))
				.then(function(result){
					expect(result).toBeTruthy();
					if (result)
						forgotPasswordLink = browser.driver.findElement(By.partialLinkText('Forgot your password'));
					else
						console.error("'Forgot password' link not found on ligin page.")
				});
		});

		it('- click on link should open #forgot-password page', function(){
			forgotPasswordLink.click();
			browser.driver.sleep(2000);
			browser.getCurrentUrl().then(function(url){
				expect(path.basename(url)).toBe('forgot-password');
			});
		});

		it( fulfillDeferSpecStr, fulfillDefer.bind(this,deferred) );

	});

	return deferred.promise;

}

function putUsernameAndSubmit(){

	var deferred = protractor.promise.defer();

	describe('Put Username to the input field and click Continue button', function(){

		beforeEach(logSpecFullName);
		afterEach(cancelCaseOnError.bind(this, deferred));

		var input_username = null;
		var continue_button = null;

		it('- input field and continue button should be present on forgot-password page', function(){
			input_username = element(by.model('userid'));
			continue_button = element(by.className('submit-button'));

			expect(input_username.isPresent()).toBe(true);
			expect(continue_button.isPresent()).toBe(true);

			input_username.isPresent().then(function(result){
				if (!result) console.error('Input field not found.')
			});

			continue_button.isPresent().then(function(result){
				if (!result) console.error('Continue button not found.')
			});

		});

		it('- should show requestSent div', function(){
			input_username.sendKeys(cfg.userName);
			continue_button.click();
			awaitEmailPromise = getPasswordResetConfirmationEmail();
			browser.driver.sleep(1000);

			// http://stackoverflow.com/questions/22850271/how-to-use-protractor-to-check-if-an-element-is-visible
			expect($('[ng-show=requestSent]').isDisplayed()).toBeTruthy();
		});

		it( fulfillDeferSpecStr, fulfillDefer.bind(this,deferred) );

	});

	return deferred.promise;
}

function getPasswordResetConfirmationEmail() {

	var ghapEmailListiner = ghapEmails.createGhapEmailListener();

	ghapEmailListiner.startListening (function(mail, deferred ){
		var mail_domain = my.getDomainFromUrl(cfg.ghapUrl);
		if (mail.headers.from.indexOf(mail_domain) === -1) {
			console.log("Mail ignored since it is received not from '%s' domain.", mail_domain);
			return;
		}
		if (extractPasswordResetUrl(mail.text))	{
			deferred.fulfill(mail);
		} else {
			deferred.reject(new Error('Email parsing fail: cannot find reset password URL.'));
		}
	});

	return ghapEmailListiner.deferred.promise

}

function extractPasswordResetUrl(text){
	// use https://regex101.com/ to debug regex
	//var pattern = new RegExp(/Reset password url: <a href="(https?:\/\/www\..{0,4}ghap.io\/#\/password-reset\?token=[\w-]{36})">/);
	var pattern = new RegExp(/<a href="(https?:\/\/www\..{0,4}ghap.io\/#\/password-reset\?token=[\w-]{36})">/);
	var res = text.match(pattern);
	if (res) return res[1];
	return null
}

var passwordResetUrl;

function handlePasswordResetEmail(){

	var deferred = protractor.promise.defer();

	describe('Email with reset password URL', function() {

		beforeEach(logSpecFullName);
		afterEach(cancelCaseOnError.bind(this, deferred));

		var timeout_in_millis = 32*1000;
		it('- awaiting email', function () {
			browser.controlFlow()
				.wait(awaitEmailPromise, timeout_in_millis - 2000, 'Password reset email not received in time.')
				.then(function (email) {
					passwordResetUrl = extractPasswordResetUrl(email.text);
					expect(passwordResetUrl).not.toBeNull();
					console.log("Password reset url '%s'", passwordResetUrl)
				});
		}, timeout_in_millis);

		it( fulfillDeferSpecStr, fulfillDefer.bind(this,deferred) );

	});

	return deferred.promise;
}

function openPasswordResetPage(){

	var deferred = protractor.promise.defer();

	describe('Open password reset page',function(){

		beforeEach(logSpecFullName);
		afterEach(cancelCaseOnError.bind(this, deferred));

		it ('- validate URL and open page', function(){
			browser.get(passwordResetUrl);
			browser.getCurrentUrl().then(function(url){
				expect(url).toEqual(passwordResetUrl);
			});
		});

		it( fulfillDeferSpecStr, fulfillDefer.bind(this,deferred) );

	});

	return deferred.promise;
}

function putNewPasswordAndSubmit(){

	var deferred = protractor.promise.defer();
	var failFast = my.createFailFast(deferred);

	var password_input;
	var passwordConfirm_input;
	var submit_button;
	var alert_dialog;

	describe('Put new password',function(){

		beforeEach(logSpecFullName);
		failFast.setAfterEach();

		it ('- input fields and submit button should be present', function(){
			password_input = element(by.model('password'));
			passwordConfirm_input = element(by.model('passwordConfirmation'));
			submit_button = $('input[type="image"]');

			expect(password_input.isPresent()).toBe(true);
			expect(passwordConfirm_input.isPresent()).toBe(true);
			expect(submit_button.isPresent()).toBe(true);
		});

		it('- submit new password', function(){
			var new_password = '$sS'+my.dateTimeStr(new Date());
			password_input.sendKeys(new_password);
			passwordConfirm_input.sendKeys(new_password);
			awaitEmailPromise = getPasswordModifiedConfirmationEmail();
			my.preventAlertErrorOnSafari();
			submit_button.click();
			cfg.renewUserPassword(new_password);
		});

		//it('- no error should happens on submit', function(){
		//	var err_div = $("div.error-message-cont");
		//	err_div.isDisplayed().then(function(res){
		//		expect(res).toBeFalsy();
		//		if (res) {
		//			err_div.getText().then(function(text){
		//				console.log('Error:',text);
		//			})
		//		}
		//	});
		//});

		if (!browser.isSafari) {
			it ('- expect alert, validate alert text and accept', function(){

				var EC = protractor.ExpectedConditions;
				// Waits for an alert pops up.
				browser.wait(EC.alertIsPresent(), 3000)
					.then(function(){
						// https://github.com/angular/protractor/issues/1486
						return browser.switchTo().alert();
					})
					.then(function(alert){
						expect(alert.accept).toBeDefined();
						expect(alert.getText()).toContain('Password was reset.');
						alert.accept();
					})
			});
		}

		it('- expect redirect to login page', function(){
			browser.driver.wait(function(driver){
				return driver.isElementPresent(By.name('j_username'))
			},10000);
		});

		failFast.fulfillDeferSpec();

	});

	return deferred.promise;
}

function handlePasswordModifiedConfirmationEmail(){

	var deferred = protractor.promise.defer();

	describe('Email with Password Modified Confirmation', function() {

		beforeEach(logSpecFullName);
		afterEach(cancelCaseOnError.bind(this, deferred));

		var timeout_in_millis = 32*1000;
		it('- awaiting email, validate subject', function () {
			browser.controlFlow()
				.wait(awaitEmailPromise, timeout_in_millis - 2000, 'Password Modified Confirmation email not received in time.')
				.then(function (email) {
					expect(email.text).toBeDefined();
					console.log("Password Modified Conviramation Email received.");
				});
		});

		it( fulfillDeferSpecStr, fulfillDefer.bind(this,deferred) );

	});

	return deferred.promise;

}

function getPasswordModifiedConfirmationEmail() {

	var ghapEmailListiner = ghapEmails.createGhapEmailListener();

	ghapEmailListiner.startListening (function(mail, deferred ){
		var mail_domain = my.getDomainFromUrl(cfg.ghapUrl);
		if (mail.headers.from.indexOf(mail_domain) === -1) {
			console.log("Mail ignored since it is received not from '%s' domain.", mail_domain);
			return;
		}
		if (mail.subject.toLowerCase().indexOf('password modified confirmation') > -1)	{
			deferred.fulfill(mail);
		}
	});

	return ghapEmailListiner.deferred.promise

}


function finished(){
	ghapEmails.stopMailListener();
	console.log("Test case have finished.")
}