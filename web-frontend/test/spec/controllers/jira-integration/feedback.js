'use strict';

describe('Controller: FeedbackCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var FeedbackCtrl,
    scope, modal;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    modal = {
      open:  jasmine.createSpy('open').and.callFake(function() {
        return { result: {then: function(){}} };
      })
    };


    FeedbackCtrl = $controller('FeedbackCtrl', {
      $scope: scope,
      $modal: modal
    });
  }));

  it('should open modal', function () {
    scope.openModal();
    expect(modal.open).toHaveBeenCalled();
  });

});
