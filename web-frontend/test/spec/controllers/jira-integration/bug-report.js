'use strict';

// QA notice:
// please do not consider this test as a controller test only:
// the hook with $modalStack and direct call to $modal
// are used to test script for the modal dialog extracted from ui-bootstrap

describe('Controller: BugReportCtrl', function () {

  var lastModalInstance;
  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
    decorateModal($provide, function(modalInstance){
      lastModalInstance = modalInstance;
    });
  }));

  var BugReportCtrl,
    scope, $modal, $httpBackend, $log;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope, _$modal_, _$httpBackend_, _$log_) {
    scope = $rootScope.$new();
    //modal = {
    //  open:  jasmine.createSpy('open').and.callFake(function() {
    //    return { result: {then: function(){}} };
    //  })
    //};
    $modal = _$modal_;
    $httpBackend = _$httpBackend_;
    $log = _$log_;

    BugReportCtrl = $controller('BugReportCtrl', {
      $scope: scope,
      $modal: $modal
    });

    $httpBackend.expectGET('/locales/locale-en.json').respond(200,'{}');
    $httpBackend.expectGET('undefined/current?token=null').respond(200,'[{}]');
    $httpBackend.expectGET('views/jira-integration/bug-report.html').respond(200,'<forn></forn>');
    $httpBackend.expectGET('views/not-modal/window.html').respond(200,'<div></div>');

  }));

  it('should open modal and log debug message on close modal instance', function () {
    spyOn($modal,'open').and.callThrough();
    scope.openModal();
    expect($modal.open).toHaveBeenCalled();
    $httpBackend.flush();
    scope.$digest();
    var close_message = 'Bug report is submitted.';
    lastModalInstance.close(close_message);
    scope.$digest();
    expect($log.debug.logs[0]).toContain(close_message);
  });

  it('should open modal and log info message on dismiss modal instance', function () {
    scope.openModal();
    $httpBackend.flush();
    scope.$digest();
    lastModalInstance.dismiss();
    scope.$digest();
    expect($log.info.logs[0][0]).toContain('Modal dismissed');
  });

});
