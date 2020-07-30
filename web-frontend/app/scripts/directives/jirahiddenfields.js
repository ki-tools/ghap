'use strict';

/**
 * @ngdoc directive
 * @name bmgfApp.directive:jiraHiddenFields
 * @description
 * # jiraHiddenFields
 */
angular.module('bmgfApp')
  .directive('jiraHiddenFields', function ($window) {
    return {
      template: '<input name="Type" ng-value="type" type="hidden"/>' +
          '<input name="CertaraProduct" value="GHAP" type="hidden"/>' +
          '<input name="Customer" value="BMGF" type="hidden"/>' +
          '<input name="Version" ng-value="version" type="hidden"/>' +
          '<input name="ProductEnvironment" ng-value="environment" type="hidden"/>',
      scope: {
        type: '@type'
      },
      restrict: 'E',
      link: function postLink(scope) {
        scope.version = '{{ VERSION }}';

        var result = new UAParser($window.navigator.userAgent).getResult();

        var os  = '';
        if(result.os && result.os.name){
          os = 'OS: ' + result.os.name + (result.os.version ? (' '+ result.os.version):'');
          os += (result.cpu && result.cpu.architecture) ? '(' + result.cpu.architecture + '), ':', ';
        }

        var device = '';
        if(result.device && result.device.model){
          device = 'Device: ' + result.device.model + ', ';
        }

        var browser = 'User Agent:' + $window.navigator.userAgent;
        if(result.browser && result.browser.name){
          browser = 'Browser: ' + result.browser.name + ' ' + result.browser.version;
        }
        scope.environment = device + os + browser;
      }
    };
  });
