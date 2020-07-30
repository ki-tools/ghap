'use strict';

/**
 * @ngdoc service
 * @name bmgfApp.focus
 * @description
 * # focus
 * Service in the bmgfApp.
 */
angular.module('bmgfApp')
  .factory('focus', function ($rootScope, $timeout) {
    return function(name) {
      $timeout(function (){
        $rootScope.$broadcast('focusOn', name);
      });
    };
  });