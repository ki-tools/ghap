/**
 * Created by Vlad on 07.10.2015.
 */

var my = require('./ghap-e2e-lib');

exports = module.exports = {};

exports.logOut = function(){

    var deferred = protractor.promise.defer();
    var failFast = my.createFailFast(deferred);

    describe('Log out from GHAP account', function() {

        console.log();
        beforeEach(my.logSpecFullName);
        failFast.setAfterEach();

        it("- 'Logout' link should be present", function(){
            expect(element(by.linkText('Logout')).isPresent()).toBe(true);
        });

        it("- click on 'Logout' link should open the GHAP login page", function () {
            browser.findElement(by.linkText('Logout')).click();
            browser.driver.wait(
                function (driver) {
                    return browser.driver.isElementPresent(By.name('j_username'))
                }, 10000)
                .then( function () {
                    expect(browser.driver.isElementPresent(By.name('j_username'))).toBe(true);
                });
        });

        failFast.fulfillDeferSpec();

    });

    return deferred.promise;

};