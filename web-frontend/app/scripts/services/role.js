'use strict';

/**
 * @ngdoc service
 * @name bmgfApp.Role
 * @description
 * # Role
 * Factory in the bmgfApp.
 */
angular.module('bmgfApp')
.factory('Role', function ($resource, Settings) {

  var res = $resource(Settings.API_ROOT + '/role/:action/:dn/:udn',
    {dn:'@dn', action:'@action', udn: '@udn'},
    {
      query: {method: 'GET', params: {action: 'all', dn: 'default'}, cache: false, isArray: true},
      add: {method: 'GET', params: {action: '@dn', dn: 'add'}, cache: false},
      remove: {method: 'GET', params: {action: '@dn', dn: 'delete'}, cache: false}
    }
    // ,
    // {
    //   addUser: {method: 'GET', params: {action: '@dn', dn: 'add', udn: '@udn'}, cache: false}
    // },
    // {
    //   removeUser: {method: 'GET', params: {action: '@dn', dn: 'delete', udn: '@udn'}, cache: false}
    // }
  );

  res.buildInRoles = {
    ADMINISTRATOR: 'GHAP Administrator',
    CURATOR:       'Data Curator',
    ANALYST:       'Data Analyst',
    CONTRIBUTOR:   'Data Contributor',
  };

  return res;

});
