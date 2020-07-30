/**
 * Created by Vlad on 25.08.2015.
 */

function consoleLog(){console.log.apply(this, arguments)}

describe('Browse GHAP site', function() {

	it('should redirect to My Account after login', function() {
		// since oAuth login page does not use angular it is required to use webdriver directly
		// see http://angular.github.io/protractor/#/api?view=webdriver.By
		browser.driver.get('http://www.qa.ghap.io');
		//browser.driver.get('http://localhost:9000');

		// http://stackoverflow.com/questions/26411574/protractor-wait-for-element-to-become-invisible-hidden
		// http://stackoverflow.com/questions/28422011/how-to-wait-for-when-an-element-is-removed-from-dom
		//browser.driver.wait(protractor.until.elementLocated(By.name('j_username')), 10000);

		// awaiting oAuth login page page
		// or internal GHAP page with top menu
		browser.driver.wait(function(driver){
			return driver.isElementPresent(By.name('j_username'))
				.then(function(result){
					// if j_username field is present then current page is oAuth login page
					if (result) return result;
					// else sleep some time to allow complete a redirects
					return driver.sleep(3000).then( function(){
						// and check if top-nav class is present
						return driver.isElementPresent(By.className('top-nav'))
					})
				})
		},10000);

		//browser.driver.getPageSource().then(consoleLog);

		browser.driver.isElementPresent(By.name('j_username'))
			.then(function(result){

				// if j_username field is not present then current page internal GHAP page
				if (!result) {
					// log out if a user logged in and wait redirects completion
					browser.findElement(by.linkText('Logout')).click();
					browser.driver.sleep(3000);
				}

				// login as PSTester
				browser.driver.findElement(By.name('j_username')).sendKeys('PSTester');
				var password_input = browser.driver.findElement(By.name('j_password'));
				password_input.clear();
				password_input.sendKeys("");
				browser.driver.findElement(By.className('submit-button')).click();
				browser.driver.sleep(3000);

				browser.getCurrentUrl().then(consoleLog);
			});

	});

});
