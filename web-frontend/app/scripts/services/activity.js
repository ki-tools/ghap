'use strict';

/**
 * @ngdoc service
 * @name bmgfApp.Activity
 * @description
 * # Activity
 * Factory in the bmgfApp.
 */
angular.module('bmgfApp')
  .factory('Activity', function ($resource, Settings) {
    return $resource(Settings.ACTIVITY_ROOT + '/Activity/:action/:field/:id', 
      {id:'@id', action:'@action'},
      {
        query:   {method: 'GET', params: {action: 'get'}, cache: false, isArray: true},
        getById: {method: 'GET', params: {action: 'get', field: 'id'}, cache: false},
        delete:  {method: 'DELETE', params: {action: 'delete', field: 'id'}, cache: false},
        create:  {method: 'PUT', params: {action: 'create'}},
        save:    {method: 'PUT', params: {action: 'update', id: ''}}
      });
  });
