/**
 * Created by Vlad on 24.11.2015.
 */
var assert = require('assert');

var ghapE2EIni = require('./../ghap-e2e-ini');
var my = require('./../ghap-e2e-lib');

describe("getDomainFromUrl", function(){
    var url = 'http://www.qa.ghap.io';
    var domain = 'qa.ghap.io';
    it("- for '" + url + "' should return '" + domain + "'", function(){
        var res = my.getDomainFromUrl(url);
        assert.equal(res, domain);
    })
});

describe("ghapE2E-Ini", function() {
    var e2e_ini;
    var new_password = '$sS'+my.dateTimeStr(new Date());
    var env_name = 'tst';

    it("- get should return non empty map", function () {
        e2e_ini = ghapE2EIni.get(env_name);
        assert(e2e_ini);
    });

    it('- save should save new password', function(){
        e2e_ini.user.password = new_password;
        ghapE2EIni.save(e2e_ini, env_name);
        assert(new_password);
    });

    it('- get should read new saved password', function(){
        e2e_ini = ghapE2EIni.get(env_name);
        assert.equal(e2e_ini.user.password, new_password);
    });
});