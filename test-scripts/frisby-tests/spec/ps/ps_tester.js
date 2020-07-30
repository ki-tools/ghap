/**
 * Created by Vlad on 09.12.2015.
 */

var cfg = require('./../Common/ghap-config');
var umsUser = require('./../ums/ums_user');
/**
 * @returns {UmsUser}
 */
exports.make = function() {
    return umsUser.makeUser(
        'Vlad Ruzov',
        'auto.tester',
        'vlad.ruzov@ontarget-group.com',
        cfg.psTesterPassword,
        'vlad.ruzov.auto.tester');
};