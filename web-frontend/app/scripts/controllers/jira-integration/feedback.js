'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:FeedbackctrlCtrl
 * @description
 * # FeedbackctrlCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('FeedbackCtrl', function ($scope, $modal, $document, $timeout, $log) {
    $scope.openModal = function () {

        var modalInstance = $modal.open({
          templateUrl: 'views/jira-integration/feedback.html',
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
