/**
 * Created by Vlad on 13.06.2015.
 */

var autoTesters = require('./ghap-autotesters');
autoTesters.login().then(function () {
	console.log('Script finished.')
});
