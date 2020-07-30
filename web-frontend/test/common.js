function mockServerCalls($provide) {
  $provide.factory('Settings', function(){
    return {
      'API_ROOT' : 'test-api',
      'JIRA_API_URL': 'http://test-jira'
    };
  });
  $provide.factory('SyncHttp', function(Settings){
    return {
      get: function(url){
        if (url === Settings.API_ROOT + '/user') {
          return '{"dn":"tester"}';
        } else if (url === Settings.API_ROOT + '/user/roles/tester') {
          return '[{"guid":"000", "name":"testerRole"}]';
        }  else {
          return null;
        }
      }
    };
  });
}

// Q: angularjs provide decorator example
// http://jsfiddle.net/jeremylikness/LATc4/
/**
 * decorate $modal service with function executed before opening of modal dialog
 * @param $provide - angular $provide service
 * @param openDecoratorFn - function parameter is last created modal instance.
 */
function decorateModal($provide, openDecoratorFn) {
  // decorate $modalStack.open to save last created modal instance
  $provide.decorator('$modalStack', function($delegate){
    var myModalStackOpen = function(originalFn) {
      return function (){
        openDecoratorFn(arguments[0]);
        return originalFn.apply(null, arguments);
      };
    };
    $delegate.open = myModalStackOpen($delegate.open);
    return $delegate;
  })
}
