/**
 * Created by Vlad on 14.08.2015.
 */

var my = require('./../Common/ghap-lib');

var obj = {key: 1};
var array = [];
array.push(my.cloneObject(obj));
array.push(obj);
obj.key = 2;
array.push(obj);
console.log(array);