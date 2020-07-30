'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:UsageReportsSystemReportsCtrl
 * @description
 * # UsageReportsSystemReportsCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('UsageReportsSystemReportsCtrl', function ($scope, Report, User, $interval, $modal, $timeout) {

    $scope.user = {};
    $scope.token = '';
    $scope.reportGroups = [];
    $scope.reports = [];

    $scope.hints = {
      'User Accounts':        'Lists Username, First Name, Last Name, Email, Account Status',
      'Group Membership':     'Lists Group, Username, First Name, Last Name, Email, Account Status',
      'Role Membership':      'Lists Role  Username, First Name, Last Name, Email, Account Status',
      'Programs':             'Lists Program, Username, Email, Permissions',
      'Grants':               'Lists Grant, Program, Username, Email, Permissions',
      'Compute Environments':  'Username, User Email, User Status, Activity Name, InstanceId, Environment Commissioned - Duration (Hours), Environment Running - Duration (Hours), Environment Stopped - Duration (Hours), Environment Creation Date, Environment Termination Date, Average CPU Load (%), StackId',
      'Windows Compute Environments': 'Username, User Email, User Status, Activity Name, InstanceId, Environment Commissioned - Duration (Hours), Environment Running - Duration (Hours), Environment Stopped - Duration (Hours), Environment Creation Date, Environment Termination Date, Average CPU Load (%), StackId',
      'Data submission dataset download': 'Email, Roles, Client Ip, Remote Ip, Timestamp'
    };

    $scope.selectReport = function(report){
      $scope.selectedReport = report;

      if ($scope.selectedReport.constraintTypes.indexOf('DATE_RANGE') === -1) {
        $scope.reports.push({name: $scope.selectedReport.typeName, created: new Date().getTime(), status: 'BUILDING'});
        Report.create($scope.user.guid, $scope.selectedReport.type, {}, function(createData){
          refreshReports();
          $scope.selectedReport = {};
        }, function(){
          $scope.errors = ['Can\'t create report'];
          $scope.selectedReport = {};
        });
        return;
      }

      var modalInstance = $modal.open({
        templateUrl:       'views/system-reports/modal.html',
        windowTemplateUrl: 'views/not-modal/window.html',
        controller:        'ModalSystemReportsCtrl',
        backdrop:          true,
        scrollableBody:    false,
        size:              'lg',
        resolve:           { selectedReport: function() {
          return $scope.selectedReport;
        } },
      });

      modalInstance.result.then(function (data) {
        $scope.reports.push({name: $scope.selectedReport.typeName, created: new Date().getTime(), status: 'BUILDING'});
        Report.constrainedCreate($scope.user.guid, $scope.selectedReport.type, data, function(createData){
          refreshReports();
          $scope.selectedReport = {};
        }, function(){
          $scope.errors = ['Can\'t create report'];
          $scope.selectedReport = {};
        });
      }, function () {
        $scope.selectedReport = {};
        // error
      });
    };

    $scope.remove = function(token) {

      var modalInstance = $modal.open({
        templateUrl:       'views/system-reports/report-delete-confirm-modal.html',
        windowTemplateUrl: 'views/not-modal/window.html',
        controller:        'ReportDeleteConfirmModalCtrl',
        backdrop:          true,
        scrollableBody:    true,
      });

      modalInstance.result.then(function() {
        Report.remove(token, function(){
          var idx = $scope.reports.map(function(r) { return r.token; }).indexOf(token);
          $scope.reports.splice(idx, 1);
        });
      });

    };

    $scope.download = function(token) {
      window.open(Report.getContentUrl(token));
    };

    var mapReportTypes = function(reportTypes){
      reportTypes.forEach(function(report){
        var idx = $scope.reportGroups.map(function(r) { return r.name; }).indexOf(report.categoryName);
        if (idx === -1) {
          $scope.reportGroups.push({
            name:     report.categoryName,
            expanded: true,
            reports:  [report]
          });
        } else {
          $scope.reportGroups[idx].reports.push(report);
        }
      });
    };

    // var checker;
    // var startChecker = function() {
    //   var reports2check = $scope.reports.filter(function(r){ return r.status === 'Building'; });
    //   reports2check.forEach(function(r2c){
    //     Report.getStatus(r2c.token, {}, function(data){
    //       var idx = $scope.reports.map(function(r){ return r.token; }).indexOf(r2c.token);
    //       $scope.reports[idx].status = data;
    //     });
    //   });
    //   if (reports2check.length === 0) {
    //     $interval.cancel(checker);
    //   }
    // };

    var setReportsAndInitContentUrl = function(data){
      $scope.reports = data;
      var tokens = [];
      for (var i = 0; i < $scope.reports.length; i++){
        var token = $scope.reports[i].token;
        $scope.reports[i].contentUrl = Report.getContentUrl(token);
        tokens.push(token);
      }
      Report.getStatuses(tokens, setStatuses);
      
      // get report statuses
      // checker = $interval(startChecker, 10000);
    };

    var setStatuses = function(data) {
      var incomplete = [];
      $scope
        .reports
        .filter(function(r) { return !$scope.isReportComplete(r); })
        .forEach(function(r, idx) {
          r.status = data[idx];

          if (!$scope.isReportComplete(r)) {
            incomplete.push(r.token);
          }
        });

      if (incomplete.length > 0) {
        $timeout(function() {
          Report.getStatuses(incomplete, setStatuses);
        }, 5000);
      }
    };

    $scope.isReportComplete = function(r){
      return (r.status !== undefined && r.status.toLowerCase() === 'complete');
    };

    var refreshReports = function() {
      Report.getUserReports($scope.user.guid, {}, setReportsAndInitContentUrl);
    };

    var setUserAndGetReports = function(user){
      $scope.user = user;
      refreshReports();
    };

    $scope.init = function() {
      Report.getAvailable({}, mapReportTypes);
      User.getCurrentUser(setUserAndGetReports);
    };

    $scope.init();

  });
