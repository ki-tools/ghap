'use strict';

/**
 * @ngdoc service
 * @name bmgfApp.PersonalStorage
 * @description
 * # PersonalStorage
 * Service in the bmgfApp.
 */
angular.module('bmgfApp')
  .factory('PersonalStorage', function ($resource, $http, Settings) {
    var res =  $resource(Settings.PROVISIONING_ROOT + '/PersonalStorage/:action/:guid/:size',
      {guid:'@guid', action:'@action', size:'@size'},
      { 
        query:   {method: 'GET', params: {action: 'get'}, cache: false, isArray: false},
        create:  {method: 'PUT', params: {action: 'create'}},
        delete:  {method: 'GET', params: {action: 'delete'}, cache: false},
        exists:  {method: 'GET', params: {action: 'exists'}, cache: false, isArray: false}
      }
    );

    return res;
  });
