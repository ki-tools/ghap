'use strict';

describe('Controller: UsageReportsSystemReportsCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var UsageReportsSystemReportsCtrl,
    scope,
    Report,
    User,
    currUser,
    reportTypes,
    reports,
    reportsStatuses,
    modal,
    reportData;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    currUser = {guid: 1};
    reportTypes = [
      {categoryName: 'a', id: 1},
      {categoryName: 'a', id: 2},
      {categoryName: 'b', id: 3}
    ];
    reports = [ {token: 123} ];
    reportsStatuses = ['COMPLETED'];
    scope = $rootScope.$new();
    var modal_results_data = {};
    modal = {
      open:  jasmine.createSpy('open').and.callFake(function() {
        return { result: {then: function(callback){
          callback(modal_results_data);
        }} };
      })
    };
    Report = {
      create: jasmine.createSpy('create').and.callFake(
        function(user_guid, report_type, data, successFn, errFn) {
          successFn();
        }
      ),
      constrainedCreate: jasmine.createSpy('constrainedCreate').and.callFake(
        function(user_guid, report_type, data, successFn, errFn) {
          successFn();
        }
      ),
      getStatuses: jasmine.createSpy('getAvailable').and.callFake(
        function(token, setStatusesFn) {
          setStatusesFn(reportsStatuses);
        }
      ),
      getAvailable: jasmine.createSpy('getAvailable').and.callFake(
        function(data, mapReportTypes) {
          mapReportTypes(reportTypes);
        }
      ),
      getUserReports: jasmine.createSpy('getUserReports').and.callFake(
        function(guid, data, setReportsAndInitContentUrl) {
          setReportsAndInitContentUrl(reports);
        }
      ),
      getContentUrl: jasmine.createSpy('getContentUrl').and.callFake(
        function(token) {
          return token;
        }
      ),
      remove: jasmine.createSpy('remove').and.callFake(function(token, callback) {
        callback();
      })
    };
    User = {
      getCurrentUser: jasmine.createSpy('getCurrentUser').and.callFake(
        function(setUserAndGetReports) {
          setUserAndGetReports(currUser);
        }
      )
    };
    UsageReportsSystemReportsCtrl = $controller('UsageReportsSystemReportsCtrl', {
      $scope:    scope,
      Report:    Report,
      User:      User,
      $interval: {},
      $modal:    modal
    });
  }));

  it('init should call for current user, report types and created reports', function () {
    expect(scope.user).toBe(currUser);
    expect(scope.reports[0].contentUrl).toBe(reports[0].token);
    expect(scope.reportGroups.length).toBe(2);
    expect(scope.reportGroups[0].name).toBe(reportTypes[0].categoryName);
    expect(scope.reportGroups[0].reports.length).toBe(2);
  });

  it('remove should remove item from reports array', function () {
    var num_reports = reports.length;
    scope.remove(123);
    expect(reports.length).toBe(num_reports-1);
  });

  describe('select constrained report', function(){

    beforeEach(function(){
      var report = {typeName:'NewReport', constraintTypes: ['DATE_RANGE']};
      reportsStatuses.push('BUILDING');
      scope.selectReport(report);
    });

    it('should open modal to get constrains', function () {
      expect(modal.open).toHaveBeenCalled();
    });

    it('create should clear selectedReport and add new report to array', function () {
      expect(Object.keys(scope.selectedReport).length).toBe(0);
      var last_report_index = reports.length-1;
      expect(reports[last_report_index].status).toBe('BUILDING');
    });

  });

  describe('select simple report', function(){

    beforeEach(function(){
      var report = {typeName:'NewReport', constraintTypes: []};
      reportsStatuses.push('BUILDING');
      scope.selectReport(report);
    });

    it('should not open modal to get constrains', function () {
      expect(modal.open).not.toHaveBeenCalled();
    });

    it('create should clear selectedReport and add new report to array', function () {
      expect(Object.keys(scope.selectedReport).length).toBe(0);
      var last_report_index = reports.length-1;
      expect(reports[last_report_index].status).toBe('BUILDING');
    });

  });

});
