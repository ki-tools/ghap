/**
 * Created by Vlad on 29.08.2016.
 */

var chai = require("chai");
var chaiAsPromised = require("chai-as-promised");
chai.use(chaiAsPromised);

require('./auth_suite');
require('./ce-page_suite');
var cfg = require('./../../frisby-tests/spec/Common/ghap-config');
if (cfg.environment == 'samba')
    require('./vz-apps_suite');
