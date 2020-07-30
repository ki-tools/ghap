'use strict';

/* globals $ */

/**
 * @ngdoc directive
 * @name bmgfApp.directive:ngConfirmClick
 * @description
 * # ngConfirmClick
 */
angular.module('bmgfApp')
  .directive('ngConfirmClick', function () {
    return {
        link: function (scope, element, attr) {
            var msg = attr.ngConfirmClick || 'Are you sure?';
            var clickAction = attr.confirmedClick;
            element.bind('click', function () {
                if ( $(this).is(':not([disabled])') && window.confirm(msg) ) {
                    scope.$eval(clickAction);
                }
            });
        }
    };
  });
