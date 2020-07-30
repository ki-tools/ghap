var my = require('./ums_common');
var endOfLine = my.endOfLine;

var frisby = require('frisby');

var useOAuth = true;
var oAuth;
var authHeader = {
	Name: '',
	Value: ''
};

var umsUser = require('./UmsUser');
var testUser = umsUser.makeUser('Administrator','','email','password','GHAP Administrator');

var umsRole = require('./ums_role');
var testRole = umsRole.create('GHAP Administrator','description')
var roleCRUD = require('./ums_role_crud');

oAuth = require('./oauth_spec');
oAuth.waitAccessToken(runSuite);

function runSuite() {
	authHeader.Name = 'Authorization';
	authHeader.Value = oAuth.access_token;

	roleCRUD.setRoleToUser(authHeader, testUser, testRole, function(){})
}

