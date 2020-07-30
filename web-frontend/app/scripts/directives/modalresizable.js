'use strict';

/**
 * @ngdoc directive
 * @name bmgfApp.directive:resizable
 * @description
 * # resizable
 */
angular.module('bmgfApp')
  .directive('modalresizable', function ($timeout) {
    var resizableConfig = {
      handles: 'e, s, se, nw'
    };

    return {
        restrict: 'A',
        scope: {
            callback: '&onResize'
        },
        link: function postLink(scope) {
            var element= angular.element(document.getElementsByClassName('modal-dialog'));


            var calcPosition = function calcPosition(){
              var forCenterEl = angular.element(document.getElementsByClassName('for-center'));
              var content = angular.element(document.getElementsByClassName('modal-content'));
              var position = content.offset();

              forCenterEl.css({
                left: 0
              });

              element.css({
                left: position.left + 'px'
              });

            };

            $timeout(calcPosition, 100);

            $(element).resizable(resizableConfig);
            $(element).on('resizestop', function () {
                if (scope.callback){
                  scope.callback();
                }
            });
        }
    };
  });
