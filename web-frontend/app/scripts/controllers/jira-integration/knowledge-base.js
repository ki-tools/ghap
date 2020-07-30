'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:KnowledgeBasectrlCtrl
 * @description
 * # KnowledgeBasectrlCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
	.controller('KnowledgeBaseCtrl', function($scope, $window, Settings, $sce) {
		$scope.openTab = function() {
			var accessToken = localStorage.getItem('access_token');
			var redirectUrl = Settings.KNOWLEDGE_BASE_URL;
			if (!redirectUrl) {
				if (console) {
					console.error('Settings.KNOWLEDGE_BASE_URL is not configured. Default URL is used');
				}
				redirectUrl = 'https://www.google.com';
			}
			$window.open($sce.trustAsResourceUrl(redirectUrl + '?access_token=' + accessToken), '_blank')
		};
	});