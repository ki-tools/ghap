'use strict';

/**
 * @ngdoc service
 * @name bmgfApp.Grant
 * @description
 * # Grant
 * Factory in the bmgfApp.
 */
angular.module('bmgfApp')
  .factory('Grant', function ($resource, Settings) {
    return $resource(Settings.PROJECT_ROOT + '/project/:project/:id/:action',
      { id:'@id', action:'@action', project: 'grant'},
      {
        save: {method:'POST', params: {id: null, action: null}},
        update: {method:'PUT', params: {id: null, action: null}},
        forProject: {method:'GET', params: {action: 'grants', project: '@id', id: null}, cache: false, isArray: true},
        delete: {method: 'DELETE', params: {action: null}, cache: false},
      }
    );
  });
