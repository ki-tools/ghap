'use strict';

describe('Controller: KnowledgeBaseCtrl', function() {

  // load the controller's module
  beforeEach(module('bmgfApp', function($provide) {
    mockServerCalls($provide);
  }));

  var KnowledgeBaseCtrl,
    $window,
    scope,
    Settings,
    token;

  // Initialize the controller and a mock scope
  beforeEach(inject(function($controller, $rootScope) {
    scope = $rootScope.$new();
    var sce = {
      trustAsResourceUrl: jasmine.createSpy('trustAsResourceUrl').and.callFake(
        function(url) {
          return url;
        }
      )
    }
    $window = {
      open: jasmine.createSpy('open')
    };
    Settings = {};
    token = 'some_token';
    spyOn(localStorage, 'getItem').and.callFake(function(key) {
      return token;
    });
    KnowledgeBaseCtrl = $controller('KnowledgeBaseCtrl', {
      $scope: scope,
      $window: $window,
      Settings: Settings,
      $sce: sce
    });
  }));

  it('openTab should open new tab with url from settings', function() {
    var testUrl = 'http://test.com';
    Settings.KNOWLEDGE_BASE_URL = testUrl;
    scope.openTab();
    expect($window.open).toHaveBeenCalledWith(testUrl + '?access_token=' + token, '_blank');
  });

});