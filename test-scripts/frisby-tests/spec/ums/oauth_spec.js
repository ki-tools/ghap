oAuth = require('./ums_oauth');
oAuth.waitAccessToken(function(){
	console.log('Test finished.')
});
