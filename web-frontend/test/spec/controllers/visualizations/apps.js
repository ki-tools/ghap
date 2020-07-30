'use strict';

describe('Controller: VisualizationsAppsCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var VisualizationsAppsCtrl,
      scope,
      http;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    http = {
      get: jasmine.createSpy('get').and.callFake(function() {
        return {
          success: function(callback){},
        };
      }),
    };
    VisualizationsAppsCtrl = $controller('VisualizationsAppsCtrl', {
      $scope: scope,
      $http: http
    });
  }));

  it('should call $http for registry.json', function () {
    expect(http.get).toHaveBeenCalled();
  });

  it('scope.setApps should set scope.apps', function () {
    var apps = [{id: 1}];
    scope.setApps(apps);
    expect(scope.apps).toBe(apps);
  });

  it('scope.getAppUrl should return url with app root at the end', function () {
    var appRoot = 'appRoot1';
    var app = {ApplicationRoot: appRoot};
    expect(scope.getAppUrl(app).indexOf(appRoot)).not.toBe(-1);
  });

  it('scope.getThumbnailUrl should include path and app parts', function () {
    var applicationRoot = 'app-root';
    var thumbnail = 'thumbnail';
    var url = scope.getThumbnailUrl(applicationRoot, thumbnail);
    expect(url.indexOf('/VisualizationPublisher/image?url=')).not.toBe(-1);
    expect(url.indexOf(thumbnail)).not.toBe(-1);
  });

});
