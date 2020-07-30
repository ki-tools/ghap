'use strict';

/**
 * @ngdoc directive
 * @name bmgfApp.directive:access
 * @description
 * # access
 */
angular.module('bmgfApp')
  .directive('access', function (Auth) {
    return {
      restrict: 'A',
      scope: {
        access: '='
      },
      link: function (scope, element) {
        if (scope.access !== undefined && scope.access.requiredRoles !== undefined && scope.access.requiredRoles.length > 0) {
          // element.addClass('hidden');
          Auth.authorize(true, scope.access.requiredRoles, scope.access.roleCheckType, function(result) {
            if (result === 'authorised') {
              element.removeClass('hidden');
            } else {
              element.addClass('hidden');
            }
          });
        } else {
          element.removeClass('hidden');
        }
      }
    };
  });
