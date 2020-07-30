/**
 * Created by Vlad on 03.09.2015.
 */

var path = require('path');
var my = require('./ghap-e2e-lib');
var cfg = require('./ghap-e2e-config');

exports = module.exports = {};

exports.openLoginPageSpec = function(){

	return my.createStdSuite('Open GHAP Login Page', function() {

		it('- should redirect to oAuth login page or to internal GHAP page', function() {

			// since oAuth login page does not use angular it is required to use webdriver directly
			// see http://angular.github.io/protractor/#/api?view=webdriver.By
			browser.driver.get(cfg.ghapUrl);
			//browser.driver.get('http://localhost:9000');

			// awaiting oAuth login page page
			// or internal GHAP page with top menu

			browser.driver.wait(function(driver){
				return driver.isElementPresent(By.name('j_username'))
					.then(function(result){
						// if j_username field is present then current page is oAuth login page
						if (result) {
							console.log('Username field is found.');
							return result;
						}

                        return driver.sleep(3000)
							.then( function(){
								return driver.isElementPresent(By.linkText('Logout'))
									.then(function(result){
										if (result) console.log("Logout link is found.");
										return result;
									})
							});

					})
			},30000);

			//browser.driver.sleep(300);

			browser.driver.isElementPresent(By.name('j_username'))
				.then(function(result){
					// if j_username field is not present then current page internal GHAP page
					if (!result) {
						// log out if a user logged in and wait redirects completion
						console.log("Click 'Logout' to leave user account. ");
						browser.driver.findElement(By.linkText('Logout')).click()
							.then(function(){

								browser.driver.wait(function(driver){
									return driver.sleep(1000)
										.then(function(){
											return browser.driver.isElementPresent(By.name('j_username'))
												.then(function(result){
													if (result) console.log('Username field is found after logout.');
													return result;
												})
										});

								},10000);

							})
					}
				});

			browser.driver.sleep(3000)
				.then(function(){
					browser.driver.getCurrentUrl()
						.then(function(url){
							var oauth_url_pattern;
							if (browser.isSafari)
								oauth_url_pattern = new RegExp(/^https?:\/\/oauth.+login$/);
							else
								oauth_url_pattern = new RegExp(/^https?:\/\/oauth.+auth#$/);
							console.log("Current url is '%s' (dirname is '%s') ",url, path.dirname(url));
							expect(path.dirname(url)).toMatch(oauth_url_pattern);
						});
				});
		});

	});
};

exports.loginAs = function(username, password){

	return my.createStdSuite("Login as "+username+" with password '"+password+"'", function(){

		it('- should redirect to a internal GHAP page.', function(){

			var username_input = browser.driver.findElement(By.name('j_username'));
			username_input.clear();
			username_input.sendKeys(username);
			var password_input = browser.driver.findElement(By.name('j_password'));
			password_input.clear();
			password_input.sendKeys(password);
			browser.driver.findElement(By.className('submit-button')).click();

			browser.driver.sleep(3000);
			browser.driver.getCurrentUrl().then(function(url){
				//console.log("%s page is open.",url);
				expect(url).not.toContain('oauth');
			});

		});

	});
};

exports.expectFailedLogin = function(username, password){

	return my.createStdSuite("Expect fail on login as "+username+" with password '"+password+"'", function(){

		it('- should remain on the login page and display error', function(){

			var username_input = browser.driver.findElement(By.name('j_username'));
			username_input.clear();
			username_input.sendKeys(username);
			var password_input = browser.driver.findElement(By.name('j_password'));
			password_input.clear();
			password_input.sendKeys(password);
			browser.driver.findElement(By.className('submit-button')).click();

			browser.driver.sleep(1000);
			browser.driver.getCurrentUrl().then(function(url){
				//console.log("%s page is open.",url);
				expect(url).toContain('oauth');
				expect(url).toContain('login_error');
				browser.driver.findElement(By.className('error-message-cont'))
					.getText()
					.then(function(text){
						console.log(text);
						expect(text).toContain('Log in failed.');
					})
			});

		});

	});
};
