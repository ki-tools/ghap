'use strict';

describe('Controller: SubmitDatasetDownloadCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var SubmitDatasetDownloadCtrl, DataSubmission,
    scope;

  var userData = [
    {id: 1}
  ];

  var location = {
    path: function(){return '/submit-dataset';}
  };

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    DataSubmission = {
      getData: jasmine.createSpy('getData').and.callFake(function(callback){
          callback(userData);
      }),
      remove: jasmine.createSpy('remove').and.callFake(function(params, success){
          success();
      })
    };


    scope = $rootScope.$new();
    SubmitDatasetDownloadCtrl = $controller('SubmitDatasetDownloadCtrl', {
      $scope: scope,
      $location: location,
      DataSubmission: DataSubmission
    });
  }));

  it('should call getData on load', function () {
    expect(DataSubmission.getData).toHaveBeenCalled();
    expect(scope.userData.length).toBe(1);
  });

  it('should remove data submission on deleteFile', function () {
    scope.deleteFile({keyName: 'my-file.dat'});
    expect(DataSubmission.remove).toHaveBeenCalled();
    expect(DataSubmission.getData.calls.count()).toEqual(2); // one on load and one after DataSubmission.remove
  });

});
