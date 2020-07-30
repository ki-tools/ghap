'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:FooterCtrl
 * @description
 * # FooterCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('FooterCtrl', function ($scope, $modal) {
  	$scope.showTerms = function () {
  		$('body').addClass('body-terms-modal');
        var modalInstance = $modal.open({
          templateUrl: 'views/terms-modal.html',
          // windowTemplateUrl: 'views/not-modal/window.html',
          controller: 'FooterModalCtrl',
          backdrop: true,
          scrollableBody: true,
          windowClass: 'terms-modal',
          openedClass: 'body-terms-modal',
          size: 'lg'
        });
        modalInstance.result.then(function () {
          $('body').removeClass('body-terms-modal');
        }, function () {
          $('body').removeClass('body-terms-modal');
        });
    };
  })
  .controller('FooterModalCtrl', function ($scope, $modalInstance) {
    $scope.dismiss = function dismiss(){
      $modalInstance.dismiss('cancel');
    };
  })
  ;