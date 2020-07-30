var cfg = require('./../Common/ghap-config');
var umsUser = require('./../ums/ums_user');
/**
 * @type {UmsUser}
 */
module.exports = umsUser.makeUser(
	'Vlad Ruzov',
	'Admin',
	'vlad.ruzov@ontarget-group.com',
	cfg.testerAdminPassword,
	''
);
//module.exports = umsUser.makeUser(
//	'Administrator',
//	'',
//	'administrator@ghap.io',
//	'');
//module.exports = umsUser.makeUser(
//	'GHAPAdministrator',
//	'',
//	'GHAPAdministrator@ghap.io',
//	'');
