/**
 * Created by Vlad on 02.09.2015.
 */

// http://stackoverflow.com/questions/29311154/fetching-values-from-email-in-protractor-test-case

function getLastEmail() {
	var deferred = protractor.promise.defer();
	console.log("Waiting for an email...");
	mailListener.start();

	mailListener.on("mail", function(mail){
		deferred.fulfill(mail);
		console.log('Mail received.');
	});

	return deferred.promise;
}

describe("Forgot password test case", function () {

	beforeEach(function () {
		browser.driver.get('http://www.qa.ghap.io');
	});

	afterEach(function(){
		mailListener.stop();
		console.log('Listener shutdown.');
	});

	it("should ...", function () {

		browser.controlFlow().await(getLastEmail()).then(function (email) {
			console.log(email.subject);
			// console.log(email.headers);
			console.log(email.text);
		});
	});
});