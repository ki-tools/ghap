'use strict';

/**
 * @ngdoc service
 * @name bmgfApp.Stack
 * @description
 * # Stack
 * Service in the bmgfApp.
 */
angular.module('bmgfApp')
  .factory('Stack', function ($resource, $http, Settings) {
    var res =  $resource(Settings.PROVISIONING_ROOT + '/MultiVirtualPrivateGrid/:action/:guid',
      {guid:'@guid', action:'@action'},
      { 
        query:   {method: 'GET', params: {action: 'get'}, cache: false, isArray: true},
        create:  {method: 'PUT', params: {action: 'create-multiple'}, cache: false, isArray: true},
        exists:  {method: 'GET', params: {action: 'exists'}, cache: false, isArray: false},
        
        pause:   {method: 'PUT', params: {action: 'pause'}, cache: false, isArray: false},
        resume:  {method: 'PUT', params: {action: 'resume'}, cache: false, isArray: false},
      }
    );

    res.getRdp = function(resource, callback)  {
      $http.put(Settings.PROVISIONING_ROOT + '/MultiVirtualPrivateGrid/rdp', resource, {cache: false})
        .success(function(data) {
          callback(data);
        })
        // .error(function(data, status) {
        //   console.error('Cannot get rdp', data, status);
        // })
        ;
    };

    res.status = function status(guid, data, callback){
      $http.get(Settings.PROVISIONING_ROOT + '/MultiVirtualPrivateGrid/status/' + guid, data, {cache: false}).
        success(function(data) {
          callback(data);
        }).
        error(function(data, status) {
          //console.error('Cannot get Stack status for ' + guid + '. Response: ' + status);
          callback(data, status);
        });
    };

    res.prototype.pemUrl = function pemUrl(){
      var accessToken = localStorage.getItem('access_token');
      return Settings.PROVISIONING_ROOT + '/MultiVirtualPrivateGrid/pem/' + this.userId + '/' + this.activityId + '?token=' + accessToken;
    };

    res.rdpFileUrl = function(r){
      var accessToken = localStorage.getItem('access_token');
      return Settings.PROVISIONING_ROOT + '/MultiVirtualPrivateGrid/rdp-file/' + encodeURIComponent(r.instanceId) + '/' + 
      encodeURIComponent(r.instanceOsType)  +
      '?token=' + accessToken +
      '&ipAddress=' + encodeURIComponent(r.address) +
      '&dnsName=' + encodeURIComponent(r.dnsname);
    };

    res.terminate = function terminate(guid, activityId, callback){
      $http.delete(Settings.PROVISIONING_ROOT + '/MultiVirtualPrivateGrid/terminate/' + guid + '/' + activityId, {data: null, cache: false}).
        success(function(data) {
          if(callback){
            callback(data);
          }
        }).
        error(function(data, status) {
          console.error('Cannot terminate Stack for ' + guid + '. Response: ' + status);
          if(callback){
            callback(null);
          }
        });
    };

    res.computeResources = function computeResources(user, success, error){
      $http.get(Settings.PROVISIONING_ROOT + '/MultiVirtualPrivateGrid/get/user/compute' + (user ? ('/'+user.guid):''), {cache: false})
        .success(success)
        .error(error);
    };

    return res;
  });
