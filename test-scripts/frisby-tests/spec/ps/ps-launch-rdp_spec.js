/**
 * Created by Vlad on 18.08.2015.
 */

var my = require('../Common/ghap-lib');
my.stepPrefix = 'LaunchRDP';
my.logModuleName(module.filename);

var umsRequests = require('../ums/ums_requests');

var psRequests = require('./ps_requests');
var Tester = require('./ps_tester').make();
var testerCompResources = [];

var oAuthService = require('./../oauth/oauth_promise');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(Tester.getName(), Tester.getPassword())
    .then(umsRequests.validateToken)
    .then(function(){return umsRequests.pullUser(oAuth, Tester)})
    .then(runSuite)
    .catch(my.reportError);

function runSuite() {
    return psRequests.multiVpgGetComputeResources4User(oAuth.header, Tester, testerCompResources)
        .then(function(){
            return findRunningWinInstance(testerCompResources);
        })
        .then(function(win_instance){
            return psRequests.multiVpgGetRdpFile(oAuth.header, win_instance )
                .then(function(){
                    return win_instance.address;
                })
        })
        .then(validateWinVPG)
}

/**
 * Find running windows instance in the compute_resources array and return it IP
 * or throw error if instance not found
 * @param {[computeResource]} compute_resources
 * @returns {string} IP address
 */
function findRunningWinInstance(compute_resources){
    var winInstance = my.findElementInArray(compute_resources, 'instanceOsType','Windows');
    expect(winInstance).not.toBeNull();
    if (winInstance){
        console.log("Windows instance found:");
        console.log(winInstance);
        if (winInstance.status !== 'running') {
            throw new Error('Windows Instance does not running.')
        }
    } else {
        throw new Error('Windows Instance not found.')
    }
    return winInstance;
}

function validateWinVPG(winVPG_ip){

    var rdp = require('node-rdpjs');

    var bitmapIsReceived = false;
    var errIsOccurred = false;

    console.log('\nCreate RDP connection.');
    var rdpClient = rdp.createClient({
        domain : 'PROD',
        userName : Tester.getName(),
        password : Tester.getPassword(),
        enablePerf : true,
        autoLogin : true,
        decompress : false,
        screen : { width : 800, height : 600 },
        locale : 'en',
        logLevel : 'INFO'
    }).on('connect', function () {
        console.log('Connected.');
    }).on('close', function() {
        console.log('Connection closed.')
    }).on('bitmap', function(bitmap) {
        if (!bitmapIsReceived) {
            bitmapIsReceived = true;
            console.log('First bitmap data received.');
        }
    }).on('error', function(err) {
        errIsOccurred = true;
        console.error(err);
    });

    rdpClient.connect(winVPG_ip, 3389);

    describe('Test RDP connection.', function(){

        beforeEach(my.logSpecFullName);

        it('Awaiting connected status.', function(){
            waitsFor(function(){ return rdpClient.connected },"RDP connection.", 5000);
            runs(function(){expect(rdpClient.connected).toBe(true);})
        });

        it('Awaiting bitmap data.', function(){
            if (rdpClient.connected) {
                waitsFor(function(){return bitmapIsReceived},"First bitmap data.", 1000);
                runs(function(){expect(bitmapIsReceived).toBe(true);})
            }
        });

        const CHECK_AFTER = 15000;
        it('Validate connection status after '+CHECK_AFTER/1000+' sec.', function(){
            if (rdpClient.connected) {
                console.log("Pause for %d sec.",CHECK_AFTER / 1000);
                var start_time_ms = new Date();
                var interval_is_finished = false;
                runs(function(){
                    setTimeout(function(){interval_is_finished = true}, CHECK_AFTER)
                });
                waitsFor(function(){return errIsOccurred || interval_is_finished},'Pause for ' + CHECK_AFTER/1000 + ' sec.', CHECK_AFTER+300);
                runs(function(){
                    var exec_time_ms = new Date() - start_time_ms;
                    expect(errIsOccurred).toBe(false);
                    if (errIsOccurred) {
                        console.log("ERROR: Pause cancelled due connection error after %s.",my.logTime(exec_time_ms));
                    }
                    else {
                        console.log("Check connection status. %s", rdpClient.connected ? 'OK.' : "FAIL.")
                        expect(rdpClient.connected).toBe(true);
                    }
                });
            }
        });

    });

    describe('Close RDP connection.', function(){
        it('Awaiting closed status.', function(){
            // call socket.destroy to prevent read ECONNRESET error at TCP.onread
            rdpClient.bufferLayer.socket.destroy();
            errIsOccurred = false;
            rdpClient.close();
            waits(1000);
            runs(function(){
                expect(rdpClient.connected).not.toBe(true);
            });
        });

        it('Check that no error happens on close event.', function(){
            runs(function(){
                expect(errIsOccurred).not.toBe(true);
            });
        })
    });
}