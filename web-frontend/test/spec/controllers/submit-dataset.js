'use strict';

describe('Controller: SubmitDatasetCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var SubmitDatasetCtrl, User,
    scope;

  var roles = [
      {
        name: 'Data Curator'
      },
      {
        name: 'Data Contributor'
      },
      {
        name: 'Data Analyst'
      }
    ];

  User = {
    getCurrentUserRoles: jasmine.createSpy('getCurrentUserRoles').and.callFake(function(callback){
        callback(roles);
    })
  };

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($rootScope, $controller) {
    scope = $rootScope.$new();
    SubmitDatasetCtrl = $controller('SubmitDatasetCtrl', {
      $scope: scope,
      User: User
    });
  }));

  it('should process Data Curator role', function () {
    expect(scope.isDataCurator).toBe(true);
  });
  it('should process Data Contributor role', function () {
    expect(scope.isDataContributor).toBe(true);
  });

});
