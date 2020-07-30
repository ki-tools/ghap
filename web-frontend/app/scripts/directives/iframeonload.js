'use strict';

/**
 * @ngdoc directive
 * @name bmgfApp.directive:iframeOnload
 * @description
 * # iframeOnload
 */
angular.module('bmgfApp')
  .directive('iframeOnload', function () {
    return {
      scope: {
          callback: '&iframeOnload'
      },
      link: function(scope, element){
        element.on('load', function(event){
          return scope.callback({
            event: event
          });
        });
        scope.$on('$destroy', function() { element.off('load'); });
      }
    };
  });
