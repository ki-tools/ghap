'use strict';

/**
 * @ngdoc service
 * @name bmgfApp.project
 * @description
 * # project
 * Factory in the bmgfApp.
 */
angular.module('bmgfApp')
  .factory('Project', function ($resource, Settings, Grant) {
    var res = $resource(Settings.PROJECT_ROOT + '/project/:id/:entityName/:userId',
      { id:'@projectId', entityName:'@entityName', userId:'@userId' },
      {
        update:      {method: 'PUT'},
        delete: {method: 'DELETE', params: {entityName: null, userId: null, id: '@id'}, cache: false},
        addGrant:  {method: 'POST', params: {entityName: 'grant'}, cache: false},

        getUserPrograms:      {method: 'GET', cache: false, isArray: true},
        getUserProgramGrants: {method: 'GET', params: {entityName: 'grants'}, cache: false, isArray: true},

        getGroupPrograms:      {method: 'GET', cache: false, isArray: true},
        getGroupProgramGrants: {method: 'GET', params: {entityName: 'grants'}, cache: false, isArray: true},

        getProgramUsers:      {method: 'GET', params: {entityName: 'users'}, cache: false, isArray: true},
        getProgramGrantUsers: {method: 'GET', params: {id: 'grant', entityName: '@guid', userId: 'users'}, cache: false, isArray: true},

        grantProgramPermissions:  {method: 'POST', params: {entityName: 'grantProjectPermissions'},  cache: false},
        revokeProgramPermissions: {method: 'POST', params: {entityName: 'revokeProjectPermissions'}, cache: false},

        grantGrantPermissions:  {method: 'POST', params: {entityName: 'grantGrantPermissions'},  cache: false},
        revokeGrantPermissions: {method: 'POST', params: {entityName: 'revokeGrantPermissions'}, cache: false},
      }
    );

    res.getGrants = function getGrants(program, callback, errorCallback){
      Grant.forProject(program, callback, errorCallback);
    };

    return res;
  });
