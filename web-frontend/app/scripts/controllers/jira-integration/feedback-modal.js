'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:FeedbackModalCtrl
 * @description
 * # FeedbackModalCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('FeedbackModalCtrl', function ($scope, $modalInstance, User, Settings, $sce, $timeout, $http) {
    User.getCurrentUser(function getCurrentUserCallback(user){
      $scope.user = user;
    });

    $scope.onlyCloseButton = false;

    /*jshint camelcase: false */
    $scope.jira_api_url = $sce.trustAsResourceUrl(Settings.JIRA_API_URL);
    if( !$scope.jira_api_url ){
      if(console){
        console.error('Settings.JIRA_API_URL is not configured. Default URL is used');
      }
      $scope.jira_api_url = $sce.trustAsResourceUrl('http://www.tripos.com/web-services/submit-error-bmgf.php');
    }

    $scope.errors = [];

    $scope.alertResult = function(event){
       /*jshint unused:false*/

      //console.log('---->', event);
    };


    $scope.submissionInProgress = false;

    $scope.isSubmissionInProgress = function(){
      return $scope.submissionInProgress;
    };

    $scope.submit = function submit(form){
      $scope.errors = [];
      $scope.submissionInProgress = true;

      var formdata = new FormData();
      //need to convert our json object to a string version of json otherwise
      // the browser will do a 'toString()' on the object which will result
      // in the value '[Object object]' on the server.
      var jiraFieldsToPopulate = ["Type", "CertaraProduct", "Customer", "Version", "ProductEnvironment",
        "Abstract", "Severity", "Description", "ProductUser", "ErrorSubmitter", "Label", "SeverityCategory", "issueData[]"];

      for (var fieldIndex = 0; fieldIndex < jiraFieldsToPopulate.length; fieldIndex++) {
        var jiraFieldName = jiraFieldsToPopulate[fieldIndex];
        var matchedElements = document.getElementsByName(jiraFieldName);
        if (matchedElements != 'undefined' && matchedElements.length > 0) {
          for (var matchedElementIndex = 0; matchedElementIndex < matchedElements.length; matchedElementIndex++) {
            var formElement = matchedElements[matchedElementIndex];

            if (formElement.type === "file") {
              for (var fileIndex = 0; fileIndex < formElement.files.length; fileIndex++) {
                formdata.append(jiraFieldName, formElement.files[fileIndex]);
              }

            } else {
              formdata.append(jiraFieldName, formElement.value);
            }



          }
        }
      }


      var config = {
        headers : {
          'Content-Type': undefined
        },
        withCredentials : false,
        transformRequest: angular.identity
      };


      $http.post($scope.jira_api_url, formdata, config)
          .then(function successCallback(response) {
            $scope.handleSubmissionResponse(response, form);

          }, function errorCallback(response) {
            $scope.handleSubmissionResponse(response, form);
          }
      );

    };

    $scope.handleSubmissionResponse = function handleSubmissionResponse(response, form) {
      $scope.submissionInProgress = false;
      console.log(response);

      var userMessage;
      var submissionError = false;

      if (response.status === 200) {
        console.log('status is 200');
        console.log(typeof response.data);

        if (response.data.indexOf("Success") !== -1) {
          submissionError = false;
          userMessage = "Submission successful.";

        } else {
          submissionError = true;
          userMessage = "An error occurred during submission.";
        }
      } else {
        submissionError = true;
        userMessage = "An error occurred during submission.";
      }

      if (submissionError) {
        $scope.errors = [userMessage];
      } else {
        $modalInstance.close(form);
      }

    };

    $scope.dismiss = function dismiss(){
      $modalInstance.dismiss('cancel');
    };
  })
;
