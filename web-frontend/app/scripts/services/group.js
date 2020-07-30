'use strict';

/**
 * @ngdoc service
 * @name bmgfApp.group
 * @description
 * # group
 * Factory in the bmgfApp.
 */
angular.module('bmgfApp')
.factory('Group', function ($resource, Settings) {

  var res = $resource(Settings.API_ROOT + '/group/:action/:dn/:udn',
    {dn:'@dn', action:'@action', udn: '@udn'},
    {
      query: {method: 'GET', params: {action: 'all', dn: 'default'}, cache: false, isArray: true},
      add: {method: 'GET', params: {action: '@dn', dn: 'add'}, cache: false},
      remove: {method: 'GET', params: {action: '@dn', dn: 'delete'}, cache: false},
      addMember: {method: 'GET', params: {action: '@dn', dn: 'add'}, cache: false},
      deleteMember: {method: 'GET', params: {action: '@dn', dn: 'delete'}, cache: false}
    }
  );

  res.getUsers = function getUsers(group){
    /*jshint unused:false*/
    return [];
  };

  res.assignUsers = function assignUsers(group, users, successCallback, errorCallback){
    /*jshint unused:false*/
    // assign users

    // delete users
  };

  return res;
});
