# web-frontend

This project is generated with [yo angular generator](https://github.com/yeoman/generator-angular)
version 0.11.1.

## Build

To build application you need npm (shipped with [nodejs](https://nodejs.org/)). Install bower and grunt with grunt-cli: `npm install --global bower grunt-cli`

Install Ruby, run `gem update --system && gem install compass`

After the first checkout run `npm install && bower install`

To build app run `grunt` - production ready app will be placed into dist folder.

So to build on build server use:

* git clone ...
* npm install && bower install
* grunt --force
* gradle war

## Development

For development purposes it's good to have yeoman with angular generator installed. Run `npm install --global yo generator-angular`

To run app locally run `grunt serve`.

## Testing

Running `grunt test` will run the unit tests with karma.
