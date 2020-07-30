'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:BugReportCtrl
 * @description
 * # BugReportCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('BugReportCtrl', function ($scope, $modal, $log) {
    $scope.openModal = function () {

        var modalInstance = $modal.open({
          templateUrl: 'views/jira-integration/bug-report.html',
          windowTemplateUrl: 'views/not-modal/window.html',
          controller: 'FeedbackModalCtrl',
          backdrop: false,
          scrollableBody: true,
          size: 'lg'
        });

        modalInstance.result.then(function (form) {
          $log.debug('Submit form: ', form);
        }, function () {
          $log.info('Modal dismissed at: ' + new Date());
        });

    };
  })
;
