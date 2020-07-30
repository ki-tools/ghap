'use strict';

/**
 * @ngdoc service
 * @name bmgfApp.ActivityRoleAssociation
 * @description
 * # ActivityRoleAssociation
 * Service in the bmgfApp.
 */
angular.module('bmgfApp')
  .factory('ActivityRoleAssociation', function ($resource, Settings, $http) {
    var res = $resource(Settings.ACTIVITY_ROOT + '/ActivityRoleAssociation/:action/:guid',
      {guid:'@guid', action:'@action'},
      { 
        query: {method: 'GET', params: {action: 'get'}, cache: false, isArray: true},
        // delete: {method: 'DELETE', params: {action: 'delete'}},
        save: {method: 'PUT', params: {action: 'create'}, isArray: true} 
      }
    );

    res.delete = function(role, callback) {
      $http.delete(Settings.ACTIVITY_ROOT + '/ActivityRoleAssociation/delete/' + role.guid, {data: null, cache: false})
        .success(function(data) {
          callback(data);
        })
        .error(function(data, status) {
          callback(data, status);
        });
    };

    return res;
  });
